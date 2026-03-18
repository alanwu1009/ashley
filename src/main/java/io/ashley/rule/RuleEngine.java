package io.ashley.rule;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ashley.core.*;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RuleEngine {

    private Map<String, RuleItem> ruleItems = new HashMap<>();
    private RuleFlow ruleFlow = null;
    private AtomicLong nestedDeepNum = new AtomicLong(0);

    private ValueHandler valueHandler = null;
    private HandleValueException handleValueException;

    public static final long MAX_NESTED_DEEP_NUM = 1000;

    public long nestedDepInc() {
        return this.nestedDeepNum.incrementAndGet();
    }

    @Getter
    private Result result;

    public class Result {
        @Getter @Setter
        private Boolean hasPassed;

        @Getter @Setter
        private String conclusionBrief = "";

        @Getter @Setter
        private RuleResult closestBlockedRuleResult;

        @Getter @Setter
        private String conclusionJson = "";

        @Getter @Setter
        private String conclusionBriefJson = "";

        @Getter @Setter
        private Map<String, Object> extraInfo = new HashMap<>();

        @Override
        public String toString() {
            return "Result{" +
                    "hasPassed=" + hasPassed +
                    ", conclusionBrief='" + conclusionBrief + '\'' +
                    ", conclusionJson='" + conclusionJson + '\'' +
                    '}';
        }
    }

    public interface RuleItemConfigLoader {
        String load(String ruleItemConfigId);
    }

    public RuleEngine setup(String ruleFlowConfig) throws RuleSetupException {
        return this.setup(ruleFlowConfig, null, "");
    }

    public RuleEngine setup(String ruleFlowConfig, String name) throws RuleSetupException {
        return this.setup(ruleFlowConfig, null, name);
    }

    public RuleEngine setup(String ruleFlowConfig, RuleItemConfigLoader ruleItemConfigLoader) throws RuleSetupException {
        return this.setup(ruleFlowConfig, ruleItemConfigLoader, "");
    }

    public RuleEngine setup(String ruleFlowConfig, RuleItemConfigLoader ruleItemConfigLoader, String name) throws RuleSetupException {
        ruleFlow = new RuleFlow();
        ruleFlow.setName(name);
        nestedDeepNum = new AtomicLong(0);
        ruleItems = new HashMap<>();
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(ruleFlowConfig, JsonObject.class);

            if (jsonObject.has("case_default")) {
                JsonArray itemsJSArray = jsonObject.getAsJsonArray("case_default");
                this.buildRuleFlow(itemsJSArray, ruleItemConfigLoader, true);
            }

            if (jsonObject.has("case_other")) {
                JsonArray itemsJSArray = jsonObject.getAsJsonArray("case_other");
                this.buildRuleFlow(itemsJSArray, ruleItemConfigLoader, false);
            }
        } catch (Exception e) {
            throw new RuleSetupException(e.toString());
        }
        return this.setup(ruleFlow);
    }

    private void buildRuleFlow(JsonArray itemJSArray, RuleItemConfigLoader ruleItemConfigLoader, boolean isDefaultCase)
            throws ClassNotFoundException, NotSupportedException {

        Iterator<JsonElement> itemsJS = itemJSArray.iterator();
        while (itemsJS.hasNext()) {
            JsonObject itemObject = (JsonObject) itemsJS.next();
            String ruleType = itemObject.get("type").getAsString();

            if (ruleType.equals("rule")) {
                RuleItem ruleItem = this.parseRuleFromJsonObject(itemObject, ruleItemConfigLoader);
                if (itemObject.has("and_flow")) {
                    JsonArray ruleItemJsonArray = itemObject.getAsJsonArray("and_flow");
                    RuleExpression andRuleExpression = parseItemsFromJsonObject(ruleItemJsonArray, AndRuleExpression.class, ruleItemConfigLoader);
                    if (isDefaultCase) {
                        ruleFlow.when(ruleItem, andRuleExpression);
                    } else {
                        andRuleExpression.add(ruleItem);
                        ruleFlow.otherCase(andRuleExpression);
                    }
                } else {
                    if (isDefaultCase) {
                        ruleFlow.when(ruleItem, ruleItem);
                    } else {
                        ruleFlow.otherCase(ruleItem);
                    }
                }
            } else if (ruleType.equals("and_expression")) {
                RuleExpression andRuleExpression = this.parseAndExpressionFromJsonObject(itemObject, ruleItemConfigLoader);
                if (itemObject.has("and_flow")) {
                    JsonArray ruleItemJsonArray = itemObject.getAsJsonArray("and_flow");
                    RuleExpression secondAndRuleExpression = parseItemsFromJsonObject(ruleItemJsonArray, AndRuleExpression.class, ruleItemConfigLoader);
                    if (isDefaultCase) {
                        ruleFlow.when(andRuleExpression, secondAndRuleExpression);
                    } else {
                        andRuleExpression.add(secondAndRuleExpression);
                        ruleFlow.otherCase(andRuleExpression);
                    }
                } else {
                    if (isDefaultCase) {
                        ruleFlow.when(andRuleExpression, andRuleExpression);
                    } else {
                        ruleFlow.otherCase(andRuleExpression);
                    }
                }
            } else if (ruleType.equals("or_expression")) {
                RuleExpression orRuleExpression = this.parseOrExpressionFromJsonObject(itemObject, ruleItemConfigLoader);
                if (itemObject.has("and_flow")) {
                    JsonArray ruleItemJsonArray = itemObject.getAsJsonArray("and_flow");
                    RuleExpression secondAndRuleExpression = parseItemsFromJsonObject(ruleItemJsonArray, AndRuleExpression.class, ruleItemConfigLoader);
                    if (isDefaultCase) {
                        ruleFlow.when(orRuleExpression, secondAndRuleExpression);
                    } else {
                        secondAndRuleExpression.add(orRuleExpression);
                        ruleFlow.otherCase(secondAndRuleExpression);
                    }
                } else {
                    if (isDefaultCase) {
                        ruleFlow.when(orRuleExpression, orRuleExpression);
                    } else {
                        ruleFlow.otherCase(orRuleExpression);
                    }
                }
            }
        }
    }

    public RuleEngine setup(RuleFlow ruleFlow) {
        this.ruleFlow = ruleFlow;
        return this;
    }

    public boolean run() throws RuleApplyingException {
        if (this.ruleFlow == null) {
            throw new NotInitializedException("RuleEngine has not been initialized!");
        }

        if (this.handleValueException != null)
            throw this.handleValueException;

        boolean hasPassed = this.ruleFlow.execute();

        this.result = new Result();
        this.result.conclusionJson = this.ruleFlow.getRuleResult().toJson();
        this.result.conclusionBrief = this.ruleFlow.getRuleResult().toBriefString();
        this.result.conclusionBriefJson = this.ruleFlow.getRuleResult().toBriefJson().toString();
        this.result.hasPassed = hasPassed;
        this.result.closestBlockedRuleResult = this.ruleFlow.getClosestBlockedRuleResult();

        return hasPassed;
    }

    public Result getResult() {
        return this.result;
    }

    private RuleExpression parseItemsFromJsonObject(JsonArray ruleItemJsonArray,
            Class<? extends RuleExpression> ruleExpressionClass,
            RuleItemConfigLoader ruleItemConfigLoader) throws ClassNotFoundException, NotSupportedException {

        RuleExpression ruleExpression;
        if (ruleExpressionClass == AndRuleExpression.class) {
            ruleExpression = new AndRuleExpression();
        } else {
            ruleExpression = new OrRuleExpression();
        }

        Iterator<JsonElement> iter = ruleItemJsonArray.iterator();
        while (iter.hasNext()) {
            JsonObject ruleItemJson = (JsonObject) iter.next();
            String ruleType = ruleItemJson.get("type").getAsString();

            if (ruleType.equals("rule")) {
                RuleItem ruleItem = this.parseRuleFromJsonObject(ruleItemJson, ruleItemConfigLoader);
                if (ruleItemJson.has("and_flow")) {
                    JsonArray ruleAndFlowJsonArray = ruleItemJson.getAsJsonArray("and_flow");
                    RuleExpression andRuleExpression = parseItemsFromJsonObject(ruleAndFlowJsonArray, AndRuleExpression.class, ruleItemConfigLoader);
                    if (andRuleExpression == null) {
                        andRuleExpression = new AndRuleExpression();
                    }
                    andRuleExpression.add(ruleItem);
                    ruleExpression.add(andRuleExpression);
                } else {
                    ruleExpression.add(ruleItem);
                }
            } else if (ruleType.equals("and_expression")) {
                ruleExpression.add(this.parseAndExpressionFromJsonObject(ruleItemJson, ruleItemConfigLoader));
            } else if (ruleType.equals("or_expression")) {
                ruleExpression.add(this.parseOrExpressionFromJsonObject(ruleItemJson, ruleItemConfigLoader));
            }
        }

        return ruleExpression;
    }

    private RuleItem parseRuleFromJsonObject(JsonObject itemObject, RuleItemConfigLoader ruleItemConfigLoader)
            throws ClassNotFoundException, NotSupportedException {

        String ruleItemConfigId = itemObject.get("rule_value").getAsString();
        String ruleItemName = itemObject.get("rule_name").getAsString();
        String ruleItemConfigJson;

        if (ruleItemConfigLoader != null) {
            ruleItemConfigJson = ruleItemConfigLoader.load(ruleItemConfigId);
        } else {
            ruleItemConfigJson = itemObject.get("operator").getAsString();
        }

        JsonObject ruleItemJsonObject = new Gson().fromJson(ruleItemConfigJson, JsonObject.class);

        ValueLoader leftValueLoader = this.getValueFromValueLoader(
                ruleItemJsonObject.get("left_value_loader").getAsString(),
                ruleItemJsonObject.get("left_value").getAsString());
        ValueLoader rightValueLoader = this.getValueFromValueLoader(
                ruleItemJsonObject.get("right_value_loader").getAsString(),
                ruleItemJsonObject.get("right_value").getAsString());

        boolean isPreviewMode = ruleItemJsonObject.has("preview_mode") && ruleItemJsonObject.get("preview_mode").getAsBoolean();
        boolean isBoolCondition = ruleItemJsonObject.has("is_bool_condition") && ruleItemJsonObject.get("is_bool_condition").getAsBoolean();

        Operator.Comparator compare = this.parseComparator(ruleItemJsonObject.get("comparator").getAsString());
        Operator operator = new Operator(Value.valueOf(leftValueLoader), Value.valueOf(rightValueLoader), compare);

        RuleItem ruleItem;
        if (ruleItemJsonObject.has("additional_params")) {
            JsonObject additionalParamsObject = ruleItemJsonObject.getAsJsonObject("additional_params");
            Map<String, Object> additionalParams = new HashMap<>();
            for (String key : additionalParamsObject.keySet()) {
                additionalParams.put(key, additionalParamsObject.get(key).getAsString());
            }
            ruleItem = new RuleItemImpl(operator, ruleItemName, this, additionalParams);
        } else {
            ruleItem = new RuleItemImpl(operator, ruleItemName, this);
        }

        ruleItem.setPreviewMode(isPreviewMode);
        ruleItem.setIsNotBoolCondition(isBoolCondition);
        this.ruleItems.put(ruleItemConfigId, ruleItem);
        return ruleItem;
    }

    private RuleExpression parseAndExpressionFromJsonObject(JsonObject itemObject, RuleItemConfigLoader ruleItemConfigLoader)
            throws ClassNotFoundException, NotSupportedException {

        JsonArray andRulesJsonArray = itemObject.getAsJsonArray("and");
        RuleExpression andRuleExpression = this.parseItemsFromJsonObject(andRulesJsonArray, AndRuleExpression.class, ruleItemConfigLoader);

        if (itemObject.has("and_flow")) {
            JsonArray ruleItemJsonArray = itemObject.getAsJsonArray("and_flow");
            RuleExpression secondAndRuleExpression = parseItemsFromJsonObject(ruleItemJsonArray, AndRuleExpression.class, ruleItemConfigLoader);
            andRuleExpression.add(secondAndRuleExpression);
        }

        return andRuleExpression;
    }

    private RuleExpression parseOrExpressionFromJsonObject(JsonObject itemObject, RuleItemConfigLoader ruleItemConfigLoader)
            throws ClassNotFoundException, NotSupportedException {

        JsonArray orRulesJsonArray = itemObject.getAsJsonArray("or");
        RuleExpression orRuleExpression = this.parseItemsFromJsonObject(orRulesJsonArray, OrRuleExpression.class, ruleItemConfigLoader);

        if (itemObject.has("and_flow")) {
            JsonArray ruleItemJsonArray = itemObject.getAsJsonArray("and_flow");
            RuleExpression secondAndRuleExpression = parseItemsFromJsonObject(ruleItemJsonArray, AndRuleExpression.class, ruleItemConfigLoader);
            orRuleExpression.add(secondAndRuleExpression);
            return new AndRuleExpression() {{
                add(orRuleExpression);
                add(secondAndRuleExpression);
            }};
        } else {
            return orRuleExpression;
        }
    }

    protected ValueLoader getValueFromValueLoader(String loaderStr, String valueString) throws ClassNotFoundException {
        Class<?> loaderClass = Class.forName(loaderStr);
        Constructor<?> constructor = loaderClass.getConstructors()[0];
        ValueLoader valueLoader;
        try {
            valueLoader = (ValueLoader) constructor.newInstance(new Object[]{});
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ClassNotFoundException("Could not instantiate class: " + loaderStr, e);
        }
        valueLoader.setValue(valueString, this);
        return valueLoader;
    }

    protected Operator.Comparator parseComparator(String comparator) throws NotSupportedException {
        String c = comparator.trim().replace(" ", "");
        switch (c) {
            case ">":               return Operator.Comparator.GREATER_THAN;
            case "<":               return Operator.Comparator.LESS_THAN;
            case "=":               return Operator.Comparator.EQUAL;
            case "!=":              return Operator.Comparator.NOTEQUAL;
            case "exactcontains":   return Operator.Comparator.CONTAINS;
            case "contains":        return Operator.Comparator.CONTAINS_ANY;
            case "notcontains":     return Operator.Comparator.NOTCONTAINS_ANY;
            case "exactnotcontains":return Operator.Comparator.NOTCONTAINS;
            default:
                throw new NotSupportedException("Unsupported comparator: " + comparator);
        }
    }

    public Map<String, RuleItem> getAllRules() {
        return this.ruleItems;
    }

    public ValueHandler getValueHandler() {
        return valueHandler;
    }

    public void setValueHandler(ValueHandler handler) {
        this.valueHandler = handler;
    }
}
