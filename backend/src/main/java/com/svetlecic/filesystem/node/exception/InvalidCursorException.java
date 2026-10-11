package com.svetlecic.filesystem.node.exception;

import com.svetlecic.filesystem.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidCursorException extends ApplicationException {

    private static final String CODE = "INVALID_CURSOR";
    private static final String MESSAGE = "Invalid cursor";

    public InvalidCursorException() {
        super(HttpStatus.BAD_REQUEST, CODE, MESSAGE);
    }

    public InvalidCursorException(Throwable cause) {
        super(HttpStatus.BAD_REQUEST, CODE, MESSAGE, cause);
    }
}
