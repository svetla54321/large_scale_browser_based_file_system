package com.svetlecic.filesystem.node.repository;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NodeRepository extends JpaRepository<Node, Long> {

    Optional<Node> findByParentIdIsNull();

    boolean existsByParentIdAndNameKey(long parentId, String nameKey);

    long countByParentId(long parentId);

    List<Node> findByParentIdAndTypeAndNameGreaterThanOrderByNameAsc(
        long parentId, NodeType type, String name, Limit limit);

    @Modifying
    @Query("delete from Node n where n.id = :id")
    int deleteNode(long id);
}
