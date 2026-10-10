package com.svetlecic.filesystem.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Suggested files")
public record SuggestionsResponse(
    @Schema(description = "Prefix as sent", example = "Rep") String prefix,
    @Schema(description = "Up to 10 files")
    List<FileHitResponse> items) {

}
