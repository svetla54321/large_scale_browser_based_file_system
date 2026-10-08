package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(description = "One page of a folder's contents: folders first, then files, each sorted by name")
public record FolderPage(
    @Schema(description = "Nodes on this page")
    List<NodeResponse> items,
    @Schema(description = "Pass as the cursor parameter to get the next page; null on the last page", nullable = true)
    @Nullable String nextCursor) {

}
