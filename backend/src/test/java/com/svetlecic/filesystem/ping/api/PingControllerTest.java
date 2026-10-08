package com.svetlecic.filesystem.ping.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PingController.class)
@ExtendWith(OutputCaptureExtension.class)
class PingControllerTest {

    @Autowired
    MockMvcTester mockMvcTester;

    @Test
    void shouldAnswerWithPong() {
        assertThat(mockMvcTester.get().uri("/api/v1/ping").header("X-Request-Id", "test-1"))
                .hasStatusOk()
                .hasHeader("X-Request-Id", "test-1")
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("pong");
    }

    @Test
    void shouldRejectUnsupportedApiVersionWithProblemDetail(CapturedOutput output) {
        assertThat(mockMvcTester.get().uri("/api/v99/ping"))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .containsHeader("X-Request-Id")
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("BAD_REQUEST");

        assertThat(output).contains("Rejected GET /api/v99/ping with 400: Invalid API version");
    }

    @Test
    void shouldGiveUnknownRouteTheStatusNameAsCode() {
        assertThat(mockMvcTester.get().uri("/api/v1/nothing-here"))
            .hasStatus(404)
            .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
            .bodyJson()
            .extractingPath("$.code")
            .isEqualTo("NOT_FOUND");
    }

    @Test
    void shouldGiveUnsupportedMethodTheStatusNameAsCode() {
        assertThat(mockMvcTester.delete().uri("/api/v1/ping"))
            .hasStatus(405)
            .bodyJson()
            .extractingPath("$.code")
            .isEqualTo("METHOD_NOT_ALLOWED");
    }
}
