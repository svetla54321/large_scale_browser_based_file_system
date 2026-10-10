package com.svetlecic.filesystem.search.mapper;

import com.svetlecic.filesystem.search.dto.FileHitResponse;
import com.svetlecic.filesystem.search.dto.SearchPageResponse;
import com.svetlecic.filesystem.search.dto.SuggestionsResponse;
import com.svetlecic.filesystem.search.repository.FileRow;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SearchMapper {

    @Mapping(target = "folderPath", source = "folderPath")
    FileHitResponse toFileHitResponse(FileRow row, String folderPath);

    @Mapping(target = "items", source = "items")
    @Mapping(target = "nextCursor", source = "nextCursor")
    SearchPageResponse toSearchPageResponse(List<FileHitResponse> items, @Nullable String nextCursor);

    @Mapping(target = "prefix", source = "prefix")
    @Mapping(target = "items", source = "items")
    SuggestionsResponse toSuggestionsResponse(String prefix, List<FileHitResponse> items);
}
