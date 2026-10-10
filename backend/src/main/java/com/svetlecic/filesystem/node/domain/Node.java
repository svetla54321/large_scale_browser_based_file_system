package com.svetlecic.filesystem.node.domain;

import static java.util.Objects.isNull;

import com.svetlecic.filesystem.common.persistence.BaseEntity;
import com.svetlecic.filesystem.node.domain.converter.NodeTypeConverter;
import com.svetlecic.filesystem.node.exception.MaxDepthExceededException;
import com.svetlecic.filesystem.node.helper.AncestorPathParser;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Node extends BaseEntity {

    public static final int MAX_DEPTH = 32;

    private @Nullable Long parentId;

    @Convert(converter = NodeTypeConverter.class)
    private NodeType type;

    private String name;

    private String nameKey;

    private String ancestorPath;

    private Node(@Nullable Long parentId, NodeType type, String name, String ancestorPath) {
        this.parentId = parentId;
        this.type = type;
        this.name = name;
        this.nameKey = NodeNameNormalizer.key(name);
        this.ancestorPath = ancestorPath;
    }

    public Node newChild(NodeType childType, String childName) {
        if (NodeType.FOLDER.equals(childType) && getDepth() >= MAX_DEPTH) {
            throw new MaxDepthExceededException(MAX_DEPTH);
        }
        return new Node(getId(), childType, childName, ancestorPath + getId() + "/");
    }

    public boolean isFolder() {
        return NodeType.FOLDER.equals(type);
    }

    public boolean isRoot() {
        return isNull(parentId);
    }

    public List<Long> getAncestorIds() {
        return AncestorPathParser.parseIds(ancestorPath);
    }

    public int getDepth() {
        return (int) ancestorPath.chars().filter(c -> c == '/').count() - 1;
    }
}
