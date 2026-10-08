package com.svetlecic.filesystem.node.domain;

import com.svetlecic.filesystem.common.exception.EnumValueNotFoundException;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NodeType {
    FOLDER("FOLDER", "Folder"),
    FILE("FILE", "File");

    private final String code;
    private final String description;

    public static NodeType fromCode(String code) {
        return Arrays.stream(values())
            .filter(type -> type.code.equals(code.trim()))
            .findAny()
            .orElseThrow(() -> new EnumValueNotFoundException(NodeType.class, "code", code));
    }
}
