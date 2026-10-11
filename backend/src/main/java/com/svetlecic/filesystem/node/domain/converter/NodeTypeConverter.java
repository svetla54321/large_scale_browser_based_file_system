package com.svetlecic.filesystem.node.domain.converter;

import static java.util.Objects.isNull;

import com.svetlecic.filesystem.node.domain.NodeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NodeTypeConverter implements AttributeConverter<NodeType, String> {

    @Override
    public String convertToDatabaseColumn(NodeType type) {
        return isNull(type) ? null : type.getCode();
    }

    @Override
    public NodeType convertToEntityAttribute(String code) {
        return isNull(code) ? null : NodeType.fromCode(code);
    }
}
