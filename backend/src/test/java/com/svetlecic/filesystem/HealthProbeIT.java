package com.svetlecic.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class HealthProbeIT {

    @Autowired
    MockMvcTester mockMvcTester;

    @ParameterizedTest
    @ValueSource(strings = {"liveness", "readiness"})
    void shouldReportProbeUpOutsideKubernetes(String probe) {
        assertThat(mockMvcTester.get().uri("/actuator/health/{probe}", probe))
            .hasStatusOk()
            .bodyJson()
            .extractingPath("$.status")
            .isEqualTo("UP");
    }
}
