package com.svetlecic.filesystem.node.api;

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
class FolderApiIT {

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
    void shouldCreateFolderAndListItInParent() {
        var created = createFolder(rootId, "Docs");

        assertThat(created).hasStatus(201);
        var folder = body(created);
        var id = folder.get("id").asLong();
        assertThat(folder.get("name").asString()).isEqualTo("Docs");
        assertThat(folder.get("type").asString()).isEqualTo("FOLDER");
        assertThat(folder.get("parentId").asLong()).isEqualTo(rootId);
        assertThat(folder.get("createdAt").isNull()).isFalse();
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/v1/folders/" + id);
        assertThat(childNames(rootId)).containsExactly("Docs");
        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}", id)).hasStatusOk();
    }

    @Test
    void shouldRejectNameThatDiffersOnlyInCase() {
        createFolder(rootId, "Docs");

        var duplicate = createFolder(rootId, "DOCS");

        assertThat(duplicate).hasStatus(409).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(body(duplicate).get("code").asString()).isEqualTo("NAME_ALREADY_EXISTS");
        assertThat(body(duplicate).get("detail").asString()).contains("already exists");
        assertThat(childNames(rootId)).containsExactly("Docs");
    }

    @Test
    void shouldReturnNotFoundForUnknownParent() {
        var result = createFolder(Long.MAX_VALUE, "Docs");

        assertThat(result).hasStatus(404);
        assertThat(body(result).get("code").asString()).isEqualTo("NODE_NOT_FOUND");
        assertThat(body(result).get("detail").asString()).isEqualTo("Folder %d not found".formatted(Long.MAX_VALUE));
    }

    @Test
    void shouldPageThroughChildrenWithoutGapsOrDuplicates() {
        List.of("delta", "alpha", "golf", "charlie", "echo", "bravo", "foxtrot")
                .forEach(name -> createFolder(rootId, name));

        var seen = new ArrayList<String>();
        String cursor = null;
        var pages = 0;
        do {
            var page = children(rootId, cursor, 3);
            seen.addAll(names(page));
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asString();
            pages++;
        } while (nonNull(cursor));

        assertThat(pages).isEqualTo(3);
        assertThat(seen).containsExactly("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf");
    }

    @Test
    void shouldListFoldersBeforeFilesAndKeepThatOrderForEveryPageSize() {
        List.of("beta", "Alpha", "Zeta").forEach(name -> createFolder(rootId, name));
        List.of("c.txt", "a.txt", "B.txt").forEach(name -> insertFile(rootId, name));
        var expected = List.of("Alpha", "beta", "Zeta", "a.txt", "B.txt", "c.txt");

        IntStream.rangeClosed(1, expected.size() + 1)
                .forEach(size -> assertThat(allChildNames(rootId, size)).as("page size %d", size).isEqualTo(expected));
    }

    @Test
    void shouldListEveryBrokenNameRuleWithItsCode() {
        var result = createFolder(rootId, "a/b".repeat(100));

        assertThat(result).hasStatus(400);
        var problem = body(result);
        assertThat(problem.get("code").asString()).isEqualTo("VALIDATION_FAILED");
        assertThat(problem.get("errors").valueStream().map(error -> error.get("code").asString()).toList())
            .containsExactly("NAME_TOO_LONG", "NAME_FORBIDDEN_CHARACTER");
    }

    @Test
    void shouldReturnBreadcrumbsFromRootDownToParent() {
        var docsId = body(createFolder(rootId, "Docs")).get("id").asLong();
        var reportsId = body(createFolder(docsId, "Reports")).get("id").asLong();

        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}", reportsId))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": %d, "name": "Reports", "parentId": %d,
                         "breadcrumbs": [{"id": %d, "name": "Home"}, {"id": %d, "name": "Docs"}]}
                        """.formatted(reportsId, docsId, rootId, docsId));
        assertThat(mockMvcTester.get().uri("/api/v1/folders/root"))
                .bodyJson()
                .extractingPath("$.breadcrumbs")
                .asArray()
                .isEmpty();
    }

    @Test
    void shouldDeleteFolderWithItsWholeSubtree() {
        var aId = body(createFolder(rootId, "a")).get("id").asLong();
        var bId = body(createFolder(aId, "b")).get("id").asLong();
        var cId = body(createFolder(bId, "c")).get("id").asLong();
        insertFile(cId, "deep.txt");
        insertFile(aId, "top.txt");
        createFolder(rootId, "keep");
        var before = countNodes();

        assertThat(mockMvcTester.delete().uri("/api/v1/folders/{id}", aId)).hasStatus(204);

        assertThat(countNodes()).isEqualTo(before - 5);
        assertThat(childNames(rootId)).containsExactly("keep");
        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}", cId)).hasStatus(404);
    }

    @Test
    void shouldRefuseToDeleteRootFolder() {
        var result = mockMvcTester.delete().uri("/api/v1/folders/{id}", rootId).exchange();

        assertThat(result).hasStatus(409);
        assertThat(body(result).get("code").asString()).isEqualTo("ROOT_FOLDER_NOT_DELETABLE");
        assertThat(mockMvcTester.get().uri("/api/v1/folders/root")).hasStatusOk();
    }

    private long countNodes() {
        return jdbcClient.sql("select count(*) from node").query(Long.class).single();
    }

    private long insertFile(long parentId, String name) {
        return jdbcClient.sql("""
                        insert into node (parent_id, type, name, name_key, ancestor_path)
                        select id, 'FILE', :name, :key, ancestor_path || id || '/' from node where id = :parent
                        returning id
                        """)
                .param("name", name)
                .param("key", NodeNameNormalizer.key(name))
                .param("parent", parentId)
                .query(Long.class)
                .single();
    }

    private List<String> allChildNames(long folderId, int pageSize) {
        var names = new ArrayList<String>();
        String cursor = null;
        do {
            var page = children(folderId, cursor, pageSize);
            names.addAll(names(page));
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asString();
        } while (nonNull(cursor));
        return names;
    }

    private MvcTestResult createFolder(long parentId, String name) {
        return mockMvcTester.post().uri("/api/v1/folders/{id}/folders", parentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(aCreateFolderRequest().name(name).build()))
                .exchange();
    }

    private List<String> childNames(long folderId) {
        return names(children(folderId, null, 200));
    }

    private JsonNode children(long folderId, @Nullable String cursor, int size) {
        var request = mockMvcTester.get().uri("/api/v1/folders/{id}/children", folderId)
                .param("size", String.valueOf(size));
        if (nonNull(cursor)) {
            request.param("cursor", cursor);
        }
        var result = request.exchange();
        assertThat(result).hasStatusOk();
        return body(result);
    }

    private static List<String> names(JsonNode page) {
        return page.get("items").valueStream()
                .map(item -> item.get("name").asString())
                .toList();
    }

    private JsonNode body(MvcTestResult result) {
        return jsonMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
