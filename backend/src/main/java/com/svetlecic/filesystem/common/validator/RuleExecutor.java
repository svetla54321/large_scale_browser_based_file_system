package com.svetlecic.filesystem.common.validator;

import com.svetlecic.filesystem.common.exception.FieldViolation;
import com.svetlecic.filesystem.common.validator.rule.AbstractRule;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Singular;

@Builder
public class RuleExecutor {

    @Singular
    private final List<AbstractRule> rules;

    @Builder.Default
    private final ErrorCatchStrategy strategy = ErrorCatchStrategy.FIRST_CATCH;

    public List<FieldViolation> validate() {
        var violations = rules.stream()
            .map(AbstractRule::validate)
            .flatMap(Optional::stream);
        return switch (strategy) {
            case FIRST_CATCH -> violations.limit(1).toList();
            case ALL_CATCH -> violations.toList();
        };
    }
}
