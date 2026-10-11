package com.svetlecic.filesystem.node.api;

import com.svetlecic.filesystem.common.exception.ProblemResponse;
import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import com.svetlecic.filesystem.node.dto.RenameRequest;
import com.svetlecic.filesystem.node.service.NodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Files")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
class FileController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final NodeService nodeService;

    @Operation(summary = "Get a file")
    @ApiResponse(responseCode = "200", description = "File found")
    @ApiResponse(responseCode = "400", description = "Invalid id",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "File not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/{id}")
    NodeResponse getFile(
        @Parameter(description = "File id", example = "42") @PathVariable long id) {

        return nodeService.getFile(id);
    }

    @Operation(summary = "Rename a file")
    @ApiResponse(responseCode = "200", description = "File renamed")
    @ApiResponse(responseCode = "400", description = "Invalid name",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "File not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name already taken",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @PatchMapping("/{id}")
    NodeResponse renameFile(
        @Parameter(description = "File id", example = "42") @PathVariable long id,
        @Valid @RequestBody RenameRequest request) {

        return nodeService.rename(id, NodeType.FILE, request.name());
    }

    @Operation(summary = "Delete a file")
    @ApiResponse(responseCode = "204", description = "File deleted")
    @ApiResponse(responseCode = "400", description = "Invalid id",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "File not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409", description = "File is busy",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteFile(@Parameter(description = "File id", example = "42") @PathVariable long id) {

        nodeService.deleteFile(id);
    }
}
