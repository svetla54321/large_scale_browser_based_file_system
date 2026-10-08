package com.svetlecic.filesystem.builder;

import com.svetlecic.filesystem.node.dto.CreateFolderRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateFolderRequestBuilder {

    public static final String DEFAULT_NAME = "Docs";

    private String name = DEFAULT_NAME;

    public static CreateFolderRequestBuilder aCreateFolderRequest() {
        return new CreateFolderRequestBuilder();
    }

    public CreateFolderRequestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CreateFolderRequest build() {
        return new CreateFolderRequest(name);
    }
}
