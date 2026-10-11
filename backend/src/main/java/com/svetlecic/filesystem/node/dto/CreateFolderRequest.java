package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Folder to create")
public record CreateFolderRequest(
    @Schema(description = "Folder name", example = "Reports", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull String name) {

}
