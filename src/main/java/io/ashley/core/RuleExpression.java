package io.ashley.core;

import io.ashley.rule.RuleApplyingException;

import java.util.List;

public interface RuleExpression extends RuleExecutable {
    String getTypeAsString();
    RuleExpression add(RuleItem ruleItem);
    RuleExpression add(RuleExpression ruleExpression);
    boolean apply() throws RuleApplyingException;
    List<RuleItem> getRuleItems();
    List<RuleExpression> getRuleExpressions();
    boolean hasReachedRule();
}
