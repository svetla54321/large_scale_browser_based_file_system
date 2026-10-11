package com.svetlecic.filesystem.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "Problem", description = "Error response (RFC 9457)")
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
    @Schema(description = "Error code, missing on 5xx", example = "NAME_ALREADY_EXISTS",
        nullable = true)
    String code,
    @Schema(description = "Same as the X-Request-Id header")
    String requestId,
    @Schema(description = "Invalid fields")
    List<FieldViolation> errors) {

}
