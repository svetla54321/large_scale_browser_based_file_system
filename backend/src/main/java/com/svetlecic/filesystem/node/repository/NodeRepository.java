package com.svetlecic.filesystem.node.repository;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByParentIdIsNull();

    boolean existsByParentIdAndNameKey(long parentId, String nameKey);

    List<Node> findByParentIdAndTypeAndNameGreaterThanOrderByNameAsc(
        long parentId, NodeType type, String name, Limit limit);
}
