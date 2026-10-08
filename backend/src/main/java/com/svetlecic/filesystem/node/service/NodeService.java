package com.svetlecic.filesystem.node.service;

import static java.util.Objects.isNull;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.dto.FolderPage;
import com.svetlecic.filesystem.node.dto.FolderResponse;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import com.svetlecic.filesystem.node.exception.NameAlreadyExistsException;
import com.svetlecic.filesystem.node.exception.NodeNotFoundException;
import com.svetlecic.filesystem.node.exception.RootFolderMissingException;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import com.svetlecic.filesystem.node.mapper.NodeMapper;
import com.svetlecic.filesystem.node.repository.NodeRepository;
import com.svetlecic.filesystem.node.validator.NodeNameValidator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeRepository nodeRepository;
    private final NodeMapper nodeMapper;
    private final NodeNameValidator nodeNameValidator;

    @Transactional(readOnly = true)
    public FolderResponse getRoot() {

        return nodeMapper.toFolderResponse(
            nodeRepository.findByParentIdIsNull().orElseThrow(RootFolderMissingException::new)
        );
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolder(long id) {

        return nodeMapper.toFolderResponse(findFolder(id));
    }

    @Transactional(readOnly = true)
    public FolderPage getChildren(long folderId, @Nullable String cursor, int size) {

        findFolder(folderId);
        var after = isNull(cursor) || cursor.isBlank() ? PageCursor.FIRST
            : PageCursor.decode(cursor);

        var folders = after.type() == NodeType.FOLDER
            ? findChildren(folderId, NodeType.FOLDER, after.nameAfterFor(NodeType.FOLDER), size + 1)
            : List.<Node>of();
        var files = folders.size() > size
            ? List.<Node>of()
            : findChildren(folderId, NodeType.FILE, after.nameAfterFor(NodeType.FILE),
                size + 1 - folders.size());

        var page = Stream.concat(folders.stream(), files.stream()).toList();
        var items = page.stream().limit(size).toList();
        var nextCursor = page.size() > size ? PageCursor.after(items.getLast()).encode() : null;
        return new FolderPage(nodeMapper.toNodeResponses(items), nextCursor);
    }

    @Transactional
    public NodeResponse createFolder(long parentId, String rawName) {

        var name = NodeNameNormalizer.normalize(rawName);
        nodeNameValidator.validate(name);
        var parent = findFolder(parentId);
        if (nodeRepository.existsByParentIdAndNameKey(parentId, NodeNameNormalizer.key(name))) {
            throw new NameAlreadyExistsException(parentId);
        }

        var folder = nodeRepository.save(parent.newChild(NodeType.FOLDER, name));
        log.info("Created folder {} in {}", folder.getId(), parentId);
        return nodeMapper.toNodeResponse(folder);
    }

    private Node findFolder(long id) {

        return nodeRepository.findById(id)
            .filter(Node::isFolder)
            .orElseThrow(() -> NodeNotFoundException.folder(id));
    }

    private List<Node> findChildren(long folderId, NodeType type, String nameAfter, int limit) {

        return nodeRepository.findByParentIdAndTypeAndNameGreaterThanOrderByNameAsc(folderId, type,
            nameAfter,
            Limit.of(limit));
    }
}
