package com.svetlecic.filesystem.builder;

import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeResponseBuilder {

    public static final long DEFAULT_ID = 2L;
    public static final String DEFAULT_NAME = "Docs";
    public static final NodeType DEFAULT_TYPE = NodeType.FOLDER;
    public static final long DEFAULT_PARENT_ID = 1L;
    public static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-10-08T10:00:00Z");

    private long id = DEFAULT_ID;
    private String name = DEFAULT_NAME;
    private NodeType type = DEFAULT_TYPE;
    private long parentId = DEFAULT_PARENT_ID;
    private Instant createdAt = DEFAULT_CREATED_AT;

    public static NodeResponseBuilder aNodeResponse() {
        return new NodeResponseBuilder();
    }

    public NodeResponseBuilder id(long id) {
        this.id = id;
        return this;
    }

    public NodeResponseBuilder name(String name) {
        this.name = name;
        return this;
    }

    public NodeResponseBuilder type(NodeType type) {
        this.type = type;
        return this;
    }

    public NodeResponseBuilder parentId(long parentId) {
        this.parentId = parentId;
        return this;
    }

    public NodeResponseBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public NodeResponse build() {
        return new NodeResponse(id, name, type, parentId, createdAt);
    }
}
