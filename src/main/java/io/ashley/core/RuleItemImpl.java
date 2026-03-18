package io.ashley.core;

import io.ashley.rule.RuleApplyingException;
import io.ashley.rule.RuleEngine;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class RuleItemImpl implements RuleItem, RuleExecutable {

    private boolean hasPassed = false;
    private Operator operator = null;
    private boolean hasApplied = false;

    @Setter
    private boolean isPreviewMode = false;

    private boolean isBoolCondition = false;

    @Getter
    private String name;
    private RuleEngine ruleEngine;

    @Setter
    @Getter
    private Map<String, Object> additionalParams = new HashMap<>();

    private RuleResult ruleResult = new RuleResult();

    public RuleItemImpl(Operator operator, String name, RuleEngine engine, Map<String, Object> additionalParams) {
        this.operator = operator;
        this.name = name;
        this.ruleEngine = engine;
        this.additionalParams = additionalParams;
    }

    public RuleItemImpl(Operator operator, String name, RuleEngine engine) {
        this.operator = operator;
        this.name = name;
        this.ruleEngine = engine;
    }

    public RuleItem apply() throws RuleApplyingException {
        long nestedDepNum = ruleEngine.nestedDepInc();
        if (nestedDepNum > RuleEngine.MAX_NESTED_DEEP_NUM) {
            throw new RuleApplyingException("Too many rules nested in the engine! Maximum is " + RuleEngine.MAX_NESTED_DEEP_NUM);
        }

        if (!this.hasApplied) {
            this.hasPassed = this.operator.assertOp();

            if (this.isBoolCondition) {
                this.ruleResult.status = this.hasPassed ? RuleResult.CONDITION_YES : RuleResult.CONDITION_NO;
            } else {
                if (!this.hasPassed) {
                    if (this.isPreviewMode) {
                        this.ruleResult.status = RuleResult.STATUS_WARNING;
                        this.hasPassed = true;
                    } else {
                        this.ruleResult.status = RuleResult.STATUS_BLOCKED;
                    }
                } else {
                    this.ruleResult.status = RuleResult.STATUS_PASSED;
                }
            }
        } else {
            this.operator.assertOp();
        }

        this.hasApplied = true;
        return this;
    }

    @Override
    public boolean execute() throws RuleApplyingException {
        this.apply();
        return this.hasPassed;
    }

    @Override
    public RuleItem loadOperator(Operator operator) {
        this.operator = operator;
        return this;
    }

    @Override
    public Operator getOperator() {
        return this.operator;
    }

    @Override
    public boolean hasPassed() {
        return this.hasPassed;
    }

    @Override
    public boolean hasApplied() {
        return this.hasApplied;
    }

    @Override
    public boolean isNotBoolCondition() {
        return !this.isBoolCondition;
    }

    @Override
    public RuleItem setIsNotBoolCondition(boolean isBoolCondition) {
        this.isBoolCondition = isBoolCondition;
        return this;
    }

    @Override
    public RuleItem setPreviewMode(boolean previewMode) {
        this.isPreviewMode = previewMode;
        return this;
    }

    @Override
    public Map<String, Object> getAdditionalParams() {
        return this.additionalParams;
    }

    @Override
    public RuleResult getRuleResult() {
        this.ruleResult.type = RuleResult.TYPE_RULE;
        this.ruleResult.ruleName = this.name;
        try {
            this.ruleResult.extraInfo.put("leftValue", this.operator.leftValue.getString());
            this.ruleResult.extraInfo.put("rightValue", this.operator.rightValue.getString());
            this.ruleResult.extraInfo.put("compare", this.operator.compare.name());
            this.ruleResult.extraInfo.put("isBoolCondition", this.isBoolCondition ? "Yes" : "No");
            for (String k : this.additionalParams.keySet()) {
                this.ruleResult.extraInfo.put(k, this.additionalParams.get(k).toString());
            }
        } catch (Exception e) {
            // ignore value loading errors during result collection
        }
        return this.ruleResult;
    }
}
