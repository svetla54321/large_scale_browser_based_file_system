package com.svetlecic.filesystem.node.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.IntegrationTest;
import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import com.svetlecic.filesystem.node.exception.MaxDepthExceededException;
import com.svetlecic.filesystem.node.exception.NameAlreadyExistsException;
import com.svetlecic.filesystem.node.repository.NodeRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class NodeServiceIT {

    @Autowired
    NodeService nodeService;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    JdbcClient jdbcClient;

    long rootId;

    @BeforeEach
    void deleteEverythingBelowRoot() {
        rootId = nodeService.getRoot().id();
        jdbcClient.sql("delete from node where parent_id = :root").param("root", rootId).update();
    }

    @Test
    void shouldLetOnlyOneOfTwoConcurrentCreatesWithTheSameNameSucceed() {
        var start = new CountDownLatch(1);
        List<Future<NodeResponse>> results;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            results = List.of("Docs", "docs").stream()
                    .map(name -> executor.submit(() -> {
                        start.await();
                        return nodeService.createFolder(rootId, name);
                    }))
                    .toList();
            start.countDown();
        }

        var failures = results.stream()
                .filter(result -> result.state() == Future.State.FAILED)
                .map(Future::exceptionNow)
                .toList();
        assertThat(failures).singleElement()
            .isInstanceOfAny(NameAlreadyExistsException.class, DataIntegrityViolationException.class);
        assertThat(nodeService.getChildren(rootId, null, 10).items()).hasSize(1);
    }

    @Test
    void shouldStoreAncestorIdsInPathAndFillAuditFields() {
        var docs = nodeService.createFolder(rootId, "Docs");
        var reports = nodeService.createFolder(docs.id(), "Reports");

        var stored = nodeRepository.findById(reports.id()).orElseThrow();
        assertThat(stored.getAncestorPath()).isEqualTo("/%d/%d/".formatted(rootId, docs.id()));
        assertThat(stored.getDepth()).isEqualTo(2);
        assertThat(stored.getCreatedAt()).isEqualTo(reports.createdAt());
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(stored.getVersion()).isZero();
    }

    @Test
    void shouldRejectFolderNestedDeeperThanMaxDepth() {
        var parentId = rootId;
        for (var depth = 1; depth <= Node.MAX_DEPTH; depth++) {
            parentId = nodeService.createFolder(parentId, "level-" + depth).id();
        }
        var deepestId = parentId;

        assertThatThrownBy(() -> nodeService.createFolder(deepestId, "too-deep"))
                .isInstanceOf(MaxDepthExceededException.class);
    }
}
