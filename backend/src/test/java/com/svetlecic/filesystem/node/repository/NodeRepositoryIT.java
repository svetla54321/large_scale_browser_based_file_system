package com.svetlecic.filesystem.node.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.IntegrationTest;
import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import java.util.Objects;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class NodeRepositoryIT {

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    JdbcClient jdbcClient;

    Node root;

    @BeforeEach
    void deleteEverythingBelowRoot() {
        root = nodeRepository.findByParentIdIsNull().orElseThrow();
        jdbcClient.sql("delete from node where parent_id = :root").param("root", root.getId()).update();
    }

    @ParameterizedTest
    @EnumSource(NodeType.class)
    void shouldPersistEveryNodeTypeThroughTheConverter(NodeType type) {
        var saved = nodeRepository.saveAndFlush(root.newChild(type, "child-" + type.getCode()));

        assertThat(nodeRepository.findById(saved.getId()).orElseThrow().getType()).isEqualTo(type);
        assertThat(jdbcClient.sql("select type from node where id = :id").param("id", saved.getId())
            .query(String.class).single()).isEqualTo(type.getCode());
    }

    @Test
    void shouldReportSameNameOfAnyTypeAsUniqueConstraintViolation() {
        nodeRepository.saveAndFlush(root.newChild(NodeType.FOLDER, "Docs"));

        assertThatThrownBy(() -> nodeRepository.saveAndFlush(root.newChild(NodeType.FILE, "DOCS")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .satisfies(exception -> assertThat(constraintKind(exception)).isEqualTo(ConstraintKind.UNIQUE));
    }

    @Test
    void shouldReportMissingParentAsForeignKeyViolation() {
        var parent = nodeRepository.saveAndFlush(root.newChild(NodeType.FOLDER, "Gone"));
        jdbcClient.sql("delete from node where id = :id").param("id", parent.getId()).update();

        assertThatThrownBy(() -> nodeRepository.saveAndFlush(parent.newChild(NodeType.FOLDER, "Orphan")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .satisfies(exception -> assertThat(constraintKind(exception)).isEqualTo(ConstraintKind.FOREIGN_KEY));
    }

    private static ConstraintKind constraintKind(Throwable throwable) {

        return Stream.iterate(throwable, Objects::nonNull, Throwable::getCause)
            .filter(ConstraintViolationException.class::isInstance)
            .map(ConstraintViolationException.class::cast)
            .map(ConstraintViolationException::getKind)
            .findFirst()
            .orElse(ConstraintKind.OTHER);
    }
}
