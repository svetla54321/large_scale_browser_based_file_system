package com.svetlecic.filesystem.node.api;

import com.svetlecic.filesystem.common.exception.ProblemResponse;
import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.dto.CreateFileRequest;
import com.svetlecic.filesystem.node.dto.CreateFolderRequest;
import com.svetlecic.filesystem.node.dto.FolderPageResponse;
import com.svetlecic.filesystem.node.dto.FolderResponse;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Folders")
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
class FolderController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final NodeService nodeService;

    @Operation(summary = "Get the root folder")
    @ApiResponse(responseCode = "200", description = "Root folder")
    @ApiResponse(responseCode = "500", description = "Root folder is missing",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/root")
    FolderResponse getRoot() {

        return nodeService.getRoot();
    }

    @Operation(summary = "Get a folder with its breadcrumbs")
    @ApiResponse(responseCode = "200", description = "Folder found")
    @ApiResponse(responseCode = "400", description = "Invalid id",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "Folder not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/{id}")
    FolderResponse getFolder(
        @Parameter(description = "Folder id", example = "1") @PathVariable long id) {

        return nodeService.getFolder(id);
    }

    @Operation(summary = "List folder contents", description = "Folders first, then files, sorted by name.")
    @ApiResponse(responseCode = "200", description = "One page of contents")
    @ApiResponse(responseCode = "400", description = "Invalid id, size or cursor",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "Folder not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/{id}/children")
    FolderPageResponse getChildren(
        @Parameter(description = "Folder id", example = "1") @PathVariable long id,
        @Parameter(description = "Cursor from the previous page")
        @RequestParam(required = false) @Nullable String cursor,
        @Parameter(description = "Page size, 1 to 200") @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {

        return nodeService.getChildren(id, cursor, size);
    }

    @Operation(summary = "Create a folder")
    @ApiResponse(responseCode = "201", description = "Folder created")
    @ApiResponse(responseCode = "400", description = "Invalid name or folder too deep",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "Parent folder not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name already taken",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @PostMapping("/{parentId}/folders")
    ResponseEntity<NodeResponse> createFolder(
        @Parameter(hidden = true) @PathVariable String version,
        @Parameter(description = "Parent folder id", example = "1") @PathVariable long parentId,
        @Valid @RequestBody CreateFolderRequest request) {

        var folder = nodeService.create(parentId, NodeType.FOLDER, request.name());
        var location = URI.create("/api/%s/folders/%d".formatted(version, folder.id()));
        return ResponseEntity.created(location).body(folder);
    }

    @Operation(summary = "Create a file")
    @ApiResponse(responseCode = "201", description = "File created")
    @ApiResponse(responseCode = "400", description = "Invalid name",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "Parent folder not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name already taken",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @PostMapping("/{parentId}/files")
    ResponseEntity<NodeResponse> createFile(
        @Parameter(hidden = true) @PathVariable String version,
        @Parameter(description = "Parent folder id", example = "1") @PathVariable long parentId,
        @Valid @RequestBody CreateFileRequest request) {

        var file = nodeService.create(parentId, NodeType.FILE, request.name());
        var location = URI.create("/api/%s/files/%d".formatted(version, file.id()));
        return ResponseEntity.created(location).body(file);
    }

    @Operation(summary = "Rename a folder")
    @ApiResponse(responseCode = "200", description = "Folder renamed")
    @ApiResponse(responseCode = "400", description = "Invalid name",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "Folder not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name already taken",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @PatchMapping("/{id}")
    NodeResponse renameFolder(
        @Parameter(description = "Folder id", example = "42") @PathVariable long id,
        @Valid @RequestBody RenameRequest request) {

        return nodeService.rename(id, NodeType.FOLDER, request.name());
    }

    @Operation(summary = "Delete a folder and everything in it")
    @ApiResponse(responseCode = "204", description = "Folder deleted")
    @ApiResponse(responseCode = "400", description = "Invalid id",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "Folder not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409", description = "Root folder, or the folder is busy",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteFolder(
        @Parameter(description = "Folder id", example = "42") @PathVariable long id) {

        nodeService.deleteFolder(id);
    }
}
