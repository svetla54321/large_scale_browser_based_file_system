package com.svetlecic.filesystem.node.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.common.exception.EnumValueNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NodeTypeTest {

    @ParameterizedTest
    @EnumSource(NodeType.class)
    void shouldFindTypeByItsCode(NodeType type) {
        assertThat(NodeType.fromCode(type.getCode())).isEqualTo(type);
    }

    @Test
    void shouldIgnoreSurroundingSpacesInCode() {
        assertThat(NodeType.fromCode(" FILE ")).isEqualTo(NodeType.FILE);
    }

    @Test
    void shouldRejectUnknownCode() {
        assertThatThrownBy(() -> NodeType.fromCode("DRIVE"))
            .isInstanceOf(EnumValueNotFoundException.class)
            .hasMessage("No NodeType with code 'DRIVE'");
    }
}
