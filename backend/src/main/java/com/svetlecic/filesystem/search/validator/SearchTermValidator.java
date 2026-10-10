package com.svetlecic.filesystem.search.validator;

import com.svetlecic.filesystem.common.exception.BusinessRuleValidationException;
import com.svetlecic.filesystem.common.validator.ErrorCatchStrategy;
import com.svetlecic.filesystem.common.validator.RuleExecutor;
import com.svetlecic.filesystem.common.validator.RuleValidator;
import com.svetlecic.filesystem.node.validator.rule.NameMaxLengthRule;
import com.svetlecic.filesystem.node.validator.rule.NameRequiredRule;
import com.svetlecic.filesystem.node.validator.rule.NameVisibleCharactersRule;
import org.springframework.stereotype.Component;

@Component
public class SearchTermValidator implements RuleValidator<SearchTerm> {

    @Override
    public void validate(SearchTerm term) {
        var violations = RuleExecutor.builder()
            .rule(new NameRequiredRule(term.field(), term.value()))
            .rule(new NameMaxLengthRule(term.field(), term.value()))
            .rule(new NameVisibleCharactersRule(term.field(), term.value()))
            .strategy(ErrorCatchStrategy.FIRST_CATCH)
            .build()
            .validate();

        if (!violations.isEmpty()) {
            throw new BusinessRuleValidationException(violations);
        }
    }
}
