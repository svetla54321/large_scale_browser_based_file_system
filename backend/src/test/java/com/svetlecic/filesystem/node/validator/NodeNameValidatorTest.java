package com.svetlecic.filesystem.node.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.svetlecic.filesystem.common.exception.BusinessRuleValidationException;
import com.svetlecic.filesystem.common.exception.FieldViolation;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;

class NodeNameValidatorTest {

    private static final int ZERO_WIDTH_JOINER = 0x200D;

    private final NodeNameValidator nodeNameValidator = new NodeNameValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Docs", ".hidden", "notes.2026.txt", "\u010Devapi", "a b", "x"})
    void shouldAcceptValidNames(String name) {
        assertThatCode(() -> nodeNameValidator.validate(name)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ".", "..", "a/b", "a\\b", "a:b", "a*b", "a?b", "a\"b", "a<b", "a>b", "a|b",
            "a\u0000b", "a\nb", "a\u202Eb", "a\u2066b", "a\uD800b", "a\uFFFFb"})
    void shouldRejectInvalidNames(String name) {
        assertThatThrownBy(() -> nodeNameValidator.validate(name)).isInstanceOf(BusinessRuleValidationException.class);
    }

    @Test
    void shouldReportEveryBrokenRuleAtOnce() {
        var name = "a/b".repeat(100);

        assertThatThrownBy(() -> nodeNameValidator.validate(name))
            .isInstanceOfSatisfying(BusinessRuleValidationException.class, exception -> {
                assertThat(exception.getCode()).isEqualTo("VALIDATION_FAILED");
                assertThat(exception.getViolations())
                    .extracting(FieldViolation::field, FieldViolation::code)
                    .containsExactly(
                        tuple("name", "NAME_TOO_LONG"),
                        tuple("name", "NAME_FORBIDDEN_CHARACTER"));
            });
    }

    static Stream<Arguments> namesWithTheRuleTheyBreak() {
        return Stream.of(
            Arguments.of("", "NAME_REQUIRED"),
            Arguments.of("x".repeat(NodeNameNormalizer.MAX_LENGTH + 1), "NAME_TOO_LONG"),
            Arguments.of("..", "NAME_RESERVED"),
            Arguments.of("a:b", "NAME_FORBIDDEN_CHARACTER"),
            Arguments.of("a" + chars(0) + "b", "NAME_INVISIBLE_CHARACTER"));
    }

    static Stream<String> namesWithInvisibleCharacters() {
        return Stream.of(0x200B, 0x200E, 0x2028, 0x2029, 0x2060)
                .map(codePoint -> "Do" + chars(codePoint) + "cs");
    }

    private static String chars(int... codePoints) {
        return new String(codePoints, 0, codePoints.length);
    }
}
