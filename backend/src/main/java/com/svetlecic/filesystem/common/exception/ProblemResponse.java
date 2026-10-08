package com.svetlecic.filesystem.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "Problem", description = "Error response in RFC 9457 problem details format")
public record ProblemResponse(
    @Schema(example = "about:blank")
    String type,
    @Schema(example = "Conflict")
    String title,
    @Schema(example = "409")
    int status,
    @Schema(example = "A file or folder with this name already exists in folder 1")
    String detail,
    @Schema(example = "/api/v1/folders/1/folders")
    String instance,
    @Schema(description = "Stable, machine-readable error code", example = "NAME_ALREADY_EXISTS")
    String code,
    @Schema(description = "Same value as the X-Request-Id response header")
    String requestId,
    @Schema(description = "Every invalid field; present only when code is VALIDATION_FAILED")
    List<FieldViolation> errors) {

}
