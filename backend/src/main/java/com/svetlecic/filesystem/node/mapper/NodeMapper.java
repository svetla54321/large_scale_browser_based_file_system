package com.svetlecic.filesystem.node.mapper;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.dto.FolderResponse;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface NodeMapper {

    NodeResponse toNodeResponse(Node node);

    List<NodeResponse> toNodeResponses(List<Node> nodes);

    FolderResponse toFolderResponse(Node folder);
}
