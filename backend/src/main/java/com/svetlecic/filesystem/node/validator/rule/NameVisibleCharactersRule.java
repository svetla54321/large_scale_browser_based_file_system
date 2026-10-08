package com.svetlecic.filesystem.node.validator.rule;

import com.svetlecic.filesystem.common.validator.rule.AbstractRule;
import com.svetlecic.filesystem.common.validator.rule.RuleCode;
import java.util.regex.Pattern;

public class NameVisibleCharactersRule extends AbstractRule {

    private static final Pattern INVISIBLE_OR_INVALID = Pattern.compile(
        "[[\\p{Cc}\\p{Cs}\\p{Cn}\\p{Zl}\\p{Zp}\\p{Cf}]&&[^\\x{200C}\\x{200D}\\x{E0020}-\\x{E007F}]]");

    private final String name;

    public NameVisibleCharactersRule(String field, String name) {
        super(field);
        this.name = name;
    }

    @Override
    public RuleCode getRuleCode() {
        return RuleCode.NAME_INVISIBLE_CHARACTER;
    }

    @Override
    public String getMessage() {
        return "must not contain invisible, control or invalid characters";
    }

    @Override
    protected boolean isValid() {
        return !INVISIBLE_OR_INVALID.matcher(name).find();
    }
}
