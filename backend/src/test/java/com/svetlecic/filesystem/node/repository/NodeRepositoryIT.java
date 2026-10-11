package com.svetlecic.filesystem.node.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.IntegrationTest;
import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class NodeRepositoryIT {

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    TransactionTemplate transactionTemplate;

    Node root;

    @BeforeEach
    void deleteEverythingBelowRoot() {
        root = nodeRepository.findByParentIdIsNull().orElseThrow();
        jdbcClient.sql("delete from node where parent_id = :root").param("root", root.getId()).update();
    }

    @Test
    void shouldReportSameNameOfAnyTypeAsUniqueConstraintViolation() {
        nodeRepository.saveAndFlush(root.newChild(NodeType.FOLDER, "Docs"));

        assertThatThrownBy(() -> nodeRepository.saveAndFlush(root.newChild(NodeType.FILE, "DOCS")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .satisfies(exception -> assertThat(constraintKind(exception)).isEqualTo(ConstraintKind.UNIQUE));
    }

    @Test
    void shouldGiveUpOnLockedParentAsPessimisticLockingFailure() {
        var parent = nodeRepository.saveAndFlush(root.newChild(NodeType.FOLDER, "Busy"));
        var locked = new CompletableFuture<Void>();
        var release = new CompletableFuture<Void>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                jdbcClient.sql("select id from node where id = :id for update").param("id", parent.getId())
                    .query(Long.class).single();
                locked.complete(null);
                release.join();
            }));
            locked.join();

            assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                jdbcClient.sql("set local lock_timeout = '200ms'").update();
                nodeRepository.saveAndFlush(parent.newChild(NodeType.FILE, "waiting.txt"));
            })).isInstanceOf(PessimisticLockingFailureException.class);
            release.complete(null);
        }
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
