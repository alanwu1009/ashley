package io.ashley;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ashley.core.RuleResult;
import io.ashley.core.ValueHandler;
import io.ashley.rule.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ashley RuleEngine Tests")
class RuleEngineTest {

    private static final String GENERAL_LOADER = "io.ashley.rule.GeneralValueLoader";
    private static final String EXPR_LOADER    = "io.ashley.rule.ExpressionValueLoader";

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Create a ValueHandler backed by a simple Map.
     * Keys present in the map are resolved to their values;
     * unknown keys are treated as literal constants and returned as-is.
     */
    private ValueHandler mapHandler(Map<String, Object> data) {
        return (key, engine) -> {
            String k = key.toString();
            return data.containsKey(k) ? data.get(k) : k;
        };
    }

    /** Build an operator JSON string. */
    private String op(String leftLoader, String leftValue,
                      String comparator,
                      String rightLoader, String rightValue) {
        JsonObject o = new JsonObject();
        o.addProperty("left_value_loader",  leftLoader);
        o.addProperty("left_value",         leftValue);
        o.addProperty("right_value_loader", rightLoader);
        o.addProperty("right_value",        rightValue);
        o.addProperty("comparator",         comparator);
        return o.toString();
    }

    /** Shorthand: both sides use GeneralValueLoader. */
    private String op(String leftValue, String comparator, String rightValue) {
        return op(GENERAL_LOADER, leftValue, comparator, GENERAL_LOADER, rightValue);
    }

    /** Build a rule item JSON object. */
    private JsonObject ruleItem(String name, String id, String operatorJson) {
        JsonObject item = new JsonObject();
        item.addProperty("type",      "rule");
        item.addProperty("rule_name", name);
        item.addProperty("rule_value", id);
        item.addProperty("operator",  operatorJson);
        return item;
    }

    /** Build an and_expression item from a list of rule/expression items. */
    private JsonObject andExpr(JsonObject... items) {
        JsonArray arr = new JsonArray();
        for (JsonObject i : items) arr.add(i);
        JsonObject e = new JsonObject();
        e.addProperty("type", "and_expression");
        e.add("and", arr);
        return e;
    }

    /** Build an or_expression item from a list of rule/expression items. */
    private JsonObject orExpr(JsonObject... items) {
        JsonArray arr = new JsonArray();
        for (JsonObject i : items) arr.add(i);
        JsonObject e = new JsonObject();
        e.addProperty("type", "or_expression");
        e.add("or", arr);
        return e;
    }

    /** Wrap items into a flow JSON with case_other containing a single and_expression. */
    private String flowWithAndExpr(JsonObject... ruleItems) {
        JsonArray caseOther = new JsonArray();
        caseOther.add(andExpr(ruleItems));
        JsonObject flow = new JsonObject();
        flow.add("case_other", caseOther);
        return flow.toString();
    }

    /** Wrap items into a flow JSON with case_other containing a single or_expression. */
    private String flowWithOrExpr(JsonObject... ruleItems) {
        JsonArray caseOther = new JsonArray();
        caseOther.add(orExpr(ruleItems));
        JsonObject flow = new JsonObject();
        flow.add("case_other", caseOther);
        return flow.toString();
    }

    /** Convenience: build engine with a map-backed value handler. */
    private RuleEngine engineWith(Map<String, Object> data) {
        RuleEngine e = new RuleEngine();
        e.setValueHandler(mapHandler(data));
        return e;
    }

    // -----------------------------------------------------------------------
    // 1. Basic comparators
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Basic comparators")
    class BasicComparators {

