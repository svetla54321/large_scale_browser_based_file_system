package com.svetlecic.filesystem.ping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(PingController.class)
@ExtendWith(OutputCaptureExtension.class)
class PingControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void shouldAnswerWithPong() {
        assertThat(mvc.get().uri("/api/v1/ping").header("X-Request-Id", "test-1"))
                .hasStatusOk()
                .hasHeader("X-Request-Id", "test-1")
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("pong");
    }

    @Test
    void shouldRejectUnsupportedApiVersionWithProblemDetail(CapturedOutput output) {
        assertThat(mvc.get().uri("/api/v99/ping"))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .containsHeader("X-Request-Id");

        assertThat(output).contains("Rejected GET /api/v99/ping with 400: Invalid API version");
    }
}
