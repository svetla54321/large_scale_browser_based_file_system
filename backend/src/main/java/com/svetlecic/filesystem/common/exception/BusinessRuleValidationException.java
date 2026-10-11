package com.svetlecic.filesystem.common.exception;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessRuleValidationException extends ApplicationException {

    public static final String CODE = "VALIDATION_FAILED";

    private final List<FieldViolation> violations;

    public BusinessRuleValidationException(List<FieldViolation> violations) {
        super(HttpStatus.BAD_REQUEST, CODE, FieldViolation.describe(violations));
        this.violations = List.copyOf(violations);
    }
}
