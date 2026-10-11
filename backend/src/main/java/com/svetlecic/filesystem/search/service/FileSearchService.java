package com.svetlecic.filesystem.search.service;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import com.svetlecic.filesystem.node.helper.AncestorPathParser;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import com.svetlecic.filesystem.node.service.NodeService;
import com.svetlecic.filesystem.search.dto.FileHitResponse;
import com.svetlecic.filesystem.search.dto.SearchPageResponse;
import com.svetlecic.filesystem.search.dto.SuggestionsResponse;
import com.svetlecic.filesystem.search.mapper.SearchMapper;
import com.svetlecic.filesystem.search.repository.FileRow;
import com.svetlecic.filesystem.search.repository.FileSearchRepository;
import com.svetlecic.filesystem.search.validator.SearchTerm;
import com.svetlecic.filesystem.search.validator.SearchTermValidator;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileSearchService {

    public static final int SUGGESTION_LIMIT = 10;

    private static final String HIGHEST_CHARACTER = Character.toString(Character.MAX_CODE_POINT);

    private final FileSearchRepository fileSearchRepository;
    private final NodeService nodeService;
    private final SearchMapper searchMapper;
    private final SearchTermValidator searchTermValidator;

    @Transactional(readOnly = true)
    public SearchPageResponse getFiles(@NonNull String rawName, @Nullable Long folderId, @Nullable String cursor, int size) {

        var key = keyOf("name", NodeNameNormalizer.normalize(rawName));
        var range = isNull(folderId) ? PathRange.EVERYWHERE : PathRange.below(nodeService.findFolder(folderId));
        var after = isNull(cursor) || cursor.isBlank() ? "" : SearchCursor.decode(cursor).ancestorPath();

        var rows = fileSearchRepository.findByName(key, range.from(), range.to(), after, size + 1);
        var items = rows.stream().limit(size).toList();
        var nextCursor = rows.size() > size ? new SearchCursor(items.getLast().ancestorPath()).encode() : null;
        return searchMapper.toSearchPageResponse(toFileHits(items), nextCursor);
    }

    @Transactional(readOnly = true)
    public SuggestionsResponse getSuggestions(@NonNull String rawPrefix) {

        var key = keyOf("prefix", NodeNameNormalizer.normalizePrefix(rawPrefix));
        var rows = fileSearchRepository.findByNamePrefix(key, key + HIGHEST_CHARACTER, SUGGESTION_LIMIT);
        return searchMapper.toSuggestionsResponse(rawPrefix, toFileHits(rows));
    }

    private String keyOf(String field, String normalized) {

        searchTermValidator.validate(new SearchTerm(field, normalized));
        return NodeNameNormalizer.key(normalized);
    }

    private List<FileHitResponse> toFileHits(List<FileRow> rows) {

        var folderIds = rows.stream()
            .flatMap(row -> AncestorPathParser.parseIds(row.ancestorPath()).stream())
            .collect(toSet());
        var names = fileSearchRepository.findNames(folderIds);
        return rows.stream()
            .filter(row -> names.keySet().containsAll(AncestorPathParser.parseIds(row.ancestorPath())))
            .map(row -> searchMapper.toFileHitResponse(row, folderPath(row.ancestorPath(), names)))
            .toList();
    }

    private static String folderPath(String ancestorPath, Map<Long, String> names) {

        return AncestorPathParser.parseIds(ancestorPath).stream()
            .skip(1)
            .map(names::get)
            .collect(joining("/", "/", ""));
    }
}
