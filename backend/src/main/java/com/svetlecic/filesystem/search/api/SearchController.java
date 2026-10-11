package com.svetlecic.filesystem.search.api;

import com.svetlecic.filesystem.common.exception.ProblemResponse;
import com.svetlecic.filesystem.search.dto.SearchPageResponse;
import com.svetlecic.filesystem.search.dto.SuggestionsResponse;
import com.svetlecic.filesystem.search.service.FileSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
class SearchController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final FileSearchService fileSearchService;

    @Operation(summary = "Search files by exact name",
        description = "Ignores letter case. With folderId it searches that folder and its subfolders.")
    @ApiResponse(responseCode = "200", description = "Matching files")
    @ApiResponse(responseCode = "400", description = "Invalid name, size or cursor",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @ApiResponse(responseCode = "404", description = "Folder not found",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/files")
    SearchPageResponse getFiles(
        @Parameter(description = "File name", example = "report.pdf")
        @RequestParam String name,
        @Parameter(description = "Folder to search in, all folders if omitted")
        @RequestParam(required = false) @Nullable Long folderId,
        @Parameter(description = "Cursor from the previous page")
        @RequestParam(required = false) @Nullable String cursor,
        @Parameter(description = "Page size, 1 to 200") @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {

        return fileSearchService.getFiles(name, folderId, cursor, size);
    }

    @Operation(summary = "Suggest files by name prefix", description = "Up to 10 files, sorted by name.")
    @ApiResponse(responseCode = "200", description = "Suggested files")
    @ApiResponse(responseCode = "400", description = "Invalid prefix",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    @GetMapping("/suggestions")
    SuggestionsResponse getSuggestions(
        @Parameter(description = "Start of the file name", example = "rep") @RequestParam String prefix) {

        return fileSearchService.getSuggestions(prefix);
    }
}
