package com.svetlecic.filesystem.node.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class NodeNameHelperTest {

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
    void shouldNotDependOnDefaultLocaleWhenLowercasing() {
        var previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            assertThat(NodeNameNormalizer.key("TITLE")).isEqualTo("title");
        } finally {
            Locale.setDefault(previous);
        }
    }
}
