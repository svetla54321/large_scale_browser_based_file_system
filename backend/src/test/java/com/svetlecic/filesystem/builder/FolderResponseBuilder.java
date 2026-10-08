package com.svetlecic.filesystem.builder;

import com.svetlecic.filesystem.node.dto.FolderResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FolderResponseBuilder {

    public static final long DEFAULT_ID = 2L;
    public static final String DEFAULT_NAME = "Docs";
    public static final Long DEFAULT_PARENT_ID = 1L;

    private long id = DEFAULT_ID;
    private String name = DEFAULT_NAME;
    private Long parentId = DEFAULT_PARENT_ID;

    public static FolderResponseBuilder aFolderResponse() {
        return new FolderResponseBuilder();
    }

    public static FolderResponseBuilder aRootFolderResponse() {
        return new FolderResponseBuilder().id(1L).name("Home").parentId(null);
    }

    public FolderResponseBuilder id(long id) {
        this.id = id;
        return this;
    }

    public FolderResponseBuilder name(String name) {
        this.name = name;
        return this;
    }

    public FolderResponseBuilder parentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public FolderResponse build() {
        return new FolderResponse(id, name, parentId);
    }
}
