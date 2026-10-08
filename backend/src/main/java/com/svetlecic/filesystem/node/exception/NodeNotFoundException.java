package com.svetlecic.filesystem.node.exception;

import com.svetlecic.filesystem.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class NodeNotFoundException extends ApplicationException {

    private NodeNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND", message);
    }

    public static NodeNotFoundException folder(long id) {
        return new NodeNotFoundException("Folder %d not found".formatted(id));
    }
}
