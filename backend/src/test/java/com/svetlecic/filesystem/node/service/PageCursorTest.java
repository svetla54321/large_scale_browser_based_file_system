package com.svetlecic.filesystem.node.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.exception.InvalidCursorException;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PageCursorTest {

    private static final String NEW_LINE = Character.toString(10);

    @Test
    void shouldSurviveRoundTripWithUnicodeAndUrlUnsafeCharacters() {
        var cursor = new PageCursor(NodeType.FILE, "\u010Devapi & co + 50%.txt");

        var encoded = cursor.encode();

        assertThat(encoded).matches("[A-Za-z0-9_-]+");
        assertThat(PageCursor.decode(encoded)).isEqualTo(cursor);
    }

    @Test
    void shouldStartFilesFromTheBeginningAfterTheLastFolder() {
        var cursor = new PageCursor(NodeType.FOLDER, "zeta");

        assertThat(cursor.nameAfterFor(NodeType.FOLDER)).isEqualTo("zeta");
        assertThat(cursor.nameAfterFor(NodeType.FILE)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not base64!", "Rk9MREVS"})
    void shouldRejectMalformedCursor(String cursor) {
        assertThatThrownBy(() -> PageCursor.decode(cursor)).isInstanceOf(InvalidCursorException.class);
    }

    @Test
    void shouldRejectCursorWhoseNameContainsNullCharacter() {
        var raw = "FOLDER" + NEW_LINE + Character.toString(0) + "x";
        var cursor = Base64.getUrlEncoder().encodeToString(raw.getBytes(UTF_8));

        assertThatThrownBy(() -> PageCursor.decode(cursor)).isInstanceOf(InvalidCursorException.class);
    }

    @Test
    void shouldRejectCursorWithMalformedUtf8() {
        var bytes = ("FOLDER" + NEW_LINE).getBytes(UTF_8);
        var malformed = Arrays.copyOf(bytes, bytes.length + 1);
        malformed[bytes.length] = (byte) 0xFF;
        var cursor = Base64.getUrlEncoder().encodeToString(malformed);

        assertThatThrownBy(() -> PageCursor.decode(cursor)).isInstanceOf(InvalidCursorException.class);
    }

    @Test
    void shouldRejectCursorWithUnknownType() {
        var cursor = Base64.getUrlEncoder().encodeToString("DRIVE\nDocs".getBytes(UTF_8));

        assertThatThrownBy(() -> PageCursor.decode(cursor)).isInstanceOf(InvalidCursorException.class);
    }
}
