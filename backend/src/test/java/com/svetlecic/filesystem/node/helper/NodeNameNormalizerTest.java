package com.svetlecic.filesystem.node.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class NodeNameNormalizerTest {

    @Test
    void shouldTrimSurroundingWhitespaceIncludingUnicodeSpaces() {
        assertThat(NodeNameNormalizer.normalize("\u2003 Reports \t")).isEqualTo("Reports");
    }

    @Test
    void shouldComposeDecomposedCharacters() {
        assertThat(NodeNameNormalizer.normalize("c\u030Cevapi.txt")).isEqualTo("\u010Devapi.txt");
    }

    @Test
    void shouldGiveSameKeyToNamesThatDifferOnlyInCase() {
        assertThat(NodeNameNormalizer.key("Report.PDF")).isEqualTo(NodeNameNormalizer.key("report.pdf"));
        assertThat(NodeNameNormalizer.key("\u010Cevapi")).isEqualTo(NodeNameNormalizer.key("\u010Devapi"));
    }

    @Test
    void shouldHandleLongRunsOfInnerSpacesInLinearTime() {
        var name = "a" + " ".repeat(1_000_000) + "b";

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> NodeNameNormalizer.normalize(name));
    }
}
