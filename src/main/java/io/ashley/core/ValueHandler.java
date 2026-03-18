package io.ashley.core;

import io.ashley.rule.HandleValueException;
import io.ashley.rule.RuleEngine;

public interface ValueHandler {
    Object handle(Object v, RuleEngine engine) throws HandleValueException;
}
