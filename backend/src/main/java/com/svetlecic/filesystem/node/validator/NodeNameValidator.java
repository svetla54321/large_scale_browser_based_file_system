package com.svetlecic.filesystem.node.validator;

import com.svetlecic.filesystem.common.exception.BusinessRuleValidationException;
import com.svetlecic.filesystem.common.validator.ErrorCatchStrategy;
import com.svetlecic.filesystem.common.validator.RuleExecutor;
import com.svetlecic.filesystem.common.validator.RuleValidator;
import com.svetlecic.filesystem.node.validator.rule.NameAllowedCharactersRule;
import com.svetlecic.filesystem.node.validator.rule.NameMaxLengthRule;
import com.svetlecic.filesystem.node.validator.rule.NameNotReservedRule;
import com.svetlecic.filesystem.node.validator.rule.NameRequiredRule;
import com.svetlecic.filesystem.node.validator.rule.NameVisibleCharactersRule;
import org.springframework.stereotype.Component;

@Component
public class NodeNameValidator implements RuleValidator<String> {

    private static final String FIELD = "name";

    @Override
    public void validate(String name) {
        var violations = RuleExecutor.builder()
            .rule(new NameRequiredRule(FIELD, name))
            .rule(new NameMaxLengthRule(FIELD, name))
            .rule(new NameNotReservedRule(FIELD, name))
            .rule(new NameAllowedCharactersRule(FIELD, name))
            .rule(new NameVisibleCharactersRule(FIELD, name))
            .strategy(ErrorCatchStrategy.ALL_CATCH)
            .build()
            .validate();

        if (!violations.isEmpty()) {
            throw new BusinessRuleValidationException(violations);
        }
    }
}
