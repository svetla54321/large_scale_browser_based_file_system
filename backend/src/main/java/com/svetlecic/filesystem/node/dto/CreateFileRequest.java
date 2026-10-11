package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "File to create")
public record CreateFileRequest(
    @Schema(description = "File name", example = "report.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull String name) {

}
