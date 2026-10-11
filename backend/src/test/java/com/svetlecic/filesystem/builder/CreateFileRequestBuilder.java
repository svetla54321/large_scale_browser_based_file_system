package com.svetlecic.filesystem.builder;

import com.svetlecic.filesystem.node.dto.CreateFileRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateFileRequestBuilder {

    public static final String DEFAULT_NAME = "report.pdf";

    private String name = DEFAULT_NAME;

    public static CreateFileRequestBuilder aCreateFileRequest() {
        return new CreateFileRequestBuilder();
    }

    public CreateFileRequestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CreateFileRequest build() {
        return new CreateFileRequest(name);
    }
}
