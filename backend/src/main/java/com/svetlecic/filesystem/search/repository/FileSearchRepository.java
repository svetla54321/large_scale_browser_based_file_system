package com.svetlecic.filesystem.search.repository;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FileSearchRepository {

    private static final String FILES_BY_NAME = """
        select id, name, parent_id, ancestor_path
        from node
        where type = 'FILE'
          and name_key = :key
          and ancestor_path >= :from
          and ancestor_path < :to
          and ancestor_path > :after
        order by ancestor_path
        limit :limit
        """;

    private static final String FILES_BY_NAME_PREFIX = """
        select id, name, parent_id, ancestor_path
        from node
        where type = 'FILE'
          and name_key >= :from
          and name_key < :to
        order by name_key, ancestor_path
        limit :limit
        """;

    private static final String NAMES_BY_ID = """
        select id, name
        from node
        where id in (:ids)
        """;

    private final JdbcClient jdbcClient;

    public List<FileRow> findByName(String key, String from, String to, String after, int limit) {

        return jdbcClient.sql(FILES_BY_NAME)
            .param("key", key)
            .param("from", from)
            .param("to", to)
            .param("after", after)
            .param("limit", limit)
            .query(FileRow.class)
            .list();
    }

    public List<FileRow> findByNamePrefix(String from, String to, int limit) {

        return jdbcClient.sql(FILES_BY_NAME_PREFIX)
            .param("from", from)
            .param("to", to)
            .param("limit", limit)
            .query(FileRow.class)
            .list();
    }

    public Map<Long, String> findNames(Collection<Long> ids) {

        if (ids.isEmpty()) {
            return Map.of();
        }
        return jdbcClient.sql(NAMES_BY_ID)
            .param("ids", ids)
            .query((row, rowNumber) -> Map.entry(row.getLong("id"), row.getString("name")))
            .stream()
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
