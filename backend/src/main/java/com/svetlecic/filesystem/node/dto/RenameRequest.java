package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New name")
public record RenameRequest(
    @Schema(description = "New name", example = "report-final.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull String name) {

}
