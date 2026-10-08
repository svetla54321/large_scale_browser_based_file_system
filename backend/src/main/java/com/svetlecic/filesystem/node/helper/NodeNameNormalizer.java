package com.svetlecic.filesystem.node.helper;

import java.text.Normalizer;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeNameNormalizer {

    public static final int MAX_LENGTH = 255;

    private static final Pattern SPACE_SEPARATORS = Pattern.compile("\\p{Zs}");
    private static final Pattern SURROUNDING_BLANKS = Pattern.compile("^[\\s\\p{Z}\\p{Cf}]+|[\\s\\p{Z}\\p{Cf}]+$");

    public static String normalize(String raw) {
        var singleSpaced = SPACE_SEPARATORS.matcher(raw).replaceAll(" ");
        var trimmed = SURROUNDING_BLANKS.matcher(singleSpaced).replaceAll("");
        return Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    }

    public static String key(String name) {
        var folded = name.codePoints()
            .map(codePoint -> Character.toLowerCase(Character.toUpperCase(codePoint)))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        return Normalizer.normalize(folded, Normalizer.Form.NFC);
    }
}
