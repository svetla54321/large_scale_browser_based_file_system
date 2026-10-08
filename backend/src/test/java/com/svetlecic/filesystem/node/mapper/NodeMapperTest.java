package com.svetlecic.filesystem.node.mapper;

import static com.svetlecic.filesystem.builder.FolderResponseBuilder.aFolderResponse;
import static com.svetlecic.filesystem.builder.FolderResponseBuilder.aRootFolderResponse;
import static com.svetlecic.filesystem.builder.NodeBuilder.aNode;
import static com.svetlecic.filesystem.builder.NodeBuilder.aRootFolder;
import static com.svetlecic.filesystem.builder.NodeResponseBuilder.aNodeResponse;
import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.node.domain.NodeType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class NodeMapperTest {

    private final NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);

    @Test
    void shouldMapNodeToNodeResponse() {
        var node = aNode().id(7L).name("Reports").type(NodeType.FILE).build();

        assertThat(nodeMapper.toNodeResponse(node))
            .isEqualTo(aNodeResponse().id(7L).name("Reports").type(NodeType.FILE).build());
    }

    @Test
    void shouldMapFolderToFolderResponse() {
        assertThat(nodeMapper.toFolderResponse(aNode().build())).isEqualTo(aFolderResponse().build());
    }

    @Test
    void shouldMapRootFolderWithoutParent() {
        assertThat(nodeMapper.toFolderResponse(aRootFolder().build())).isEqualTo(aRootFolderResponse().build());
    }

    @Test
    void shouldKeepOrderWhenMappingList() {
        var nodes = List.of(aNode().id(3L).name("b").build(), aNode().id(4L).name("a").build());

        assertThat(nodeMapper.toNodeResponses(nodes))
            .containsExactly(aNodeResponse().id(3L).name("b").build(), aNodeResponse().id(4L).name("a").build());
    }
}
