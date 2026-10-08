package com.svetlecic.filesystem.node.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NodeNameHelperUnicodeTest {

    private static final int NO_BREAK_SPACE = 0x00A0;
    private static final int ZERO_WIDTH_SPACE = 0x200B;
    private static final int BYTE_ORDER_MARK = 0xFEFF;

    @Test
    void shouldTreatNonBreakingAndFigureSpacesAsBlank() {
        assertThat(NodeNameNormalizer.normalize(chars(NO_BREAK_SPACE, 0x2007, 0x202F))).isEmpty();
    }

    @Test
    void shouldTrimInvisibleFormatCharactersAroundName() {
        assertThat(NodeNameNormalizer.normalize(chars(BYTE_ORDER_MARK) + "Docs" + chars(ZERO_WIDTH_SPACE))).isEqualTo("Docs");
    }

    @Test
    void shouldTurnInnerNonBreakingSpaceIntoNormalSpace() {
        assertThat(NodeNameNormalizer.normalize("My" + chars(NO_BREAK_SPACE) + "Docs")).isEqualTo("My Docs");
    }

    @ParameterizedTest
    @MethodSource("caseVariants")
    void shouldGiveSameKeyToCaseVariants(String first, String second) {
        assertThat(NodeNameNormalizer.key(first)).isEqualTo(NodeNameNormalizer.key(second));
    }

    static Stream<Arguments> caseVariants() {
        return Stream.of(
                Arguments.of(chars(0x039F, 0x0394, 0x039F, 0x03A3), chars(0x03BF, 0x03B4, 0x03BF, 0x03C2)),
                Arguments.of(chars(0x017F), "S"),
                Arguments.of(chars(0x00B5), chars(0x039C)),
                Arguments.of("REPORT.PDF", "report.pdf"));
    }

    @Test
    void shouldKeepSharpSAndDoubleSApart() {
        assertThat(NodeNameNormalizer.key("STRASSE")).isNotEqualTo(
            NodeNameNormalizer.key("STRA" + chars(0x00DF) + "E"));
    }

    @Test
    void shouldMapDottedCapitalIToSingleLetter() {
        assertThat(NodeNameNormalizer.key(chars(0x0130))).isEqualTo("i");
    }

    private static String chars(int... codePoints) {
        return new String(codePoints, 0, codePoints.length);
    }
}
