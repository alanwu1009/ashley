package io.ashley.rule;

import io.ashley.core.RuleExecutable;
import io.ashley.core.RuleExpression;
import io.ashley.core.RuleItem;
import io.ashley.core.RuleResult;

import java.util.LinkedHashMap;

public class RuleFlow implements RuleExecutable {

    private LinkedHashMap<RuleExecutable, RuleExecutable> ruleExecutes = new LinkedHashMap<>();
    private RuleExecutable otherExecute = null;

    private boolean hasApplied = false;
    private boolean hasPassed = false;

    private RuleResult closestBlockedRuleResult;
    private RuleResult ruleResult = new RuleResult();

    private String name = "";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean execute() throws RuleApplyingException {
        if (this.hasApplied)
            return hasPassed;

        boolean isHitCase = false;

        for (RuleExecutable whenItem : ruleExecutes.keySet()) {
            RuleExecutable thenItem = ruleExecutes.get(whenItem);
            boolean passed = whenItem.execute();

            if (passed) {
                this.hasPassed = thenItem.execute();

                if (thenItem instanceof RuleExpression) {
                    if (!((RuleExpression) thenItem).hasReachedRule()) {
                        this.hasPassed = false;
                    }
                } else if (thenItem instanceof RuleItem) {
                    if (((RuleItem) thenItem).isNotBoolCondition()) {
                        this.hasPassed = false;
                    }
                }

                if (!this.passed())
                    this.closestBlockedRuleResult = thenItem.getRuleResult().getClosestBlockedRule();

                isHitCase = true;

                this.ruleResult.caseRuleResult = whenItem.getRuleResult();
                this.ruleResult.subResults.clear();
                this.ruleResult.subResults.add(thenItem.getRuleResult());

                break;
            }
        }

        if (otherExecute != null && !isHitCase) {
            this.hasPassed = otherExecute.execute();

            if (otherExecute instanceof RuleExpression) {
                if (!((RuleExpression) otherExecute).hasReachedRule()) {
                    this.hasPassed = false;
                }
            } else if (otherExecute instanceof RuleItem) {
                if (((RuleItem) otherExecute).isNotBoolCondition()) {
                    this.hasPassed = false;
                }
            }

            this.ruleResult.subResults.clear();
            this.ruleResult.subResults.add(otherExecute.getRuleResult());

            if (!this.passed())
                this.closestBlockedRuleResult = otherExecute.getRuleResult().getClosestBlockedRule();
        }

        this.hasApplied = true;
        return this.hasPassed;
    }

    public RuleFlow when(RuleExecutable when, RuleExecutable then) {
        ruleExecutes.put(when, then);
        return this;
    }

    public RuleFlow otherCase(RuleExecutable then) {
        this.otherExecute = then;
        return this;
    }

    public void clear() {
        ruleExecutes.clear();
        otherExecute = null;
        hasApplied = false;
        hasPassed = false;
        ruleResult = new RuleResult();
        this.closestBlockedRuleResult = null;
        name = "";
    }

    public boolean passed() {
        return this.hasPassed;
    }

    public RuleResult getClosestBlockedRuleResult() {
        return this.closestBlockedRuleResult;
    }

    @Override
    public RuleResult getRuleResult() {
        this.ruleResult.type = RuleResult.TYPE_RULE_FLOW;
        this.ruleResult.status = this.passed() ? RuleResult.STATUS_PASSED : RuleResult.STATUS_BLOCKED;
        this.ruleResult.ruleName = this.name;
        return this.ruleResult;
    }
}
