package com.svetlecic.filesystem.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(description = "Page of search results")
public record SearchPageResponse(
    @Schema(description = "Files")
    List<FileHitResponse> items,
    @Schema(description = "Cursor for the next page, null on the last page", nullable = true)
    @Nullable String nextCursor) {

}
