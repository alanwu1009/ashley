package io.ashley.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class RuleResult {
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_WARNING = "WARNING";

    public static final String CONDITION_YES = "YES";
    public static final String CONDITION_NO = "NO";
    public static final String IGNORED = "IGNORED";

    public static final String TYPE_EXPRESSION_AND = "and";
    public static final String TYPE_EXPRESSION_OR = "or";
    public static final String TYPE_RULE = "rule";
    public static final String TYPE_RULE_FLOW = "flow";

    public String status;
    public String ruleName;
    public String type;

    public Set<RuleResult> subResults = new LinkedHashSet<>();
    public RuleResult caseRuleResult;

    public Map<String, String> extraInfo = new HashMap<>();

    public String toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("name", this.ruleName);
        result.addProperty("type", this.type);
        result.addProperty("status", this.status);

        if (this.type.equals(TYPE_RULE_FLOW)) {
            if (this.caseRuleResult != null)
                result.addProperty("case", this.caseRuleResult.ruleName);
            else
                result.addProperty("case", "Default case");
        }

        if (extraInfo.size() > 0) {
            for (String k : extraInfo.keySet()) {
                result.addProperty(k, extraInfo.get(k));
            }
        }

        if (subResults != null && subResults.size() > 0) {
            JsonArray jsonArray = new JsonArray();
            for (RuleResult r : subResults) {
                jsonArray.add(new Gson().fromJson(r.toJson(), JsonObject.class));
            }
            if (this.type.equals(TYPE_EXPRESSION_AND))
                result.add("and_expression", jsonArray);
            else if (this.type.equals(TYPE_EXPRESSION_OR))
                result.add("or_expression", jsonArray);
            else
                result.add("rule_content", jsonArray);
        }

        return result.toString();
    }

    public String toBriefString() {
        StringBuilder result = new StringBuilder();

        if (this.type.equals(TYPE_RULE_FLOW)) {
            result.append("\n\n<strong>").append(this.ruleName.trim()).append("</strong>\n");
            if (this.caseRuleResult != null)
                result.append("<strong>CASE:</strong>").append(this.caseRuleResult.ruleName.split(":")[0]).append("\n");
            else
                result.append("<strong>CASE:</strong>").append("Default case").append("\n");
        } else if (this.type.equals(TYPE_RULE)) {
            if (this.status != null && !this.status.equals(CONDITION_YES) && !this.status.equals(CONDITION_NO) && !this.status.equals(IGNORED))
                result.append(this.ruleName.split(":")[0]).append(" [").append(this.status).append("]").append("\n");
        }

        if (subResults != null && subResults.size() > 0) {
            if (!this.type.equals(TYPE_EXPRESSION_AND) && !this.type.equals(TYPE_EXPRESSION_OR)
                    && !this.status.equals(CONDITION_YES) && !this.status.equals(CONDITION_NO) && !this.status.equals(IGNORED)) {
                result.append("<strong>RULE:</strong>\n");
            }
            for (RuleResult r : subResults) {
                if (r.status != null && !r.status.equals(CONDITION_YES) && !r.status.equals(CONDITION_NO) && !r.status.equals(IGNORED))
                    result.append(r.toBriefString());
            }
        }

        return result.toString();
    }

    public JsonElement toBriefJson() {
        JsonObject result = new JsonObject();
        JsonArray ruleJsonArray = new JsonArray();

        if (this.type.equals(TYPE_RULE_FLOW)) {
            result.addProperty("rule_flow_name", this.ruleName.trim());
            if (this.caseRuleResult != null) {
                result.addProperty("case", this.caseRuleResult.ruleName.split(":")[0]);
            } else {
                result.addProperty("case", "Default case");
            }
            result.add("rules", new JsonArray());
        } else if (this.type.equals(TYPE_RULE)) {
            if (this.status != null && !this.status.equals(CONDITION_YES) && !this.status.equals(CONDITION_NO) && !this.status.equals(IGNORED)) {
                result.addProperty("rule_name", this.ruleName);
                result.addProperty("status", this.status);
            }
        }

        if (subResults != null && subResults.size() > 0) {
            if (!result.has("rules") && result.size() > 0) {
                ruleJsonArray.add(result);
            }

            for (RuleResult r : subResults) {
                if (r.status != null && !r.status.equals(CONDITION_YES) && !r.status.equals(CONDITION_NO) && !r.status.equals(IGNORED)) {
                    JsonElement subResultsJson = r.toBriefJson();
                    if (subResultsJson instanceof JsonArray) {
                        for (JsonElement o : (JsonArray) subResultsJson) {
                            if (((JsonObject) o).size() > 0) {
                                ruleJsonArray.add(o);
                            }
                        }
                    } else if (subResultsJson instanceof JsonObject) {
                        ruleJsonArray.add(subResultsJson);
                    }
                }
            }
        }

        if (ruleJsonArray.size() > 0 && result.has("rules")) {
            result.add("rules", ruleJsonArray);
            return result;
        } else if (ruleJsonArray.size() > 0) {
            return ruleJsonArray;
        } else {
            return result;
        }
    }

    public RuleResult getClosestBlockedRule() {
        if (this.type.equals(TYPE_RULE) && this.status.equals(STATUS_BLOCKED)) {
            return this;
        }

        if (subResults != null && subResults.size() > 0) {
            for (RuleResult result : subResults) {
                if (!result.status.equals(IGNORED) && !result.status.equals(STATUS_PASSED) && !result.status.equals(STATUS_WARNING)) {
                    RuleResult closestBlockedRule = result.getClosestBlockedRule();
                    if (closestBlockedRule != null) {
                        return closestBlockedRule;
                    }
                }
            }
        }

        return null;
    }
}
