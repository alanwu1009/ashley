package io.ashley.rule;

import io.ashley.core.ValueHandler;
import io.ashley.core.ValueLoader;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.jexl2.Script;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A ValueLoader that supports mathematical expressions with labeled variables.
 *
 * <p>Usage: reference variables in the expression with the {@code label:} prefix,
 * e.g. {@code label:amount * 0.1 + label:fee}. The labels are resolved via the
 * configured {@link ValueHandler} and the expression is evaluated using JEXL.
 */
public class ExpressionValueLoader implements ValueLoader {

    Object value;
    RuleEngine engine;

    @Override
    public void setValue(Object value, RuleEngine engine) {
        this.value = value;
        this.engine = engine;
    }

    @Override
    public Object loadValue() throws HandleValueException {
        if (this.value instanceof String) {
            ValueHandler handler = engine.getValueHandler();
            String rgex = "label:(.*?)([\\-|\\+|\\*|\\%|\\)|\\s]|$)";
            List<String> labels = ExpressionValueLoader.parseLabels(this.value.toString(), rgex);

            Map<String, Object> kv = new HashMap<>();
            try {
                for (String label : labels) {
                    Object fv = "";
                    Object v1 = handler.handle(label, engine);
                    if (v1 == null) {
                        fv = "";
                    } else if (v1 instanceof BigDecimal) {
                        fv = ((BigDecimal) v1).doubleValue();
                    } else if (v1 instanceof List) {
                        fv = String.join(",", (List<String>) v1);
                    } else {
                        fv = v1.toString();
                    }
                    kv.put(label, fv);
                }
            } catch (HandleValueException e) {
                throw e;
            } catch (Exception e) {
                throw new HandleValueException("Error occurred while handling expression value: " + this.value + " - " + e.getMessage());
            }

            Object rsV = ExpressionValueLoader.evaluate(this.value.toString().replaceAll("label:", ""), kv);
            return rsV.toString();
        } else {
            return this.value;
        }
    }

    public static List<String> parseLabels(String expression, String regex) {
        List<String> list = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(expression);
        while (m.find()) {
            list.add(m.group(1));
        }
        return list;
    }

    public static Object evaluate(String jexlExp, Map<String, Object> map) {
        JexlEngine jexl = new JexlEngine();
        Script expression = jexl.createScript(jexlExp);
        JexlContext jc = new MapContext();
        for (String key : map.keySet()) {
            jc.set(key, map.get(key));
        }
        Object result = expression.execute(jc);
        return result == null ? "" : result;
    }
}
