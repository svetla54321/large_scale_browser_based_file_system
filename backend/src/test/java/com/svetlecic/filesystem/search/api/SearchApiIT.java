package com.svetlecic.filesystem.search.api;

import static com.svetlecic.filesystem.builder.CreateFileRequestBuilder.aCreateFileRequest;
import static com.svetlecic.filesystem.builder.CreateFolderRequestBuilder.aCreateFolderRequest;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.IntegrationTest;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
class SearchApiIT {

    @Autowired
    MockMvcTester mockMvcTester;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JdbcClient jdbcClient;

    long rootId;

    @BeforeEach
    void deleteEverythingBelowRoot() {
        rootId = jdbcClient.sql("select id from node where parent_id is null").query(Long.class).single();
        jdbcClient.sql("delete from node where parent_id = :root").param("root", rootId).update();
    }

    @Test
    void shouldFindOnlyFilesWithExactlyThatNameIgnoringCase() {
        var docsId = createFolder(rootId, "Docs");
        var workId = createFolder(docsId, "Work");
        createFile(workId, "REPORT.PDF");
        createFile(rootId, "Report.pdf");
        createFile(docsId, "report.pdf.bak");
        createFolder(docsId, "report.pdf");

        var page = search("report.pdf", null, null, 50);

        assertThat(hits(page)).containsExactly("Report.pdf in /", "REPORT.PDF in /Docs/Work");
        assertThat(page.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void shouldNotMatchPartOfAName() {
        createFile(rootId, "report.pdf");

        assertThat(search("report", null, null, 50).get("items").isEmpty()).isTrue();
    }

    @Test
    void shouldSearchTheWholeSubtreeOfTheFolderAndNothingElse() {
        var docsId = createFolder(rootId, "Docs");
        var deepId = createFolder(createFolder(docsId, "a"), "b");
        var otherId = createFolder(rootId, "Other");
        createFile(deepId, "plan.txt");
        createFile(docsId, "plan.txt");
        createFile(otherId, "plan.txt");
        createFile(rootId, "plan.txt");

        assertThat(hits(search("plan.txt", docsId, null, 50)))
            .containsExactly("plan.txt in /Docs", "plan.txt in /Docs/a/b");
    }

    @Test
    void shouldPageThroughAllHitsExactlyOnce() {
        IntStream.rangeClosed(1, 7).forEach(i -> createFile(createFolder(rootId, "project-" + i), "README.md"));

        var pages = allPages("readme.md", 3);

        assertThat(pages).hasSize(3);
        assertThat(pages.stream().flatMap(List::stream).toList()).hasSize(7).doesNotHaveDuplicates();
    }

    @Test
    void shouldAnswerUnknownFolderOrFileIdWithNotFound() {
        var fileId = createFile(rootId, "notes.txt");

        assertThat(mockMvcTester.get().uri("/api/v1/search/files?name=a&folderId={id}", Long.MAX_VALUE))
            .hasStatus(404);
        assertThat(mockMvcTester.get().uri("/api/v1/search/files?name=a&folderId={id}", fileId)).hasStatus(404);
    }

    @Test
    void shouldSuggestAtMostTenFilesOrderedByName() {
        var docsId = createFolder(rootId, "Docs");
        IntStream.iterate(12, i -> i >= 1, i -> i - 1)
            .forEach(i -> createFile(i % 2 == 0 ? rootId : docsId, "report-%02d.pdf".formatted(i)));
        createFolder(rootId, "rep");
        createFolder(docsId, "report");
        createFile(rootId, "notes.txt");

        var suggestions = suggest("Rep");

        assertThat(suggestions.get("prefix").asString()).isEqualTo("Rep");
        assertThat(names(suggestions)).containsExactly(IntStream.rangeClosed(1, 10)
            .mapToObj(i -> "report-%02d.pdf".formatted(i)).toArray(String[]::new));
    }

    @Test
    void shouldTreatPercentAndUnderscoreAsOrdinaryCharacters() {
        List.of("50%.txt", "500.txt", "a_b.txt", "axb.txt").forEach(name -> createFile(rootId, name));

        assertThat(names(suggest("50%"))).containsExactly("50%.txt");
        assertThat(names(suggest("a_"))).containsExactly("a_b.txt");
    }

    @Test
    void shouldMatchAccentedNamesWhateverTheCaseOrFormOfTheInput() {
        var name = Character.toString(0x010C) + "evapi.txt";
        createFile(rootId, name);

        assertThat(names(suggest(Character.toString(0x010D) + "ev"))).containsExactly(name);
        assertThat(names(suggest("C" + Character.toString(0x030C) + "EV"))).containsExactly(name);
        assertThat(hits(search(name.toUpperCase(), null, null, 50))).containsExactly(name + " in /");
    }

    private JsonNode search(String name, @Nullable Long folderId, @Nullable String cursor, int size) {
        var request = mockMvcTester.get().uri("/api/v1/search/files")
            .param("name", name)
            .param("size", String.valueOf(size));
        if (nonNull(folderId)) {
            request.param("folderId", String.valueOf(folderId));
        }
        if (nonNull(cursor)) {
            request.param("cursor", cursor);
        }
        var result = request.exchange();
        assertThat(result).hasStatusOk();
        return body(result);
    }

    private List<List<String>> allPages(String name, int size) {
        var pages = new ArrayList<List<String>>();
        String cursor = null;
        do {
            var page = search(name, null, cursor, size);
            pages.add(hits(page));
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asString();
        } while (nonNull(cursor) && pages.size() < 10);
        return pages;
    }

    private JsonNode suggest(String prefix) {
        var result = mockMvcTester.get().uri("/api/v1/search/suggestions").param("prefix", prefix).exchange();
        assertThat(result).hasStatusOk();
        return body(result);
    }

    private static List<String> hits(JsonNode page) {
        return page.get("items").valueStream()
            .map(item -> item.get("name").asString() + " in " + item.get("folderPath").asString())
            .toList();
    }

    private static List<String> names(JsonNode response) {
        return response.get("items").valueStream().map(item -> item.get("name").asString()).toList();
    }

    private long createFolder(long parentId, String name) {
        return idOf(mockMvcTester.post().uri("/api/v1/folders/{id}/folders", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(aCreateFolderRequest().name(name).build()))
            .exchange());
    }

    private long createFile(long parentId, String name) {
        return idOf(mockMvcTester.post().uri("/api/v1/folders/{id}/files", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(aCreateFileRequest().name(name).build()))
            .exchange());
    }

    private void insertFolder(long id, long parentId, String name) {
        jdbcClient.sql("""
                insert into node (id, parent_id, type, name, name_key, ancestor_path)
                overriding system value
                select :id, id, 'FOLDER', :name, :key, ancestor_path || id || '/' from node where id = :parent
                """)
            .param("id", id)
            .param("name", name)
            .param("key", NodeNameNormalizer.key(name))
            .param("parent", parentId)
            .update();
    }

    private long idOf(MvcTestResult result) {
        assertThat(result).hasStatus(201);
        return body(result).get("id").asLong();
    }

    private JsonNode body(MvcTestResult result) {
        return jsonMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
