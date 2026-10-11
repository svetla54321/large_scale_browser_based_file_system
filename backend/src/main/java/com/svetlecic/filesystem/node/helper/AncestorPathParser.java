package com.svetlecic.filesystem.node.helper;

import static java.util.function.Predicate.not;

import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AncestorPathParser {

    public static List<Long> parseIds(@NonNull String ancestorPath) {
        return Arrays.stream(ancestorPath.split("/"))
            .filter(not(String::isEmpty))
            .map(Long::valueOf)
            .toList();
    }
}
