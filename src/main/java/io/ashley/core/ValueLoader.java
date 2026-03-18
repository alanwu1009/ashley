package io.ashley.core;

import io.ashley.rule.HandleValueException;
import io.ashley.rule.RuleEngine;

public interface ValueLoader {
    void setValue(Object v, RuleEngine engine);
    Object loadValue() throws HandleValueException;
}
