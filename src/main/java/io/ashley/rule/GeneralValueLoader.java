package io.ashley.rule;

import io.ashley.core.ValueHandler;
import io.ashley.core.ValueLoader;

public class GeneralValueLoader implements ValueLoader {

    Object value;
    RuleEngine engine;

    @Override
    public void setValue(Object value, RuleEngine engine) {
        this.value = value;
        this.engine = engine;
    }

    @Override
    public Object loadValue() throws HandleValueException {
        ValueHandler handler = engine.getValueHandler();
        if (handler != null) {
            return handler.handle(this.value, engine);
        }
        return this.value;
    }
}
