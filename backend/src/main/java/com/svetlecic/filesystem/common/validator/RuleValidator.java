package com.svetlecic.filesystem.common.validator;

public interface RuleValidator<T> {

    void validate(T value);
}
