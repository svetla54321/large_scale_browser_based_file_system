package com.svetlecic.filesystem.node.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.exception.InvalidCursorException;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Base64;
import java.util.regex.Pattern;

record PageCursor(NodeType type, String name) {

    static final PageCursor FIRST = new PageCursor(NodeType.FOLDER, "");

    private static final char SEPARATOR = '\n';
    private static final Pattern CONTROL_OR_SURROGATE = Pattern.compile("[\\p{Cc}\\p{Cs}]");

    static PageCursor after(Node node) {
        return new PageCursor(node.getType(), node.getName());
    }

    static PageCursor decode(String cursor) {
        try {
            var bytes = Base64.getUrlDecoder().decode(cursor);
            var raw = UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            var separator = raw.indexOf(SEPARATOR);
            if (separator < 0) {
                throw new InvalidCursorException();
            }
            var name = raw.substring(separator + 1);
            if (!isPlausibleName(name)) {
                throw new InvalidCursorException();
            }
            return new PageCursor(NodeType.valueOf(raw.substring(0, separator)), name);
        } catch (IllegalArgumentException | CharacterCodingException e) {
            throw new InvalidCursorException(e);
        }
    }

    String encode() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((type.name() + SEPARATOR + name).getBytes(UTF_8));
    }

    String nameAfterFor(NodeType listedType) {
        return type == listedType ? name : "";
    }

    private static boolean isPlausibleName(String name) {
        return !name.isEmpty()
                && name.codePointCount(0, name.length()) <= NodeNameNormalizer.MAX_LENGTH
                && !CONTROL_OR_SURROGATE.matcher(name).find();
    }
}
