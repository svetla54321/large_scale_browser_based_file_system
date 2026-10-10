package com.svetlecic.filesystem;

import static com.svetlecic.filesystem.builder.CreateFileRequestBuilder.aCreateFileRequest;
import static com.svetlecic.filesystem.builder.CreateFolderRequestBuilder.aCreateFolderRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.svetlecic.filesystem.node.dto.FolderResponse;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import com.svetlecic.filesystem.search.dto.FileHitResponse;
import com.svetlecic.filesystem.search.dto.SearchPageResponse;
import com.svetlecic.filesystem.search.dto.SuggestionsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class FileSystemFlowIT {

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void deleteEverythingBelowRoot() {
        jdbcClient.sql("delete from node where parent_id = (select id from node where parent_id is null)").update();
    }

    @Test
    void shouldCreateFindAndDeleteFilesOverHttp() {
        var root = get("/api/v1/folders/root", FolderResponse.class);
        var docs = create("/api/v1/folders/{id}/folders", root.id(), aCreateFolderRequest().name("Docs").build());
        var work = create("/api/v1/folders/{id}/folders", docs.id(), aCreateFolderRequest().name("Work").build());
        var report = create("/api/v1/folders/{id}/files", work.id(), aCreateFileRequest().name("Report.pdf").build());
        var readme = create("/api/v1/folders/{id}/files", root.id(), aCreateFileRequest().name("readme.md").build());

        restTestClient.post()
            .uri("/api/v1/folders/{id}/files", work.id())
            .contentType(MediaType.APPLICATION_JSON)
            .body(aCreateFileRequest().name("REPORT.PDF").build())
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("NAME_ALREADY_EXISTS")
            .jsonPath("$.requestId").isNotEmpty();

        var everywhere = get("/api/v1/search/files?name=report.pdf", SearchPageResponse.class);
        var inDocs = get("/api/v1/search/files?name=report.pdf&folderId=" + docs.id(), SearchPageResponse.class);
        var suggestions = get("/api/v1/search/suggestions?prefix=re", SuggestionsResponse.class);

        assertThat(everywhere.items())
            .extracting(FileHitResponse::id, FileHitResponse::name, FileHitResponse::parentId, FileHitResponse::folderPath)
            .containsExactly(tuple(report.id(), "Report.pdf", work.id(), "/Docs/Work"));
        assertThat(inDocs.items()).extracting(FileHitResponse::id).containsExactly(report.id());
        assertThat(suggestions.items()).extracting(FileHitResponse::id).containsExactly(readme.id(), report.id());

        restTestClient.delete().uri("/api/v1/folders/{id}", docs.id()).exchange().expectStatus().isNoContent();

        restTestClient.get().uri("/api/v1/files/{id}", report.id()).exchange().expectStatus().isNotFound();
        restTestClient.get().uri("/api/v1/folders/{id}", work.id()).exchange().expectStatus().isNotFound();
        assertThat(get("/api/v1/search/files?name=report.pdf", SearchPageResponse.class).items()).isEmpty();
        assertThat(get("/api/v1/search/suggestions?prefix=re", SuggestionsResponse.class).items())
            .extracting(FileHitResponse::id)
            .containsExactly(readme.id());
    }

    private <T> T get(String uri, Class<T> type) {
        return restTestClient.get()
            .uri(uri)
            .exchange()
            .expectStatus().isOk()
            .expectBody(type)
            .returnResult()
            .getResponseBody();
    }

    private NodeResponse create(String uri, long parentId, Object request) {
        var result = restTestClient.post()
            .uri(uri, parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(NodeResponse.class)
            .returnResult();
        var created = result.getResponseBody();
        var location = result.getResponseHeaders().getLocation();

        assertThat(location).isNotNull();
        restTestClient.get()
            .uri(location.toString())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(created.id());
        return created;
    }
}
