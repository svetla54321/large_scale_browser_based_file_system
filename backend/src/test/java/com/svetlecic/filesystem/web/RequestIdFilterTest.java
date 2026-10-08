package com.svetlecic.filesystem.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() throws Exception {
        var response = new MockHttpServletResponse();

        requestIdFilter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void shouldReuseValidRequestIdFromHeader() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "nginx-4f2a9c");
        var response = new MockHttpServletResponse();

        requestIdFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("nginx-4f2a9c");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc\r\nfake log line", "has spaces", "0123456789012345678901234567890123456789012345678901234567890123456789"})
    void shouldReplaceInvalidRequestId(String header) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", header);
        var response = new MockHttpServletResponse();

        requestIdFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).isNotEqualTo(header).hasSize(36);
    }

    @Test
    void shouldKeepRequestIdInMdcOnlyWhileRequestIsHandled() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "abc-123");
        var idDuringRequest = new AtomicReference<String>();

        requestIdFilter.doFilter(request, new MockHttpServletResponse(), (req, res) -> idDuringRequest.set(MDC.get("requestId")));

        assertThat(idDuringRequest).hasValue("abc-123");
        assertThat(MDC.get("requestId")).isNull();
    }
}
