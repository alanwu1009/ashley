# Ashley Rule Engine

A lightweight, JSON-driven rule engine for Java, designed for risk control and business rule evaluation.

## Features

- **JSON-driven configuration** — define rules as plain JSON, no code changes needed
- **AND / OR expressions** — compose complex rule logic with nested expressions
- **Conditional branching** — `case_default` / `case_other` flow control
- **Preview mode** — run rules in warning-only mode without blocking
- **Boolean conditions** — use rules as conditions to gate downstream rule execution
- **Expression evaluation** — compute dynamic values via JEXL math expressions
- **Custom value loaders** — plug in your own data-fetching logic via `ValueHandler`
- **Detailed results** — full JSON audit trail of every rule evaluation

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.ashley</groupId>
    <artifactId>ashley</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Define a rule flow in JSON

```json
{
  "case_default": [
    {
      "type": "rule",
      "rule_name": "Age must be >= 18",
      "rule_value": "age_check",
      "operator": "{\"left_value_loader\":\"io.ashley.rule.GeneralValueLoader\",\"left_value\":\"userAge\",\"right_value_loader\":\"io.ashley.rule.GeneralValueLoader\",\"right_value\":\"18\",\"comparator\":\">=\"}"
    }
  ]
}
```

### 2. Implement `ValueHandler` to supply your data

```java
public class MyValueHandler implements ValueHandler {
    private final Map<String, Object> context;

    public MyValueHandler(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public Object handle(Object key, RuleEngine engine) {
        return context.get(key.toString());
    }
}
```

### 3. Run the engine

```java
String ruleConfig = "..."; // your JSON rule config

RuleEngine engine = new RuleEngine();
engine.setValueHandler(new MyValueHandler(Map.of("userAge", "25")));
engine.setup(ruleConfig, "my-rule-flow");

boolean passed = engine.run();
RuleEngine.Result result = engine.getResult();

System.out.println("Passed: " + result.getHasPassed());
System.out.println("Detail: " + result.getConclusionJson());
```

## Rule Configuration

### Operator JSON fields

| Field | Description |
|-------|-------------|
| `left_value_loader` | Fully qualified class name of the `ValueLoader` for the left operand |
| `left_value` | Value or key passed to the left value loader |
| `right_value_loader` | Fully qualified class name of the `ValueLoader` for the right operand |
| `right_value` | Value or key passed to the right value loader |
| `comparator` | One of: `=`, `!=`, `>`, `<`, `contains`, `exact contains`, `notcontains`, `exactnotcontains` |
| `preview_mode` | *(optional)* `true` to emit a WARNING instead of BLOCKED |
| `is_bool_condition` | *(optional)* `true` to use this rule as a conditional gate |
| `additional_params` | *(optional)* arbitrary key-value pairs included in the result |

### Flow structure

```json
{
  "case_default": [ /* rules applied unconditionally */ ],
  "case_other":   [ /* rules applied when case_default conditions are not met */ ]
}
```

### Rule types

| `type` value | Description |
|-------------|-------------|
| `rule` | A single rule item |
| `and_expression` | All contained rules must pass |
| `or_expression` | At least one contained rule must pass |

### Built-in ValueLoaders

| Class | Description |
|-------|-------------|
| `io.ashley.rule.GeneralValueLoader` | Resolves the value via the configured `ValueHandler`; falls back to the raw string if no handler is set |
| `io.ashley.rule.ExpressionValueLoader` | Evaluates a JEXL math expression; reference variables with `label:variableName` |

## Extending the Engine

### Custom ValueLoader

Implement `io.ashley.core.ValueLoader` and reference the fully qualified class name in your rule JSON:

```java
public class RedisValueLoader implements ValueLoader {
    private String key;
    private RuleEngine engine;

    @Override
    public void setValue(Object v, RuleEngine engine) {
        this.key = v.toString();
        this.engine = engine;
    }

    @Override
    public Object loadValue() throws HandleValueException {
        return redisClient.get(key); // your implementation
    }
}
```

## Result

After `engine.run()`, call `engine.getResult()` to get a `Result` object:

| Method | Description |
|--------|-------------|
| `getHasPassed()` | `true` if all rules passed |
| `getConclusionJson()` | Full JSON audit trail |
| `getConclusionBriefJson()` | Simplified JSON summary |
| `getConclusionBrief()` | HTML-formatted summary string |
| `getClosestBlockedRuleResult()` | The first rule that caused a BLOCKED status |

## License

Apache License 2.0
