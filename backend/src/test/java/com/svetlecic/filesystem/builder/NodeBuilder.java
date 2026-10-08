package com.svetlecic.filesystem.builder;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import com.svetlecic.filesystem.node.domain.NodeType;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeBuilder {

    public static final long DEFAULT_ID = 2L;
    public static final Long DEFAULT_PARENT_ID = 1L;
    public static final NodeType DEFAULT_TYPE = NodeType.FOLDER;
    public static final String DEFAULT_NAME = "Docs";
    public static final String DEFAULT_ANCESTOR_PATH = "/1/";
    public static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-10-08T10:00:00Z");

    private long id = DEFAULT_ID;
    private Long parentId = DEFAULT_PARENT_ID;
    private NodeType type = DEFAULT_TYPE;
    private String name = DEFAULT_NAME;
    private String ancestorPath = DEFAULT_ANCESTOR_PATH;
    private Instant createdAt = DEFAULT_CREATED_AT;

    public static NodeBuilder aNode() {
        return new NodeBuilder();
    }

    public static NodeBuilder aRootFolder() {
        return new NodeBuilder().id(1L).parentId(null).name("Home").ancestorPath("/");
    }

    public NodeBuilder id(long id) {
        this.id = id;
        return this;
    }

    public NodeBuilder parentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public NodeBuilder type(NodeType type) {
        this.type = type;
        return this;
    }

    public NodeBuilder name(String name) {
        this.name = name;
        return this;
    }

    public NodeBuilder ancestorPath(String ancestorPath) {
        this.ancestorPath = ancestorPath;
        return this;
    }

    public NodeBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Node build() {
        var node = BeanUtils.instantiateClass(Node.class);
        ReflectionTestUtils.setField(node, "id", id);
        ReflectionTestUtils.setField(node, "version", 0L);
        ReflectionTestUtils.setField(node, "parentId", parentId);
        ReflectionTestUtils.setField(node, "type", type);
        ReflectionTestUtils.setField(node, "name", name);
        ReflectionTestUtils.setField(node, "nameKey", NodeNameNormalizer.key(name));
        ReflectionTestUtils.setField(node, "ancestorPath", ancestorPath);
        ReflectionTestUtils.setField(node, "createdAt", createdAt);
        ReflectionTestUtils.setField(node, "updatedAt", createdAt);
        return node;
    }
}
