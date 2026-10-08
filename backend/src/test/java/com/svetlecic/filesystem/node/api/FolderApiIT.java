package com.svetlecic.filesystem.node.api;

import static com.svetlecic.filesystem.builder.CreateFolderRequestBuilder.aCreateFolderRequest;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.TestcontainersConfiguration;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
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
    void shouldReturnRootFolder() {
        assertThat(mockMvcTester.get().uri("/api/v1/folders/root"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id": %d, "name": "Home", "parentId": null}
                        """.formatted(rootId));
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
    void shouldStoreTrimmedNameButKeepItsCase() {
        var created = createFolder(rootId, "  Quarterly Reports  ");

        assertThat(created).hasStatus(201);
        assertThat(body(created).get("name").asString()).isEqualTo("Quarterly Reports");
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
    void shouldTreatComposedAndDecomposedCharactersAsTheSameName() {
        createFolder(rootId, "\u010Cevapi");

        assertThat(createFolder(rootId, "C\u030Cevapi")).hasStatus(409);
    }

    @Test
    void shouldAllowSameNameInDifferentFolders() {
        var firstId = body(createFolder(rootId, "a")).get("id").asLong();
        var secondId = body(createFolder(rootId, "b")).get("id").asLong();

        assertThat(createFolder(firstId, "shared")).hasStatus(201);
        assertThat(createFolder(secondId, "shared")).hasStatus(201);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "a/b", "..", "bad\u0000name"})
    void shouldRejectInvalidName(String name) {
        assertThat(createFolder(rootId, name))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(childNames(rootId)).isEmpty();
    }

    @Test
    void shouldRejectMissingName() {
        assertThat(mockMvcTester.post().uri("/api/v1/folders/{id}/folders", rootId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(400);
    }

    @Test
    void shouldReturnNotFoundForUnknownParent() {
        var result = createFolder(Long.MAX_VALUE, "Docs");

        assertThat(result).hasStatus(404);
        assertThat(body(result).get("code").asString()).isEqualTo("NODE_NOT_FOUND");
        assertThat(body(result).get("detail").asString()).isEqualTo("Folder %d not found".formatted(Long.MAX_VALUE));
    }

    @Test
    void shouldReturnBadRequestForNonNumericId() {
        assertThat(mockMvcTester.get().uri("/api/v1/folders/abc")).hasStatus(400);
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
    void shouldNotReturnCursorWhenPageIsExactlyFull() {
        createFolder(rootId, "a");
        createFolder(rootId, "b");

        assertThat(children(rootId, null, 2).get("nextCursor").isNull()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 201})
    void shouldRejectPageSizeOutOfRange(int size) {
        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}/children?size={size}", rootId, size)).hasStatus(400);
    }

    @Test
    void shouldRejectInvalidCursor() {
        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}/children?cursor=garbage!", rootId)).hasStatus(400);
    }

    @Test
    void shouldReturnNotFoundForChildrenOfUnknownFolder() {
        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}/children", Long.MAX_VALUE)).hasStatus(404);
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
    void shouldTreatEmptyCursorAsFirstPage() {
        createFolder(rootId, "Docs");

        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}/children?cursor=", rootId))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[0].name")
                .isEqualTo("Docs");
    }

    @Test
    void shouldRejectForgedCursorWithControlCharacter() {
        var forged = Base64.getUrlEncoder().encodeToString(
                ("FOLDER" + Character.toString(10) + Character.toString(0)).getBytes(StandardCharsets.UTF_8));

        assertThat(mockMvcTester.get().uri("/api/v1/folders/{id}/children", rootId).param("cursor", forged))
                .hasStatus(400);
    }

    @Test
    void shouldRejectNameThatOnlyLooksBlank() {
        assertThat(createFolder(rootId, Character.toString(0x00A0))).hasStatus(400);
    }

    @Test
    void shouldRejectNonTextualName() {
        assertThat(mockMvcTester.post().uri("/api/v1/folders/{id}/folders", rootId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": 123}"))
                .hasStatus(400);
        assertThat(childNames(rootId)).isEmpty();
    }

    @Test
    void shouldReturnLocationThatResolvesForEquivalentVersionSpelling() {
        var created = mockMvcTester.post().uri("/api/v1.0/folders/{id}/folders", rootId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Docs\"}")
                .exchange();

        assertThat(created).hasStatus(201);
        var location = created.getResponse().getHeader("Location");
        assertThat(location).isEqualTo("/api/v1.0/folders/" + body(created).get("id").asLong());
        assertThat(mockMvcTester.get().uri(location)).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Docs");
    }

    @Test
    void shouldNameTheInvalidInputInValidationErrors() {
        var missingName = mockMvcTester.post().uri("/api/v1/folders/{id}/folders", rootId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange();
        var tooLargePage = mockMvcTester.get().uri("/api/v1/folders/{id}/children?size=500", rootId).exchange();

        assertThat(body(missingName).get("detail").asString()).startsWith("name:");
        assertThat(body(missingName).get("code").asString()).isEqualTo("VALIDATION_FAILED");
        assertThat(body(missingName).at("/errors/0/field").asString()).isEqualTo("name");
        assertThat(body(missingName).at("/errors/0/code").asString()).isEqualTo("NotNull");
        assertThat(body(tooLargePage).get("detail").asString()).startsWith("size:");
        assertThat(body(tooLargePage).at("/errors/0/field").asString()).isEqualTo("size");
        assertThat(body(tooLargePage).at("/errors/0/code").asString()).isEqualTo("Max");
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
    void shouldPutRequestIdIntoErrorBodyAndHeader() {
        var result = mockMvcTester.get().uri("/api/v1/folders/{id}", Long.MAX_VALUE)
            .header("X-Request-Id", "trace-me-42")
            .exchange();

        assertThat(result).hasStatus(404).hasHeader("X-Request-Id", "trace-me-42");
        assertThat(body(result).get("requestId").asString()).isEqualTo("trace-me-42");
    }

    private void insertFile(long parentId, String name) {
        jdbcClient.sql("""
                        insert into node (parent_id, type, name, name_key, ancestor_path)
                        select id, 'FILE', :name, :key, ancestor_path || id || '/' from node where id = :parent
                        """)
                .param("name", name)
                .param("key", NodeNameNormalizer.key(name))
                .param("parent", parentId)
                .update();
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
