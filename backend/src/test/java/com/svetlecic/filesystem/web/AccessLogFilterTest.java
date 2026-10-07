package com.svetlecic.filesystem.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class AccessLogFilterTest {

    private final AccessLogFilter filter = new AccessLogFilter();

    @Test
    void shouldLogMethodPathAndStatusOfEveryRequest(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/folders/7/files");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> response.setStatus(201));

        assertThat(output).contains("POST /api/v1/folders/7/files 201");
    }

    @Test
    void shouldSkipActuatorRequests(CapturedOutput output) throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(output).doesNotContain("/actuator/health");
    }
}
