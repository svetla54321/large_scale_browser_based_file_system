package com.svetlecic.filesystem.node.dto;

import com.svetlecic.filesystem.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "A file or folder")
public record NodeResponse(
    @Schema(description = "Node id", example = "42")
    long id,
    @Schema(description = "Name", example = "Reports")
    String name,
    @Schema(description = "FOLDER or FILE")
    NodeType type,
    @Schema(description = "Parent folder id", example = "1")
    long parentId,
    @Schema(description = "Creation time (UTC)")
    Instant createdAt) {

}
