package com.svetlecic.filesystem.node.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.svetlecic.filesystem.IntegrationTest;
import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import com.svetlecic.filesystem.node.exception.MaxDepthExceededException;
import com.svetlecic.filesystem.node.exception.NameAlreadyExistsException;
import com.svetlecic.filesystem.node.exception.NodeNotFoundException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
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
                        return nodeService.create(rootId, NodeType.FOLDER, name);
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
    void shouldStopFoldersButNotFilesAtMaxDepth() {
        var parentId = rootId;
        for (var depth = 1; depth <= Node.MAX_DEPTH; depth++) {
            parentId = nodeService.create(parentId, NodeType.FOLDER, "level-" + depth).id();
        }
        var deepestId = parentId;

        assertThatThrownBy(() -> nodeService.create(deepestId, NodeType.FOLDER, "too-deep"))
                .isInstanceOf(MaxDepthExceededException.class);
        assertThat(nodeService.create(deepestId, NodeType.FILE, "notes.txt").parentId()).isEqualTo(deepestId);
    }

    @Test
    void shouldNeverLeaveOrphansWhenCreateRacesWithDelete() {
        IntStream.range(0, 10).forEach(round -> {
            var parentId = nodeService.create(rootId, NodeType.FOLDER, "doomed-" + round).id();
            var start = new CountDownLatch(1);
            List<Future<?>> results;

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                results = List.of(
                        executor.submit(() -> {
                            start.await();
                            return nodeService.create(parentId, NodeType.FILE, "late.txt");
                        }),
                        executor.submit(() -> {
                            start.await();
                            nodeService.deleteFolder(parentId);
                            return parentId;
                        }));
                start.countDown();
            }

            assertThat(results.getLast().state()).isEqualTo(Future.State.SUCCESS);
            results.stream()
                    .filter(result -> result.state() == Future.State.FAILED)
                    .map(Future::exceptionNow)
                    .forEach(failure -> assertThat(failure)
                            .isInstanceOfAny(NodeNotFoundException.class, DataIntegrityViolationException.class));
        });

        assertThat(jdbcClient.sql("""
                select count(*) from node child
                where child.parent_id is not null
                  and not exists (select 1 from node parent where parent.id = child.parent_id)
                """).query(Long.class).single()).isZero();
        assertThat(nodeService.getChildren(rootId, null, 50).items()).isEmpty();
    }
}
