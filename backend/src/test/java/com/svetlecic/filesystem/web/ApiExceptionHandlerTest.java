package com.svetlecic.filesystem.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    @Test
    void shouldNotExposeDetailsOfUnexpectedErrors() {
        var problem = new ApiExceptionHandler().handleUnexpected(
                new IllegalStateException("connection to db-prod-1 refused"),
                new MockHttpServletRequest("GET", "/api/v1/ping"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Unexpected error");
    }
}
