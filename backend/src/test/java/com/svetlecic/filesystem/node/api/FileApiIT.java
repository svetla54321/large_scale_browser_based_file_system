package com.svetlecic.filesystem.node.api;

import static com.svetlecic.filesystem.builder.CreateFileRequestBuilder.aCreateFileRequest;
import static com.svetlecic.filesystem.builder.CreateFolderRequestBuilder.aCreateFolderRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.IntegrationTest;
import java.util.List;
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
class FileApiIT {

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
    void shouldShareNamesBetweenFilesAndFolders() {
        createFolder(rootId, "Docs");
        createFile(rootId, "notes.txt");

        var fileLikeFolder = createFile(rootId, "DOCS");
        var folderLikeFile = createFolder(rootId, "Notes.TXT");

        assertThat(fileLikeFolder).hasStatus(409);
        assertThat(body(fileLikeFolder).get("code").asString()).isEqualTo("NAME_ALREADY_EXISTS");
        assertThat(folderLikeFile).hasStatus(409);
        assertThat(childNames(rootId)).containsExactly("Docs", "notes.txt");
    }

    @Test
    void shouldDeleteFileOnceAndAnswerNotFoundAfterwards() {
        var fileId = idOf(createFile(rootId, "notes.txt"));

        assertThat(mockMvcTester.delete().uri("/api/v1/files/{id}", fileId)).hasStatus(204);
        var again = mockMvcTester.delete().uri("/api/v1/files/{id}", fileId).exchange();

        assertThat(again).hasStatus(404);
        assertThat(body(again).get("detail").asString()).isEqualTo("File %d not found".formatted(fileId));
        assertThat(childNames(rootId)).isEmpty();
    }

    private MvcTestResult createFile(long parentId, String name) {
        return mockMvcTester.post().uri("/api/v1/folders/{id}/files", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(aCreateFileRequest().name(name).build()))
            .exchange();
    }

    private MvcTestResult createFolder(long parentId, String name) {
        return mockMvcTester.post().uri("/api/v1/folders/{id}/folders", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(aCreateFolderRequest().name(name).build()))
            .exchange();
    }

    private List<String> childNames(long folderId) {
        var page = body(mockMvcTester.get().uri("/api/v1/folders/{id}/children", folderId).exchange());
        return page.get("items").valueStream().map(item -> item.get("name").asString()).toList();
    }

    private long idOf(MvcTestResult result) {
        assertThat(result).hasStatus(201);
        return body(result).get("id").asLong();
    }

    private JsonNode body(MvcTestResult result) {
        return jsonMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
