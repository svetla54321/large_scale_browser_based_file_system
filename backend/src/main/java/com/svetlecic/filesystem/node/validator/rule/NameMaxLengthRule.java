package com.svetlecic.filesystem.node.validator.rule;

import com.svetlecic.filesystem.common.validator.rule.AbstractRule;
import com.svetlecic.filesystem.common.validator.rule.RuleCode;
import com.svetlecic.filesystem.node.helper.NodeNameNormalizer;

public class NameMaxLengthRule extends AbstractRule {

    private final String name;

    public NameMaxLengthRule(String field, String name) {
        super(field);
        this.name = name;
    }

    @Override
    public RuleCode getRuleCode() {
        return RuleCode.NAME_TOO_LONG;
    }

    @Override
    public String getMessage() {
        return "must be at most %d characters".formatted(NodeNameNormalizer.MAX_LENGTH);
    }

    @Override
    protected boolean isValid() {
        return name.codePointCount(0, name.length()) <= NodeNameNormalizer.MAX_LENGTH;
    }
}
