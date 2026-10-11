package com.svetlecic.filesystem.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
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
    void shouldTurnUniqueViolationFromConcurrentRequestIntoConflict() {
        var problem = apiExceptionHandler.handleDataIntegrityViolation(dataIntegrityViolation(ConstraintKind.UNIQUE),
            request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("code", "CONCURRENT_CHANGE");
    }

    @Test
    void shouldTurnLockTimeoutIntoConflict() {
        var problem = apiExceptionHandler.handleConcurrentChange(
            new PessimisticLockingFailureException("canceling statement due to lock timeout"), request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("code", "CONCURRENT_CHANGE");
    }

    private static DataIntegrityViolationException dataIntegrityViolation(ConstraintKind kind) {
        var cause = new ConstraintViolationException("constraint violated", new SQLException("violated"), kind,
            "some_constraint");
        return new DataIntegrityViolationException("could not execute statement", cause);
    }
}
