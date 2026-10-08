package com.svetlecic.filesystem.node.exception;

import com.svetlecic.filesystem.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class MaxDepthExceededException extends ApplicationException {

    public MaxDepthExceededException(int maxDepth) {
        super(HttpStatus.BAD_REQUEST, "MAX_DEPTH_EXCEEDED",
            "Folders can be nested at most %d levels deep".formatted(maxDepth));
    }
}
