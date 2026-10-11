package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(description = "Page of folder contents")
public record FolderPageResponse(
    @Schema(description = "Folders and files")
    List<NodeResponse> items,
    @Schema(description = "Cursor for the next page, null on the last page", nullable = true)
    @Nullable String nextCursor,
    @Schema(description = "Number of items in the folder", example = "120")
    long totalItems) {

}
