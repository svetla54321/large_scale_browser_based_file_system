package com.svetlecic.filesystem.search.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.svetlecic.filesystem.node.exception.InvalidCursorException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Base64;
import java.util.regex.Pattern;

record SearchCursor(String ancestorPath) {

    private static final Pattern ANCESTOR_PATH = Pattern.compile("(/\\d{1,19})+/");

    static SearchCursor decode(String cursor) {
        try {
            var bytes = Base64.getUrlDecoder().decode(cursor);
            var path = UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            if (!ANCESTOR_PATH.matcher(path).matches()) {
                throw new InvalidCursorException();
            }
            return new SearchCursor(path);
        } catch (IllegalArgumentException | CharacterCodingException exception) {
            throw new InvalidCursorException(exception);
        }
    }

    String encode() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(ancestorPath.getBytes(UTF_8));
    }
}
