package com.svetlecic.filesystem.common.validator.rule;

import com.svetlecic.filesystem.common.exception.FieldViolation;
import java.util.Optional;
import lombok.Getter;

@Getter
public abstract class AbstractRule {

    private final String field;

    protected AbstractRule(String field) {
        this.field = field;
    }

    public abstract RuleCode getRuleCode();

    public abstract String getMessage();

    protected abstract boolean isValid();

    public Optional<FieldViolation> validate() {
        return isValid()
            ? Optional.empty()
            : Optional.of(new FieldViolation(field, getRuleCode().name(), getMessage()));
    }
}