        @Test
        @DisplayName("= : equal values → pass")
        void equal_pass() throws Exception {
            String flow = flowWithAndExpr(ruleItem("status check", "r1", op("status", "=", "active")));
            RuleEngine e = engineWith(Map.of("status", "active"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("= : unequal values → block")
        void equal_block() throws Exception {
            String flow = flowWithAndExpr(ruleItem("status check", "r1", op("status", "=", "active")));
            RuleEngine e = engineWith(Map.of("status", "inactive"));
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("!= : different values → pass")
        void notEqual_pass() throws Exception {
            String flow = flowWithAndExpr(ruleItem("not banned", "r1", op("status", "!=", "banned")));
            RuleEngine e = engineWith(Map.of("status", "active"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("!= : same values → block")
        void notEqual_block() throws Exception {
            String flow = flowWithAndExpr(ruleItem("not banned", "r1", op("status", "!=", "banned")));
            RuleEngine e = engineWith(Map.of("status", "banned"));
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("> : left > right → pass")
        void greaterThan_pass() throws Exception {
            String flow = flowWithAndExpr(ruleItem("age check", "r1", op("age", ">", "18")));
            RuleEngine e = engineWith(Map.of("age", "25"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("> : left <= right → block")
        void greaterThan_block() throws Exception {
            String flow = flowWithAndExpr(ruleItem("age check", "r1", op("age", ">", "18")));
            RuleEngine e = engineWith(Map.of("age", "16"));
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("< : left < right → pass")
        void lessThan_pass() throws Exception {
            String flow = flowWithAndExpr(ruleItem("risk score", "r1", op("riskScore", "<", "100")));
            RuleEngine e = engineWith(Map.of("riskScore", "50"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("< : left >= right → block")
        void lessThan_block() throws Exception {
            String flow = flowWithAndExpr(ruleItem("risk score", "r1", op("riskScore", "<", "100")));
            RuleEngine e = engineWith(Map.of("riskScore", "150"));
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("comparator with surrounding spaces is accepted")
        void comparator_withSpaces() throws Exception {
            JsonObject opJson = new JsonObject();
            opJson.addProperty("left_value_loader",  GENERAL_LOADER);
            opJson.addProperty("left_value",         "score");
            opJson.addProperty("right_value_loader", GENERAL_LOADER);
            opJson.addProperty("right_value",        "60");
            opJson.addProperty("comparator",         " > ");   // intentional spaces
            String flow = flowWithAndExpr(ruleItem("score check", "r1", opJson.toString()));
            RuleEngine e = engineWith(Map.of("score", "80"));
            e.setup(flow);
            assertTrue(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 2. List comparators
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("List comparators")
    class ListComparators {

        @Test
        @DisplayName("contains : left contains any of right → pass")
        void containsAny_pass() throws Exception {
            String flow = flowWithAndExpr(ruleItem("country check", "r1", op("country", "contains", "US,CN,JP")));
            RuleEngine e = engineWith(Map.of("country", "CN"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("contains : left does not contain any of right → block")
        void containsAny_block() throws Exception {
            String flow = flowWithAndExpr(ruleItem("country check", "r1", op("country", "contains", "US,CN,JP")));
            RuleEngine e = engineWith(Map.of("country", "DE"));
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("notcontains : left does not appear in right → pass")
        void notContainsAny_pass() throws Exception {
            // notcontains: true when any item in right is NOT found in left
            // left="US", right="IR,KP" → "IR" not in "US" → true
            String flow = flowWithAndExpr(ruleItem("blacklist check", "r1", op("country", "notcontains", "IR,KP")));
            RuleEngine e = engineWith(Map.of("country", "US"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("exact contains : left contains all of right → pass")
        void exactContains_pass() throws Exception {
            // left list must containAll of right list
            String flow = flowWithAndExpr(ruleItem("tag check", "r1", op("tags", "exact contains", "vip,premium")));
            RuleEngine e = engineWith(Map.of("tags", "vip,premium,gold"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("exact contains : left missing some of right → block")
        void exactContains_block() throws Exception {
            String flow = flowWithAndExpr(ruleItem("tag check", "r1", op("tags", "exact contains", "vip,premium")));
            RuleEngine e = engineWith(Map.of("tags", "vip,gold"));
            e.setup(flow);
            assertFalse(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 3. AND expression
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("AND expression")
    class AndExpressionTests {

        @Test
        @DisplayName("all rules pass → pass")
        void allPass() throws Exception {
            String flow = flowWithAndExpr(
                ruleItem("age check",    "r1", op("age",    ">", "18")),
                ruleItem("status check", "r2", op("status", "=", "active"))
            );
            RuleEngine e = engineWith(Map.of("age", "25", "status", "active"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("one rule fails → block")
        void oneRuleFails() throws Exception {
            String flow = flowWithAndExpr(
                ruleItem("age check",    "r1", op("age",    ">", "18")),
                ruleItem("status check", "r2", op("status", "=", "active"))
            );
            RuleEngine e = engineWith(Map.of("age", "25", "status", "suspended"));
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("all rules fail → block")
        void allFail() throws Exception {
            String flow = flowWithAndExpr(
                ruleItem("age check",    "r1", op("age",    ">", "18")),
                ruleItem("status check", "r2", op("status", "=", "active"))
            );
            RuleEngine e = engineWith(Map.of("age", "15", "status", "suspended"));
            e.setup(flow);
            assertFalse(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 4. OR expression
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("OR expression")
    class OrExpressionTests {

        @Test
        @DisplayName("at least one rule passes → pass")
        void onePass() throws Exception {
            String flow = flowWithOrExpr(
                ruleItem("high score", "r1", op("score", ">", "80")),
                ruleItem("vip user",   "r2", op("vip",   "=", "true"))
            );
            RuleEngine e = engineWith(Map.of("score", "60", "vip", "true"));
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("all rules fail → block")
        void allFail() throws Exception {
            String flow = flowWithOrExpr(
                ruleItem("high score", "r1", op("score", ">", "80")),
                ruleItem("vip user",   "r2", op("vip",   "=", "true"))
            );
            RuleEngine e = engineWith(Map.of("score", "60", "vip", "false"));
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("all rules pass → pass")
        void allPass() throws Exception {
            String flow = flowWithOrExpr(
                ruleItem("high score", "r1", op("score", ">", "80")),
                ruleItem("vip user",   "r2", op("vip",   "=", "true"))
            );
            RuleEngine e = engineWith(Map.of("score", "90", "vip", "true"));
            e.setup(flow);
            assertTrue(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 5. Nested expressions
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Nested expressions")
    class NestedExpressions {

        @Test
        @DisplayName("OR( AND(age>18, status=active), vip=true ) — OR passes via vip")
        void orContainingAnd_passViaOr() throws Exception {
            JsonObject andPart = andExpr(
                ruleItem("age",    "r1", op("age",    ">", "18")),
                ruleItem("status", "r2", op("status", "=", "active"))
            );
            JsonObject vipRule = ruleItem("vip", "r3", op("vip", "=", "true"));

            JsonArray caseOther = new JsonArray();
            caseOther.add(orExpr(andPart, vipRule));
            JsonObject flow = new JsonObject();
            flow.add("case_other", caseOther);

            // age=15 fails, status=active passes → AND fails; vip=true → OR passes
            RuleEngine e = engineWith(Map.of("age", "15", "status", "active", "vip", "true"));
            e.setup(flow.toString());
            assertTrue(e.run());
        }

        @Test
        @DisplayName("OR( AND(age>18, status=active), vip=true ) — both sides fail → block")
        void orContainingAnd_allFail() throws Exception {
            JsonObject andPart = andExpr(
                ruleItem("age",    "r1", op("age",    ">", "18")),
                ruleItem("status", "r2", op("status", "=", "active"))
            );
            JsonObject vipRule = ruleItem("vip", "r3", op("vip", "=", "true"));

            JsonArray caseOther = new JsonArray();
            caseOther.add(orExpr(andPart, vipRule));
            JsonObject flow = new JsonObject();
            flow.add("case_other", caseOther);

            // age=15 fails → AND fails; vip=false → OR fails
            RuleEngine e = engineWith(Map.of("age", "15", "status", "active", "vip", "false"));
            e.setup(flow.toString());
            assertFalse(e.run());
        }

        @Test
        @DisplayName("AND containing OR: AND( OR(score>80, vip=true), status=active ) → pass")
        void andContainingOr_pass() throws Exception {
            JsonObject orPart = orExpr(
                ruleItem("score", "r1", op("score", ">", "80")),
                ruleItem("vip",   "r2", op("vip",   "=", "true"))
            );
            JsonObject statusRule = ruleItem("status", "r3", op("status", "=", "active"));

            JsonArray andArr = new JsonArray();
            andArr.add(orPart);
            andArr.add(statusRule);
            JsonObject andExpression = new JsonObject();
            andExpression.addProperty("type", "and_expression");
            andExpression.add("and", andArr);

            JsonArray caseOther = new JsonArray();
            caseOther.add(andExpression);
            JsonObject flow = new JsonObject();
            flow.add("case_other", caseOther);

            // score=90 → OR passes; status=active → AND passes
            RuleEngine e = engineWith(Map.of("score", "90", "vip", "false", "status", "active"));
            e.setup(flow.toString());
            assertTrue(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 6. case_default + case_other branching
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("case_default + case_other branching")
    class CaseBranching {

        @Test
        @DisplayName("case_default condition not met → case_other rules run and pass")
        void caseDefault_notHit_caseOther_passes() throws Exception {
            // case_default: bool condition — is userType = "premium"?
            // case_other: actual validation rules
            JsonObject condOpJson = new Gson().fromJson(op("userType", "=", "premium"), JsonObject.class);
            condOpJson.addProperty("is_bool_condition", true);

            JsonObject condRule = new JsonObject();
            condRule.addProperty("type",       "rule");
            condRule.addProperty("rule_name",  "Is Premium");
            condRule.addProperty("rule_value", "is_premium");
            condRule.addProperty("operator",   condOpJson.toString());

            JsonArray caseDefault = new JsonArray();
            caseDefault.add(condRule);

            JsonObject mainRule = ruleItem("amount check", "amount_r", op("amount", "<", "10000"));
            JsonArray caseOther = new JsonArray();
            caseOther.add(andExpr(mainRule));

            JsonObject flow = new JsonObject();
            flow.add("case_default", caseDefault);
            flow.add("case_other",   caseOther);

            // userType=standard → case_default bool condition is false → case_other runs
            // amount=5000 < 10000 → passes
            RuleEngine e = engineWith(Map.of("userType", "standard", "amount", "5000"));
            e.setup(flow.toString(), "branching-test");
            assertTrue(e.run());
        }

        @Test
        @DisplayName("case_default condition not met → case_other rules run and block")
        void caseDefault_notHit_caseOther_blocks() throws Exception {
            JsonObject condOpJson = new Gson().fromJson(op("userType", "=", "premium"), JsonObject.class);
            condOpJson.addProperty("is_bool_condition", true);

            JsonObject condRule = new JsonObject();
            condRule.addProperty("type",       "rule");
            condRule.addProperty("rule_name",  "Is Premium");
            condRule.addProperty("rule_value", "is_premium");
            condRule.addProperty("operator",   condOpJson.toString());

            JsonArray caseDefault = new JsonArray();
            caseDefault.add(condRule);

            JsonObject mainRule = ruleItem("amount check", "amount_r", op("amount", "<", "10000"));
            JsonArray caseOther = new JsonArray();
            caseOther.add(andExpr(mainRule));

            JsonObject flow = new JsonObject();
            flow.add("case_default", caseDefault);
            flow.add("case_other",   caseOther);

            // amount=50000 > 10000 → case_other blocks
            RuleEngine e = engineWith(Map.of("userType", "standard", "amount", "50000"));
            e.setup(flow.toString());
            assertFalse(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 7. Preview mode
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Preview mode")
    class PreviewModeTests {

        @Test
        @DisplayName("rule fails but preview_mode=true → result passes with WARNING status")
        void ruleFailsInPreview_passes() throws Exception {
            JsonObject opJson = new Gson().fromJson(op("age", ">", "18"), JsonObject.class);
            opJson.addProperty("preview_mode", true);

            String flow = flowWithAndExpr(ruleItem("age preview", "r1", opJson.toString()));
            RuleEngine e = engineWith(Map.of("age", "15"));   // fails: 15 < 18
            e.setup(flow);

            assertTrue(e.run());                               // engine passes
            assertTrue(e.getResult().getConclusionJson().contains("WARNING"));
        }

        @Test
        @DisplayName("rule passes with preview_mode=true → status is PASSED normally")
        void rulePassesInPreview_passes() throws Exception {
            JsonObject opJson = new Gson().fromJson(op("age", ">", "18"), JsonObject.class);
            opJson.addProperty("preview_mode", true);

            String flow = flowWithAndExpr(ruleItem("age preview", "r1", opJson.toString()));
            RuleEngine e = engineWith(Map.of("age", "25"));
            e.setup(flow);

            assertTrue(e.run());
            assertTrue(e.getResult().getConclusionJson().contains("PASSED"));
        }

        @Test
        @DisplayName("rule fails without preview_mode → BLOCKED")
        void ruleFails_noPreview_blocked() throws Exception {
            String flow = flowWithAndExpr(ruleItem("age check", "r1", op("age", ">", "18")));
            RuleEngine e = engineWith(Map.of("age", "15"));
            e.setup(flow);

            assertFalse(e.run());
            assertTrue(e.getResult().getConclusionJson().contains("BLOCKED"));
        }
    }

    // -----------------------------------------------------------------------
    // 8. ExpressionValueLoader
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("ExpressionValueLoader")
    class ExpressionValueLoaderTests {

        @Test
        @DisplayName("price * quantity < limit → pass")
        void expression_pass() throws Exception {
            JsonObject opJson = new JsonObject();
            opJson.addProperty("left_value_loader",  EXPR_LOADER);
            opJson.addProperty("left_value",         "label:price * label:quantity");
            opJson.addProperty("right_value_loader", GENERAL_LOADER);
            opJson.addProperty("right_value",        "10000");
            opJson.addProperty("comparator",         "<");

            String flow = flowWithAndExpr(ruleItem("total check", "r1", opJson.toString()));
            RuleEngine e = engineWith(Map.of("price", "100", "quantity", "50")); // 5000 < 10000
            e.setup(flow);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("price * quantity >= limit → block")
        void expression_block() throws Exception {
            JsonObject opJson = new JsonObject();
            opJson.addProperty("left_value_loader",  EXPR_LOADER);
            opJson.addProperty("left_value",         "label:price * label:quantity");
            opJson.addProperty("right_value_loader", GENERAL_LOADER);
            opJson.addProperty("right_value",        "10000");
            opJson.addProperty("comparator",         "<");

            String flow = flowWithAndExpr(ruleItem("total check", "r1", opJson.toString()));
            RuleEngine e = engineWith(Map.of("price", "500", "quantity", "50")); // 25000 > 10000
            e.setup(flow);
            assertFalse(e.run());
        }

        @Test
        @DisplayName("addition expression: a + b > threshold → pass")
        void additionExpression_pass() throws Exception {
            JsonObject opJson = new JsonObject();
            opJson.addProperty("left_value_loader",  EXPR_LOADER);
            opJson.addProperty("left_value",         "label:baseAmount + label:fee");
            opJson.addProperty("right_value_loader", GENERAL_LOADER);
            opJson.addProperty("right_value",        "100");
            opJson.addProperty("comparator",         ">");

            String flow = flowWithAndExpr(ruleItem("total > 100", "r1", opJson.toString()));
            RuleEngine e = engineWith(Map.of("baseAmount", "90", "fee", "20")); // 110 > 100
            e.setup(flow);
            assertTrue(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 9. RuleItemConfigLoader
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("RuleItemConfigLoader")
    class RuleItemConfigLoaderTests {

        @Test
        @DisplayName("operator loaded externally via loader → rule evaluated correctly")
        void loaderProvided_pass() throws Exception {
            String operatorJson = op("balance", ">", "1000");
            RuleEngine.RuleItemConfigLoader loader = id -> "balance_check".equals(id) ? operatorJson : null;

            JsonObject rule = new JsonObject();
            rule.addProperty("type",       "rule");
            rule.addProperty("rule_name",  "Balance check");
            rule.addProperty("rule_value", "balance_check");
            // No "operator" field — loaded via loader

            JsonArray caseOther = new JsonArray();
            caseOther.add(andExpr(rule));
            JsonObject flow = new JsonObject();
            flow.add("case_other", caseOther);

            RuleEngine e = engineWith(Map.of("balance", "5000"));
            e.setup(flow.toString(), loader);
            assertTrue(e.run());
        }

        @Test
        @DisplayName("operator loaded externally via loader → rule blocks")
        void loaderProvided_block() throws Exception {
            String operatorJson = op("balance", ">", "1000");
            RuleEngine.RuleItemConfigLoader loader = id -> "balance_check".equals(id) ? operatorJson : null;

            JsonObject rule = new JsonObject();
            rule.addProperty("type",       "rule");
            rule.addProperty("rule_name",  "Balance check");
            rule.addProperty("rule_value", "balance_check");

            JsonArray caseOther = new JsonArray();
            caseOther.add(andExpr(rule));
            JsonObject flow = new JsonObject();
            flow.add("case_other", caseOther);

            RuleEngine e = engineWith(Map.of("balance", "200"));
            e.setup(flow.toString(), loader);
            assertFalse(e.run());
        }
    }

    // -----------------------------------------------------------------------
    // 10. Additional params
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Additional params")
    class AdditionalParamsTests {

        @Test
        @DisplayName("additional_params appear in result JSON when rule is blocked")
        void additionalParams_inResultJson() throws Exception {
            JsonObject opJson = new Gson().fromJson(op("age", ">", "18"), JsonObject.class);
            JsonObject params = new JsonObject();
            params.addProperty("category",  "age_check");
            params.addProperty("rule_code", "AGE-001");
            opJson.add("additional_params", params);

            String flow = flowWithAndExpr(ruleItem("Age verification", "r1", opJson.toString()));
            RuleEngine e = engineWith(Map.of("age", "15"));
            e.setup(flow);
            e.run();

            String json = e.getResult().getConclusionJson();
            assertTrue(json.contains("age_check"),  "category should appear in JSON");
            assertTrue(json.contains("AGE-001"),    "rule_code should appear in JSON");
        }
    }

    // -----------------------------------------------------------------------
    // 11. Result object verification
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Result object")
    class ResultObjectTests {

        @Test
        @DisplayName("conclusionJson contains rule name and PASSED status")
        void conclusionJson_passed() throws Exception {
            String flow = flowWithAndExpr(ruleItem("Age verification", "r1", op("age", ">", "18")));
            RuleEngine e = engineWith(Map.of("age", "25"));
            e.setup(flow, "my-flow");
            e.run();

            String json = e.getResult().getConclusionJson();
            assertNotNull(json);
            assertTrue(json.contains("Age verification"));
            assertTrue(json.contains(RuleResult.STATUS_PASSED));
        }

        @Test
        @DisplayName("closestBlockedRuleResult points to the failing rule")
        void closestBlockedRule_correct() throws Exception {
            String flow = flowWithAndExpr(
                ruleItem("age check",    "r1", op("age",    ">", "18")),
                ruleItem("status check", "r2", op("status", "=", "active"))
            );
            RuleEngine e = engineWith(Map.of("age", "15", "status", "active"));
            e.setup(flow, "flow");
            assertFalse(e.run());

            RuleResult blocked = e.getResult().getClosestBlockedRuleResult();
            assertNotNull(blocked);
            assertEquals(RuleResult.STATUS_BLOCKED, blocked.status);
            assertEquals("age check", blocked.ruleName);
        }

        @Test
        @DisplayName("conclusionBriefJson is valid non-empty JSON")
        void conclusionBriefJson_nonEmpty() throws Exception {
            String flow = flowWithAndExpr(ruleItem("age check", "r1", op("age", ">", "18")));
            RuleEngine e = engineWith(Map.of("age", "15"));
            e.setup(flow, "flow");
            e.run();

            String briefJson = e.getResult().getConclusionBriefJson();
            assertNotNull(briefJson);
            assertFalse(briefJson.isBlank());
        }

        @Test
        @DisplayName("getAllRules returns all registered rule items")
        void getAllRules_correct() throws Exception {
            String flow = flowWithAndExpr(
                ruleItem("r1", "id_r1", op("age",    ">", "18")),
                ruleItem("r2", "id_r2", op("status", "=", "active"))
            );
            RuleEngine e = engineWith(Map.of("age", "25", "status", "active"));
            e.setup(flow);

            assertEquals(2, e.getAllRules().size());
            assertTrue(e.getAllRules().containsKey("id_r1"));
            assertTrue(e.getAllRules().containsKey("id_r2"));
        }
    }

    // -----------------------------------------------------------------------
    // 12. Exception cases
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Exception handling")
    class ExceptionTests {

        @Test
        @DisplayName("run() before setup() throws NotInitializedException")
        void run_withoutSetup_throws() {
            RuleEngine e = new RuleEngine();
            assertThrows(NotInitializedException.class, e::run);
        }

        @Test
        @DisplayName("setup() with invalid JSON throws RuleSetupException")
        void setup_invalidJson_throws() {
            RuleEngine e = new RuleEngine();
            assertThrows(RuleSetupException.class, () -> e.setup("not-valid-json"));
        }

        @Test
        @DisplayName("setup() with unsupported comparator throws RuleSetupException")
        void setup_unsupportedComparator_throws() {
            RuleEngine e = new RuleEngine();
            JsonObject opJson = new JsonObject();
            opJson.addProperty("left_value_loader",  GENERAL_LOADER);
            opJson.addProperty("left_value",         "x");
            opJson.addProperty("right_value_loader", GENERAL_LOADER);
            opJson.addProperty("right_value",        "y");
            opJson.addProperty("comparator",         "INVALID_OP");

            String flow = flowWithAndExpr(ruleItem("test", "r1", opJson.toString()));
            assertThrows(RuleSetupException.class, () -> e.setup(flow));
        }

        @Test
        @DisplayName("setup() with unknown ValueLoader class throws RuleSetupException")
        void setup_unknownLoader_throws() {
            RuleEngine e = new RuleEngine();
            JsonObject opJson = new JsonObject();
            opJson.addProperty("left_value_loader",  "com.nonexistent.Loader");
            opJson.addProperty("left_value",         "x");
            opJson.addProperty("right_value_loader", GENERAL_LOADER);
            opJson.addProperty("right_value",        "y");
            opJson.addProperty("comparator",         "=");

            String flow = flowWithAndExpr(ruleItem("test", "r1", opJson.toString()));
            assertThrows(RuleSetupException.class, () -> e.setup(flow));
        }
    }

    // -----------------------------------------------------------------------
    // 13. Engine can be re-setup (stateless between setups)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Re-setup support")
    class ReSetupTests {

        @Test
        @DisplayName("same engine re-setup with different config works correctly")
        void reSetup_independentRuns() throws Exception {
            String flow1 = flowWithAndExpr(ruleItem("age check", "r1", op("age", ">", "18")));
            String flow2 = flowWithAndExpr(ruleItem("score check", "r1", op("score", ">", "60")));

            RuleEngine e = new RuleEngine();

            e.setValueHandler(mapHandler(Map.of("age", "25")));
            e.setup(flow1);
            assertTrue(e.run(), "first setup: age=25 > 18 → pass");

            e.setValueHandler(mapHandler(Map.of("score", "40")));
            e.setup(flow2);
            assertFalse(e.run(), "second setup: score=40 < 60 → block");
        }
    }
}
