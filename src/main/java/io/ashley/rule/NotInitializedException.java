package io.ashley.rule;

public class NotInitializedException extends RuleApplyingException {

    public NotInitializedException(String message) {
        super(message);
    }
}
