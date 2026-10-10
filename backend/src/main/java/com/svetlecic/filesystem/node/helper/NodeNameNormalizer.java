package com.svetlecic.filesystem.node.helper;

import java.text.Normalizer;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeNameNormalizer {

    public static final int MAX_LENGTH = 255;

    private static final Pattern SPACE_SEPARATORS = Pattern.compile("\\p{Zs}");
    private static final int FIRST_TAG_CHARACTER = 0xE0020;
    private static final int LAST_TAG_CHARACTER = 0xE007F;

    public static String normalize(@NonNull String raw) {
        var text = SPACE_SEPARATORS.matcher(raw).replaceAll(" ");
        var start = skipLeadingBlanks(text);
        var end = text.length();
        while (end > start && isTrailingBlank(text.codePointBefore(end))) {
            end -= Character.charCount(text.codePointBefore(end));
        }
        return Normalizer.normalize(text.substring(start, end), Normalizer.Form.NFC);
    }

    public static String normalizePrefix(@NonNull String raw) {
        var text = SPACE_SEPARATORS.matcher(raw).replaceAll(" ");
        return Normalizer.normalize(text.substring(skipLeadingBlanks(text)), Normalizer.Form.NFC);
    }

    public static String key(@NonNull String name) {
        var folded = name.codePoints()
            .map(codePoint -> Character.toLowerCase(Character.toUpperCase(codePoint)))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        return Normalizer.normalize(folded, Normalizer.Form.NFC);
    }

    private static int skipLeadingBlanks(String text) {
        var start = 0;
        while (start < text.length() && isBlank(text.codePointAt(start))) {
            start += Character.charCount(text.codePointAt(start));
        }
        return start;
    }

    private static boolean isTrailingBlank(int codePoint) {
        return isBlank(codePoint) && (codePoint < FIRST_TAG_CHARACTER || codePoint > LAST_TAG_CHARACTER);
    }

    private static boolean isBlank(int codePoint) {
        return codePoint == ' ' || (codePoint >= '\t' && codePoint <= '\r')
            || Character.isSpaceChar(codePoint) || Character.getType(codePoint) == Character.FORMAT;
    }
}
