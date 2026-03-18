package io.ashley.core;

import io.ashley.rule.RuleApplyingException;

import java.util.ArrayList;
import java.util.List;

public class AndRuleExpression implements RuleExpression, RuleExecutable {

    private List<RuleItem> ruleItems = new ArrayList<>();
    private List<RuleItem> conditionItems = new ArrayList<>();
    private List<RuleExpression> expressions = new ArrayList<>();
    private RuleResult ruleResult = new RuleResult();
    private boolean hasReachedRule = false;

    @Override
    public String getTypeAsString() {
        return "AND Expression";
    }

    @Override
    public RuleExpression add(RuleItem ruleItem) {
        if (ruleItem.isNotBoolCondition()) {
            ruleItems.add(ruleItem);
        } else {
            conditionItems.add(ruleItem);
        }
        return this;
    }

    @Override
    public RuleExpression add(RuleExpression ruleExpression) {
        expressions.add(ruleExpression);
        return this;
    }

    @Override
    public boolean apply() throws RuleApplyingException {
        Boolean passed = null;
        Boolean conditionPassed = null;

        for (RuleItem item : conditionItems) {
            if (!item.apply().hasPassed()) {
                if (conditionPassed == null)
                    conditionPassed = false;
            }
        }
        if (conditionPassed == null)
            conditionPassed = true;

        if (conditionPassed) {
            for (RuleItem item : ruleItems) {
                this.hasReachedRule = true;
                if (!item.apply().hasPassed()) {
                    if (passed == null)
                        passed = false;
                }
            }

            for (RuleExpression exp : expressions) {
                boolean exHit = exp.apply();
                if (exp.hasReachedRule())
                    this.hasReachedRule = true;
                if (passed == null && !exHit && exp.hasReachedRule())
                    passed = false;
            }
        } else {
            for (RuleItem item : ruleItems)
                item.apply();
            for (RuleExpression exp : expressions)
                exp.apply();
        }

        if (passed == null) {
            passed = true;
        }

        if (!this.hasReachedRule) {
            this.ruleResult.status = RuleResult.IGNORED;
        } else {
            this.ruleResult.status = passed ? RuleResult.STATUS_PASSED : RuleResult.STATUS_BLOCKED;
        }

        return passed;
    }

    @Override
    public boolean execute() throws RuleApplyingException {
        return this.apply();
    }

    @Override
    public boolean hasReachedRule() {
        return this.hasReachedRule;
    }

    @Override
    public List<RuleItem> getRuleItems() {
        return this.ruleItems;
    }

    @Override
    public List<RuleExpression> getRuleExpressions() {
        return this.expressions;
    }

    @Override
    public RuleResult getRuleResult() {
        this.ruleResult.type = RuleResult.TYPE_EXPRESSION_AND;
        this.ruleResult.ruleName = this.getTypeAsString();
        this.ruleResult.subResults.clear();
        for (RuleItem item : this.conditionItems) {
            this.ruleResult.subResults.add(item.getRuleResult());
        }
        for (RuleItem item : this.ruleItems) {
            this.ruleResult.subResults.add(item.getRuleResult());
        }
        for (RuleExpression exp : expressions) {
            this.ruleResult.subResults.add(exp.getRuleResult());
        }
        return this.ruleResult;
    }
}
