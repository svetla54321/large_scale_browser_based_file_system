package com.svetlecic.filesystem.node.validator.rule;

import com.svetlecic.filesystem.common.validator.rule.AbstractRule;
import com.svetlecic.filesystem.common.validator.rule.RuleCode;
import java.util.regex.Pattern;

public class NameAllowedCharactersRule extends AbstractRule {

    private static final Pattern FORBIDDEN_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|]");

    private final String name;

    public NameAllowedCharactersRule(String field, String name) {
        super(field);
        this.name = name;
    }

    @Override
    public RuleCode getRuleCode() {
        return RuleCode.NAME_FORBIDDEN_CHARACTER;
    }

    @Override
    public String getMessage() {
        return "must not contain any of \\ / : * ? \" < > |";
    }

    @Override
    protected boolean isValid() {
        return !FORBIDDEN_CHARACTERS.matcher(name).find();
    }
}
