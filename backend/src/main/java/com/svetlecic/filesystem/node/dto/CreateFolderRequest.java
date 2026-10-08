package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Folder to create")
public record CreateFolderRequest(
    @Schema(description = "Folder name, 1 to 255 characters, unique in the parent folder regardless of letter case",
        example = "Reports", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull String name) {

}
