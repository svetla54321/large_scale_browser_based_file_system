package com.svetlecic.filesystem.node.mapper;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.dto.BreadcrumbResponse;
import com.svetlecic.filesystem.node.dto.FolderPageResponse;
import com.svetlecic.filesystem.node.dto.FolderResponse;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface NodeMapper {

    NodeResponse toNodeResponse(Node node);

    List<NodeResponse> toNodeResponses(List<Node> nodes);

    @Mapping(target = "items", source = "nodes")
    @Mapping(target = "nextCursor", source = "nextCursor")
    @Mapping(target = "totalItems", source = "totalItems")
    FolderPageResponse toFolderPageResponse(List<Node> nodes, @Nullable String nextCursor, long totalItems);

    @Mapping(target = "id", source = "folder.id")
    @Mapping(target = "name", source = "folder.name")
    @Mapping(target = "parentId", source = "folder.parentId")
    @Mapping(target = "breadcrumbs", source = "ancestors")
    FolderResponse toFolderResponse(Node folder, List<Node> ancestors);

    BreadcrumbResponse toBreadcrumbResponse(Node node);
}
