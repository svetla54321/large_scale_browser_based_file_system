package com.svetlecic.filesystem.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A file found by search")
public record FileHitResponse(
    @Schema(description = "File id", example = "42") long id,
    @Schema(description = "File name", example = "report.pdf") String name,
    @Schema(description = "Parent folder id", example = "7") long parentId,
    @Schema(description = "Folder path", example = "/Docs/Work") String folderPath) {

}
