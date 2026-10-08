package com.svetlecic.filesystem.node.api;

import com.svetlecic.filesystem.common.exception.ProblemResponse;
import com.svetlecic.filesystem.node.dto.CreateFolderRequest;
import com.svetlecic.filesystem.node.dto.FolderPage;
import com.svetlecic.filesystem.node.dto.FolderResponse;
import com.svetlecic.filesystem.node.dto.NodeResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Folders", description = "Browse the folder tree and create folders")
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
class FolderController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final NodeService nodeService;

    @Operation(summary = "Get the root folder", description = "The root folder always exists and cannot be deleted.")
    @ApiResponse(responseCode = "200", description = "Root folder")
    @ApiResponse(responseCode = "500", description = "ROOT_FOLDER_MISSING: the database has no root folder",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/root")
    FolderResponse getRoot() {

        return nodeService.getRoot();
    }

    @Operation(summary = "Get a folder")
    @ApiResponse(responseCode = "200", description = "Folder found")
    @ApiResponse(responseCode = "400", description = "Id is not a number",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "NODE_NOT_FOUND: no folder with this id",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/{id}")
    FolderResponse getFolder(
        @Parameter(description = "Folder id", example = "1") @PathVariable long id) {

        return nodeService.getFolder(id);
    }

    @Operation(summary = "List a folder's contents",
        description =
            "Folders first, then files, each sorted by name. Keyset pagination: pass nextCursor from "
                + "the previous page as cursor to get the next page.")
    @ApiResponse(responseCode = "200", description = "One page of the folder's contents")
    @ApiResponse(responseCode = "400",
        description = "INVALID_CURSOR: the cursor was not issued by this API; "
            + "VALIDATION_FAILED: size is outside 1 to 200 (see errors)",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "NODE_NOT_FOUND: no folder with this id",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/{id}/children")
    FolderPage getChildren(
        @Parameter(description = "Folder id", example = "1") @PathVariable long id,
        @Parameter(description = "nextCursor from the previous page; omit for the first page")
        @RequestParam(required = false) @Nullable String cursor,
        @Parameter(description = "Page size, 1 to 200") @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {

        return nodeService.getChildren(id, cursor, size);
    }

    @Operation(summary = "Create a folder",
        description = "The name is trimmed and must be unique in the parent folder regardless of letter case.")
    @ApiResponse(responseCode = "201", description = "Folder created; Location points to the new folder")
    @ApiResponse(responseCode = "400",
        description =
            "VALIDATION_FAILED: the name is missing (errors: NotNull) or breaks one or more rules (errors: "
                + "NAME_REQUIRED, NAME_TOO_LONG, NAME_RESERVED, NAME_FORBIDDEN_CHARACTER, NAME_INVISIBLE_CHARACTER); "
                + "MAX_DEPTH_EXCEEDED: the parent is already 32 levels deep; "
                + "MALFORMED_REQUEST_BODY: the body is not valid JSON or name is not a string",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "NODE_NOT_FOUND: the parent folder does not exist",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "409",
        description =
            "NAME_ALREADY_EXISTS: a file or folder with the same name already exists in the parent; "
                + "CONCURRENT_CHANGE: another request changed the same folder at the same time",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @PostMapping("/{parentId}/folders")
    ResponseEntity<NodeResponse> createFolder(
        @Parameter(hidden = true) @PathVariable String version,
        @Parameter(description = "Id of the folder to create the new folder in", example = "1") @PathVariable long parentId,
        @Valid @RequestBody CreateFolderRequest request) {

        var folder = nodeService.createFolder(parentId, request.name());
        var location = URI.create("/api/%s/folders/%d".formatted(version, folder.id()));
        return ResponseEntity.created(location).body(folder);
    }
}
