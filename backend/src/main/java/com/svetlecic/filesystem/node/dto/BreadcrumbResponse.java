package com.svetlecic.filesystem.node.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Folder on the path to the current folder")
public record BreadcrumbResponse(
    @Schema(description = "Folder id", example = "1")
    long id,
    @Schema(description = "Folder name", example = "Home")
    String name) {

}
