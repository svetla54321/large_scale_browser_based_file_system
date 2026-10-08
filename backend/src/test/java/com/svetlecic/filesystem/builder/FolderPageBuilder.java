package com.svetlecic.filesystem.builder;

import com.svetlecic.filesystem.node.dto.FolderPage;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FolderPageBuilder {

    private final List<NodeResponse> items = new ArrayList<>();
    private String nextCursor;

    public static FolderPageBuilder aFolderPage() {
        return new FolderPageBuilder();
    }

    public FolderPageBuilder item(NodeResponse item) {
        items.add(item);
        return this;
    }

    public FolderPageBuilder nextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
        return this;
    }

    public FolderPage build() {
        return new FolderPage(List.copyOf(items), nextCursor);
    }
}
