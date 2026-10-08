package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "A folder")
public record FolderResponse(
    @Schema(description = "Folder id", example = "42") long id,
    @Schema(description = "Folder name", example = "Reports") String name,
    @Schema(description = "Id of the parent folder; null for the root folder", example = "1", nullable = true)
    @Nullable Long parentId) {

}
