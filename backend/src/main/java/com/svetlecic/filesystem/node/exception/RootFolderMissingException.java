package com.svetlecic.filesystem.node.exception;

import com.svetlecic.filesystem.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RootFolderMissingException extends ApplicationException {

    public RootFolderMissingException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "ROOT_FOLDER_MISSING", "Root folder is missing");
    }
}
