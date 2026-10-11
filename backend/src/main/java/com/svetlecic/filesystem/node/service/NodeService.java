package com.svetlecic.filesystem.node.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.svetlecic.filesystem.node.domain.Node;
import com.svetlecic.filesystem.node.domain.NodeType;
import com.svetlecic.filesystem.node.dto.FolderPageResponse;
import com.svetlecic.filesystem.node.dto.FolderResponse;
import com.svetlecic.filesystem.node.dto.NodeResponse;
import com.svetlecic.filesystem.node.exception.NameAlreadyExistsException;
import com.svetlecic.filesystem.node.exception.NodeNotFoundException;
import com.svetlecic.filesystem.node.exception.RootFolderDeletionException;
import com.svetlecic.filesystem.node.exception.RootFolderMissingException;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;
import com.svetlecic.filesystem.node.mapper.NodeMapper;
import com.svetlecic.filesystem.node.repository.NodeRepository;
import com.svetlecic.filesystem.node.validator.NodeNameValidator;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
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

        var root = nodeRepository.findByParentIdIsNull().orElseThrow(RootFolderMissingException::new);
        return nodeMapper.toFolderResponse(root, List.of());
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolder(long id) {

        var folder = findFolder(id);
        return nodeMapper.toFolderResponse(folder, findAncestors(folder));
    }

    @Transactional(readOnly = true)
    public NodeResponse getFile(long id) {

        return nodeMapper.toNodeResponse(findFile(id));
    }

    @Transactional(readOnly = true)
    public FolderPageResponse getChildren(long folderId, @Nullable String cursor, int size) {

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
        return nodeMapper.toFolderPageResponse(items, nextCursor, nodeRepository.countByParentId(folderId));
    }

    @Transactional
    public void deleteFolder(long id) {

        if (findFolder(id).isRoot()) {
            throw new RootFolderDeletionException();
        }
        if (nodeRepository.deleteNode(id) == 0) {
            throw NodeNotFoundException.folder(id);
        }
        log.info("Deleted folder {} with its subtree", id);
    }

    @Transactional
    public void deleteFile(long id) {

        findFile(id);
        if (nodeRepository.deleteNode(id) == 0) {
            throw NodeNotFoundException.file(id);
        }
        log.info("Deleted file {}", id);
    }

    @Transactional
    public NodeResponse create(long parentId, @NonNull NodeType type, @NonNull String rawName) {

        var name = NodeNameNormalizer.normalize(rawName);
        nodeNameValidator.validate(name);
        var parent = findFolder(parentId);
        if (nodeRepository.existsByParentIdAndNameKey(parentId, NodeNameNormalizer.key(name))) {
            throw new NameAlreadyExistsException(parentId);
        }

        var node = nodeRepository.save(parent.newChild(type, name));
        log.info("Created {} {} in {}", type.getCode(), node.getId(), parentId);
        return nodeMapper.toNodeResponse(node);
    }

    @Transactional
    public NodeResponse rename(long id, @NonNull NodeType type, @NonNull String rawName) {

        var name = NodeNameNormalizer.normalize(rawName);
        nodeNameValidator.validate(name);
        var node = NodeType.FOLDER.equals(type) ? findFolder(id) : findFile(id);
        var key = NodeNameNormalizer.key(name);
        if (!key.equals(node.getNameKey()) && nonNull(node.getParentId())
            && nodeRepository.existsByParentIdAndNameKey(node.getParentId(), key)) {
            throw new NameAlreadyExistsException(node.getParentId());
        }

        node.rename(name);
        log.info("Renamed {} {}", type.getCode(), id);
        return nodeMapper.toNodeResponse(node);
    }

    @Transactional(readOnly = true)
    public Node findFolder(long id) {

        return nodeRepository.findById(id)
            .filter(Node::isFolder)
            .orElseThrow(() -> NodeNotFoundException.folder(id));
    }

    private Node findFile(long id) {

        return nodeRepository.findById(id)
            .filter(node -> !node.isFolder())
            .orElseThrow(() -> NodeNotFoundException.file(id));
    }

    private List<Node> findAncestors(@NonNull Node folder) {

        var ids = folder.getAncestorIds();
        var byId = nodeRepository.findAllById(ids).stream().collect(toMap(Node::getId, identity()));
        if (byId.size() != ids.size()) {
            throw NodeNotFoundException.folder(folder.getId());
        }
        return ids.stream().map(byId::get).toList();
    }

    private List<Node> findChildren(long folderId, NodeType type, String nameAfter, int limit) {

        return nodeRepository.findByParentIdAndTypeAndNameGreaterThanOrderByNameAsc(folderId, type,
            nameAfter,
            Limit.of(limit));
    }
}
