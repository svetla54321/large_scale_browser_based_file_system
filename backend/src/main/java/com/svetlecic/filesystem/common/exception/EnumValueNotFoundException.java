package com.svetlecic.filesystem.common.exception;

import org.springframework.http.HttpStatus;

public class EnumValueNotFoundException extends ApplicationException {

    public <T extends Enum<T>> EnumValueNotFoundException(Class<T> enumType, String source, Object value) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "ENUM_VALUE_NOT_FOUND",
            "No %s with %s '%s'".formatted(enumType.getSimpleName(), source, value));
    }
}
