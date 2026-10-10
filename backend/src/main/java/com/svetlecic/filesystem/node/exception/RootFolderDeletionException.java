package com.svetlecic.filesystem.node.exception;

import com.svetlecic.filesystem.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RootFolderDeletionException extends ApplicationException {

    public RootFolderDeletionException() {
        super(HttpStatus.CONFLICT, "ROOT_FOLDER_NOT_DELETABLE", "The root folder cannot be deleted");
    }
}
