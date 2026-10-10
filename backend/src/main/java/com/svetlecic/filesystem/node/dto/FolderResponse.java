package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(description = "A folder")
public record FolderResponse(
    @Schema(description = "Folder id", example = "42")
    long id,
    @Schema(description = "Folder name", example = "Reports")
    String name,
    @Schema(description = "Parent folder id, null for the root", example = "1", nullable = true)
    @Nullable Long parentId,
    @Schema(description = "Path from the root to the parent")
    List<BreadcrumbResponse> breadcrumbs) {

}
