package com.svetlecic.filesystem.node.domain.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.node.domain.NodeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NodeTypeConverterTest {

    private final NodeTypeConverter nodeTypeConverter = new NodeTypeConverter();

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
