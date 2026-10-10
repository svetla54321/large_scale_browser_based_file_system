package com.svetlecic.filesystem.web;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

import com.svetlecic.filesystem.common.exception.ApplicationException;
import com.svetlecic.filesystem.common.exception.BusinessRuleValidationException;
import com.svetlecic.filesystem.common.exception.FieldViolation;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE = "code";
    private static final String ERRORS = "errors";
    private static final String REQUEST_ID = "requestId";
    private static final String CLIENT_ERROR = "CLIENT_ERROR";

    private static final String UNEXPECTED_ERROR = "Unexpected error";
    private static final String MALFORMED_QUERY_STRING = "MALFORMED_QUERY_STRING";
    private static final String MALFORMED_REQUEST_BODY = "MALFORMED_REQUEST_BODY";
    private static final String CONCURRENT_CHANGE = "CONCURRENT_CHANGE";
    private static final String CONCURRENT_CHANGE_DETAIL =
        "The request conflicts with a change made by another request at the same time; reload and try again";
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");

    @ExceptionHandler(ApplicationException.class)
    ProblemDetail handleApplicationException(ApplicationException exception, HttpServletRequest request) {
        var status = exception.getStatus();
        if (status.is5xxServerError()) {
            logUnexpected(request, exception);
            return problem(status, UNEXPECTED_ERROR, null);
        }
        logRejected(request.getMethod(), request.getRequestURI(), status.value(), exception.getMessage());
        return problem(status, exception.getMessage(), exception.getCode());
    }

    @ExceptionHandler(BusinessRuleValidationException.class)
    ProblemDetail handleBusinessRuleValidation(BusinessRuleValidationException exception, HttpServletRequest request) {
        var problemDetail = handleApplicationException(exception, request);
        problemDetail.setProperty(ERRORS, exception.getViolations());
        return problemDetail;
    }

    @ExceptionHandler(InvalidParameterException.class)
    ProblemDetail handleMalformedQueryString(InvalidParameterException exception, HttpServletRequest request) {
        var detail = "Malformed query string";
        logRejected(request.getMethod(), request.getRequestURI(), HttpStatus.BAD_REQUEST.value(), detail);
        return problem(HttpStatus.BAD_REQUEST, detail, MALFORMED_QUERY_STRING);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception,
        HttpServletRequest request) {

        return switch (constraintKind(exception)) {
            case UNIQUE, FOREIGN_KEY -> {
                logRejected(request.getMethod(), request.getRequestURI(), HttpStatus.CONFLICT.value(),
                    CONCURRENT_CHANGE_DETAIL);
                yield problem(HttpStatus.CONFLICT, CONCURRENT_CHANGE_DETAIL, CONCURRENT_CHANGE);
            }
            default -> handleUnexpected(exception, request);
        };
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail handleLockTimeout(PessimisticLockingFailureException exception, HttpServletRequest request) {

        logRejected(request.getMethod(), request.getRequestURI(), HttpStatus.CONFLICT.value(), CONCURRENT_CHANGE_DETAIL);
        return problem(HttpStatus.CONFLICT, CONCURRENT_CHANGE_DETAIL, CONCURRENT_CHANGE);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        logUnexpected(request, exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR, null);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var violations = exception.getFieldErrors().stream()
            .map(error -> violation(error.getField(), error))
            .sorted(FieldViolation.BY_FIELD_AND_CODE)
            .toList();
        describe(exception, violations);
        return handleExceptionInternal(exception, null, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var violations = exception.getParameterValidationResults().stream()
            .flatMap(ApiExceptionHandler::violations)
            .sorted(FieldViolation.BY_FIELD_AND_CODE)
            .toList();
        describe(exception, violations);
        return handleExceptionInternal(exception, null, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        describe(exception, List.of(new FieldViolation(exception.getParameterName(), "Required", "is required")));
        return handleExceptionInternal(exception, null, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problemDetail = createProblemDetail(exception, status, "Failed to read request", null, null, request);
        problemDetail.setProperty(CODE, MALFORMED_REQUEST_BODY);
        return handleExceptionInternal(exception, problemDetail, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception exception, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is4xxClientError()) {
            logRejected(method(request), path(request), statusCode.value(), clientSafeDetail(exception, body));
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            addRequestId(problemDetail);
            addStatusCodeWhenMissing(problemDetail, statusCode);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static void addStatusCodeWhenMissing(ProblemDetail problemDetail, HttpStatusCode statusCode) {
        var properties = problemDetail.getProperties();
        if (statusCode.is4xxClientError() && (isNull(properties) || !properties.containsKey(CODE))) {
            var status = HttpStatus.resolve(statusCode.value());
            problemDetail.setProperty(CODE, nonNull(status) ? status.name() : CLIENT_ERROR);
        }
    }

    private static ConstraintKind constraintKind(Throwable throwable) {

        return Stream.iterate(throwable, Objects::nonNull, Throwable::getCause)
            .filter(ConstraintViolationException.class::isInstance)
            .map(ConstraintViolationException.class::cast)
            .map(ConstraintViolationException::getKind)
            .findFirst()
            .orElse(ConstraintKind.OTHER);
    }

    private static Stream<FieldViolation> violations(ParameterValidationResult result) {
        if (result instanceof ParameterErrors errors) {
            return errors.getFieldErrors().stream().map(error -> violation(error.getField(), error));
        }
        var parameter = requireNonNullElse(result.getMethodParameter().getParameterName(), "");
        return result.getResolvableErrors().stream().map(error -> violation(parameter, error));
    }

    private static FieldViolation violation(String field, MessageSourceResolvable error) {
        var codes = error.getCodes();
        var code = isNull(codes) || codes.length == 0 ? "Invalid" : codes[codes.length - 1];
        return new FieldViolation(field, code, requireNonNullElse(error.getDefaultMessage(), "is invalid"));
    }

    private static void describe(ErrorResponse exception, List<FieldViolation> violations) {
        var problemDetail = exception.getBody();
        problemDetail.setDetail(FieldViolation.describe(violations));
        problemDetail.setProperty(CODE, BusinessRuleValidationException.CODE);
        problemDetail.setProperty(ERRORS, violations);
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail, @Nullable String code) {
        var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        if (nonNull(code)) {
            problemDetail.setProperty(CODE, code);
        }
        addRequestId(problemDetail);
        return problemDetail;
    }

    private static void addRequestId(ProblemDetail problemDetail) {
        var requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (nonNull(requestId)) {
            problemDetail.setProperty(REQUEST_ID, requestId);
        }
    }

    private static @Nullable String clientSafeDetail(Exception exception, @Nullable Object body) {
        if (body instanceof ProblemDetail problemDetail) {
            return problemDetail.getDetail();
        }
        if (exception instanceof ErrorResponse error) {
            return error.getBody().getDetail();
        }
        return exception.getClass().getSimpleName();
    }

    private static void logUnexpected(HttpServletRequest request, Exception exception) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
    }

    private static void logRejected(String method, String path, int status, @Nullable String detail) {
        var safeDetail = isNull(detail) ? "" : CONTROL_CHARACTERS.matcher(detail).replaceAll("?");
        log.warn("Rejected {} {} with {}: {}", method, path, status, safeDetail);
    }

    private static String method(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getMethod() : "";
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : "";
    }
}
