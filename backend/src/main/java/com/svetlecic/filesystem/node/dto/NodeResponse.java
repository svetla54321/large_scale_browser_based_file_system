package com.svetlecic.filesystem.node.dto;

import com.svetlecic.filesystem.node.domain.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "A file or folder")
public record NodeResponse(
    @Schema(description = "Node id", example = "42")
    long id,
    @Schema(description = "Name as entered, with surrounding whitespace removed", example = "Reports")
    String name,
    @Schema(description = "Whether the node is a folder or a file")
    NodeType type,
    @Schema(description = "Id of the folder that contains this node", example = "1")
    long parentId,
    @Schema(description = "When the node was created (UTC)")
    Instant createdAt) {

}
