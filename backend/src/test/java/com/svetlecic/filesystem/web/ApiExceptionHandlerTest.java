package com.svetlecic.filesystem.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.common.exception.BusinessRuleValidationException;
import com.svetlecic.filesystem.common.exception.FieldViolation;
import com.svetlecic.filesystem.node.exception.NodeNotFoundException;
import com.svetlecic.filesystem.node.exception.RootFolderMissingException;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler apiExceptionHandler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/folders/1/folders");

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestIdFilter.MDC_KEY);
    }

    @Test
    void shouldNotExposeDetailsOfUnexpectedErrors() {
        var problem = apiExceptionHandler.handleUnexpected(
            new IllegalStateException("connection to db-prod-1 refused"), request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Unexpected error");
        assertThat(problem.getProperties()).isNullOrEmpty();
    }

    @Test
    void shouldNotExposeCodeOrDetailsOfServerSideApplicationErrors() {
        var problem = apiExceptionHandler.handleApplicationException(new RootFolderMissingException(), request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Unexpected error");
        assertThat(problem.getProperties()).isNullOrEmpty();
    }

    @Test
    void shouldUseStatusMessageAndCodeOfClientSideApplicationErrors() {
        var problem = apiExceptionHandler.handleApplicationException(NodeNotFoundException.folder(42), request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Folder 42 not found");
        assertThat(problem.getProperties()).containsEntry("code", "NODE_NOT_FOUND");
    }

    @Test
    void shouldListEveryInvalidFieldOfBusinessRuleValidation() {
        var violations = List.of(new FieldViolation("name", "NAME_REQUIRED", "must not be empty"));

        var problem = apiExceptionHandler.handleBusinessRuleValidation(
            new BusinessRuleValidationException(violations), request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("name: must not be empty");
        assertThat(problem.getProperties())
            .containsEntry("code", "VALIDATION_FAILED")
            .containsEntry("errors", violations);
    }

    @Test
    void shouldIncludeRequestIdFromMdc() {
        MDC.put(RequestIdFilter.MDC_KEY, "req-42");

        var problem = apiExceptionHandler.handleApplicationException(NodeNotFoundException.folder(42), request);

        assertThat(problem.getProperties()).containsEntry("requestId", "req-42");
    }

    @Test
    void shouldTurnUniqueViolationFromConcurrentRequestIntoConflict() {
        var problem = apiExceptionHandler.handleDataIntegrityViolation(dataIntegrityViolation(ConstraintKind.UNIQUE),
            request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("code", "CONCURRENT_CHANGE");
    }

    @Test
    void shouldTurnMissingParentFromConcurrentDeleteIntoConflict() {
        var problem = apiExceptionHandler.handleDataIntegrityViolation(
            dataIntegrityViolation(ConstraintKind.FOREIGN_KEY), request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("code", "CONCURRENT_CHANGE");
    }

    @Test
    void shouldTreatOtherConstraintViolationsAsUnexpected() {
        var problem = apiExceptionHandler.handleDataIntegrityViolation(dataIntegrityViolation(ConstraintKind.CHECK),
            request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Unexpected error");
    }

    private static DataIntegrityViolationException dataIntegrityViolation(ConstraintKind kind) {
        var cause = new ConstraintViolationException("constraint violated", new SQLException("violated"), kind,
            "some_constraint");
        return new DataIntegrityViolationException("could not execute statement", cause);
    }
}
