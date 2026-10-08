package com.svetlecic.filesystem.node.validator.rule;

import com.svetlecic.filesystem.common.validator.rule.AbstractRule;
import com.svetlecic.filesystem.common.validator.rule.RuleCode;
import java.util.Set;

public class NameNotReservedRule extends AbstractRule {

    private static final Set<String> RESERVED_NAMES = Set.of(".", "..");

    private final String name;

    public NameNotReservedRule(String field, String name) {
        super(field);
        this.name = name;
    }

    @Override
    public RuleCode getRuleCode() {
        return RuleCode.NAME_RESERVED;
    }

    @Override
    public String getMessage() {
        return "must not be '.' or '..'";
    }

    @Override
    protected boolean isValid() {

        return !RESERVED_NAMES.contains(name);
    }
}
