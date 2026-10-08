package com.svetlecic.filesystem.common.exception;

import static java.util.stream.Collectors.joining;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Comparator;
import java.util.List;

@Schema(description = "One invalid request field or parameter")
public record FieldViolation(
    @Schema(description = "Field or parameter name", example = "name")
    String field,
    @Schema(description = "Stable code of the broken rule", example = "NAME_FORBIDDEN_CHARACTER")
    String code,
    @Schema(description = "Human-readable explanation", example = "must not contain any of \\ / : * ? \" < > |")
    String message) {

    public static final Comparator<FieldViolation> BY_FIELD_AND_CODE =
        Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::code);

    public static String describe(List<FieldViolation> violations) {
        return violations.stream()
            .map(violation -> violation.field() + ": " + violation.message())
            .collect(joining("; "));
    }

}
