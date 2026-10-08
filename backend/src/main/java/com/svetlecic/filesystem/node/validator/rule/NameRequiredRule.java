package com.svetlecic.filesystem.node.validator.rule;

import com.svetlecic.filesystem.common.validator.rule.AbstractRule;
import com.svetlecic.filesystem.common.validator.rule.RuleCode;

public class NameRequiredRule extends AbstractRule {

    private final String name;

    public NameRequiredRule(String field, String name) {
        super(field);
        this.name = name;
    }

    @Override
    public RuleCode getRuleCode() {
        return RuleCode.NAME_REQUIRED;
    }

    @Override
    public String getMessage() {
        return "must not be empty";
    }

    @Override
    protected boolean isValid() {

        return !name.isEmpty();
    }
}
