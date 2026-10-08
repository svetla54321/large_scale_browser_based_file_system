package com.svetlecic.filesystem.node.domain;

import static com.svetlecic.filesystem.builder.NodeBuilder.aNode;
import static com.svetlecic.filesystem.builder.NodeBuilder.aRootFolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.node.exception.MaxDepthExceededException;
import org.junit.jupiter.api.Test;

class NodeTest {

    @Test
    void shouldAppendParentIdToChildPath() {
        var parent = aNode().id(42L).ancestorPath("/1/").build();

        var child = parent.newChild(NodeType.FILE, "Notes.txt");

        assertThat(child.getParentId()).isEqualTo(42L);
        assertThat(child.getAncestorPath()).isEqualTo("/1/42/");
        assertThat(child.getDepth()).isEqualTo(2);
        assertThat(child.getNameKey()).isEqualTo("notes.txt");
    }

    @Test
    void shouldPutRootAtDepthZero() {
        assertThat(aRootFolder().build().getDepth()).isZero();
    }

    @Test
    void shouldAllowChildAtMaxDepth() {
        var parent = aNode().ancestorPath(pathOfDepth(Node.MAX_DEPTH - 1)).build();

        assertThat(parent.newChild(NodeType.FOLDER, "last").getDepth()).isEqualTo(Node.MAX_DEPTH);
    }

    @Test
    void shouldRejectChildOfFolderAtMaxDepth() {
        var deepest = aNode().ancestorPath(pathOfDepth(Node.MAX_DEPTH)).build();

        assertThatThrownBy(() -> deepest.newChild(NodeType.FOLDER, "too-deep"))
            .isInstanceOf(MaxDepthExceededException.class);
    }

    private static String pathOfDepth(int depth) {
        return "/" + "7/".repeat(depth);
    }
}
