package com.svetlecic.filesystem.node.exception;

import com.svetlecic.filesystem.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class NameAlreadyExistsException extends ApplicationException {

    public NameAlreadyExistsException(long parentId) {
        super(HttpStatus.CONFLICT, "NAME_ALREADY_EXISTS",
            "A file or folder with this name already exists in folder %d".formatted(parentId));
    }
}
