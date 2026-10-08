package com.svetlecic.filesystem.common.validator;

import static org.assertj.core.api.Assertions.assertThat;

import com.svetlecic.filesystem.common.exception.FieldViolation;
import com.svetlecic.filesystem.common.validator.rule.AbstractRule;
import com.svetlecic.filesystem.common.validator.rule.RuleCode;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RuleExecutorTest {

    private final AtomicInteger evaluatedRules = new AtomicInteger();

    @Test
    void shouldStopAtFirstBrokenRuleWithFirstCatchStrategy() {
        var violations = RuleExecutor.builder()
            .rule(rule(RuleCode.NAME_REQUIRED, true))
            .rule(rule(RuleCode.NAME_TOO_LONG, false))
            .rule(rule(RuleCode.NAME_RESERVED, false))
            .build()
            .validate();

        assertThat(violations).extracting(FieldViolation::code).containsExactly("NAME_TOO_LONG");
        assertThat(evaluatedRules).hasValue(2);
    }

    @Test
    void shouldCollectEveryBrokenRuleWithAllCatchStrategy() {
        var violations = RuleExecutor.builder()
            .rule(rule(RuleCode.NAME_REQUIRED, false))
            .rule(rule(RuleCode.NAME_TOO_LONG, true))
            .rule(rule(RuleCode.NAME_RESERVED, false))
            .strategy(ErrorCatchStrategy.ALL_CATCH)
            .build()
            .validate();

        assertThat(violations).extracting(FieldViolation::code).containsExactly("NAME_REQUIRED", "NAME_RESERVED");
        assertThat(evaluatedRules).hasValue(3);
    }

    @Test
    void shouldReturnNoViolationsWhenEveryRuleHolds() {
        var violations = RuleExecutor.builder()
            .rule(rule(RuleCode.NAME_REQUIRED, true))
            .strategy(ErrorCatchStrategy.ALL_CATCH)
            .build()
            .validate();

        assertThat(violations).isEmpty();
    }

    private AbstractRule rule(RuleCode code, boolean valid) {
        return new AbstractRule("name") {

            @Override
            public RuleCode getRuleCode() {
                return code;
            }

            @Override
            public String getMessage() {
                return "broken";
            }

            @Override
            protected boolean isValid() {
                evaluatedRules.incrementAndGet();
                return valid;
            }
        };
    }
}
