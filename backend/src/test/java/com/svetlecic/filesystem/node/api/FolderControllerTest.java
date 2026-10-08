package com.svetlecic.filesystem.node.api;

import static com.svetlecic.filesystem.builder.CreateFolderRequestBuilder.aCreateFolderRequest;
import static com.svetlecic.filesystem.builder.FolderPageBuilder.aFolderPage;
import static com.svetlecic.filesystem.builder.FolderResponseBuilder.aRootFolderResponse;
import static com.svetlecic.filesystem.builder.NodeResponseBuilder.aNodeResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.svetlecic.filesystem.node.exception.NameAlreadyExistsException;
import com.svetlecic.filesystem.node.exception.NodeNotFoundException;
import com.svetlecic.filesystem.node.service.NodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(FolderController.class)
class FolderControllerTest {

    @Autowired
    MockMvcTester mockMvcTester;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    NodeService nodeService;

    @Test
    void shouldReturnRootFolder() {
        given(nodeService.getRoot()).willReturn(aRootFolderResponse().build());

        assertThat(mockMvcTester.get().uri("/api/v1/folders/root"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                {"id": 1, "name": "Home", "parentId": null}
                """);
    }

    @Test
    void shouldReturnPageWithCursorOfNextPage() {
        given(nodeService.getChildren(1L, null, 50))
            .willReturn(aFolderPage().item(aNodeResponse().build()).nextCursor("next").build());

        assertThat(mockMvcTester.get().uri("/api/v1/folders/1/children"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                {"items": [{"id": 2, "name": "Docs", "type": "FOLDER", "parentId": 1,
                            "createdAt": "2026-10-08T10:00:00Z"}],
                 "nextCursor": "next"}
                """);
    }

    @Test
    void shouldPassCursorAndSizeToService() {
        given(nodeService.getChildren(1L, "abc", 10)).willReturn(aFolderPage().build());

        assertThat(mockMvcTester.get().uri("/api/v1/folders/1/children?cursor=abc&size=10"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                {"items": [], "nextCursor": null}
                """);
    }

    @Test
    void shouldCreateFolderAndPointLocationAtIt() {
        given(nodeService.createFolder(1L, "Docs")).willReturn(aNodeResponse().build());

        assertThat(mockMvcTester.post().uri("/api/v1/folders/1/folders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(aCreateFolderRequest().build())))
            .hasStatus(201)
            .hasHeader("Location", "/api/v1/folders/2")
            .bodyJson()
            .extractingPath("$.name")
            .isEqualTo("Docs");
    }

    @Test
    void shouldTurnMissingFolderIntoProblemWithCode() {
        given(nodeService.getFolder(42L)).willThrow(NodeNotFoundException.folder(42));

        assertThat(mockMvcTester.get().uri("/api/v1/folders/42"))
            .hasStatus(404)
            .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
            .bodyJson()
            .extractingPath("$.code")
            .isEqualTo("NODE_NOT_FOUND");
    }

    @Test
    void shouldTurnDuplicateNameIntoConflict() {
        given(nodeService.createFolder(1L, "Docs")).willThrow(new NameAlreadyExistsException(1L));

        assertThat(mockMvcTester.post().uri("/api/v1/folders/1/folders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(aCreateFolderRequest().build())))
            .hasStatus(409)
            .bodyJson()
            .extractingPath("$.code")
            .isEqualTo("NAME_ALREADY_EXISTS");
    }

    @Test
    void shouldRejectTooLargePageBeforeReachingService() {
        assertThat(mockMvcTester.get().uri("/api/v1/folders/1/children?size=500"))
            .hasStatus(400)
            .bodyJson()
            .extractingPath("$.errors[0].code")
            .isEqualTo("Max");
        then(nodeService).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectMissingNameBeforeReachingService() {
        assertThat(mockMvcTester.post().uri("/api/v1/folders/1/folders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .hasStatus(400)
            .bodyJson()
            .extractingPath("$.errors[0].code")
            .isEqualTo("NotNull");
        then(nodeService).shouldHaveNoInteractions();
    }
}
