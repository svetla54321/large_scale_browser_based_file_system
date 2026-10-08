package com.svetlecic.filesystem.node.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.common.exception.EnumValueNotFoundException;
import com.svetlecic.filesystem.node.domain.converter.NodeTypeConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NodeTypeTest {

    private final NodeTypeConverter nodeTypeConverter = new NodeTypeConverter();

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

    @Test
    void shouldStoreTheCodesTheDatabaseCheckConstraintAllows() {
        assertThat(nodeTypeConverter.convertToDatabaseColumn(NodeType.FOLDER)).isEqualTo("FOLDER");
        assertThat(nodeTypeConverter.convertToDatabaseColumn(NodeType.FILE)).isEqualTo("FILE");
    }

    @ParameterizedTest
    @EnumSource(NodeType.class)
    void shouldConvertBothWays(NodeType type) {
        var code = nodeTypeConverter.convertToDatabaseColumn(type);

        assertThat(nodeTypeConverter.convertToEntityAttribute(code)).isEqualTo(type);
    }

    @Test
    void shouldKeepNullAsNull() {
        assertThat(nodeTypeConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(nodeTypeConverter.convertToEntityAttribute(null)).isNull();
    }
}
