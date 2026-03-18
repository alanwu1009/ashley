package io.ashley.core;

import io.ashley.rule.RuleApplyingException;

public interface RuleExecutable {

    /**
     * Returns true if the rule passed, false otherwise.
     */
    boolean execute() throws RuleApplyingException;

    RuleResult getRuleResult();
}
