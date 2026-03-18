package io.ashley.core;

import io.ashley.rule.RuleApplyingException;

import java.util.Map;

public interface RuleItem extends RuleExecutable {
    RuleItem setPreviewMode(boolean previewMode);
    RuleItem apply() throws RuleApplyingException;

    RuleItem loadOperator(Operator operator);
    RuleItem setIsNotBoolCondition(boolean b);
    Operator getOperator();
    boolean hasPassed();
    boolean hasApplied();
    boolean isNotBoolCondition();

    Map<String, Object> getAdditionalParams();
}
