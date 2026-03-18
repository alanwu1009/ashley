# Ashley Rule Engine

[English](#english) | [中文](#中文)

---

<a name="english"></a>
## English

A lightweight, JSON-driven rule engine for Java, designed for risk control and business rule evaluation.

### Features

- **JSON-driven configuration** — define rules as plain JSON, no code changes needed
- **AND / OR expressions** — compose complex rule logic with nested expressions
- **Conditional branching** — `case_default` / `case_other` flow control
- **Preview mode** — run rules in warning-only mode without blocking
- **Boolean conditions** — use rules as conditions to gate downstream rule execution
- **Expression evaluation** — compute dynamic values via JEXL math expressions
- **Custom value loaders** — plug in your own data-fetching logic via `ValueHandler`
- **Detailed results** — full JSON audit trail of every rule evaluation

### Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.ashley</groupId>
    <artifactId>ashley</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Quick Start

#### 1. Define a rule flow in JSON

```json
{
  "case_other": [
    {
      "type": "and_expression",
      "and": [
        {
          "type": "rule",
          "rule_name": "Age must be >= 18",
          "rule_value": "age_check",
          "operator": "{\"left_value_loader\":\"io.ashley.rule.GeneralValueLoader\",\"left_value\":\"userAge\",\"right_value_loader\":\"io.ashley.rule.GeneralValueLoader\",\"right_value\":\"18\",\"comparator\":\">\"}"
        }
      ]
    }
  ]
}
```

#### 2. Implement `ValueHandler` to supply your data

```java
public class MyValueHandler implements ValueHandler {
    private final Map<String, Object> context;

    public MyValueHandler(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public Object handle(Object key, RuleEngine engine) {
        String k = key.toString();
        // Return value from context if exists, otherwise treat as literal constant
        return context.containsKey(k) ? context.get(k) : k;
    }
}
```

#### 3. Run the engine

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

### Rule Configuration

#### Operator JSON fields

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

#### Flow structure

```json
{
  "case_default": [ /* condition rules — if matched, execute the associated and_flow */ ],
  "case_other":   [ /* main rules — executed when no case_default condition matches */ ]
}
```

#### Rule types

| `type` value | Description |
|-------------|-------------|
| `rule` | A single rule item |
| `and_expression` | All contained rules must pass |
| `or_expression` | At least one contained rule must pass |

#### Built-in ValueLoaders

| Class | Description |
|-------|-------------|
| `io.ashley.rule.GeneralValueLoader` | Resolves the value via the configured `ValueHandler`; returns the raw string if no handler is set |
| `io.ashley.rule.ExpressionValueLoader` | Evaluates a JEXL math expression; reference variables with `label:variableName` |

### Extending the Engine

#### Custom ValueLoader

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

### Result

After `engine.run()`, call `engine.getResult()` to get a `Result` object:

| Method | Description |
|--------|-------------|
| `getHasPassed()` | `true` if all rules passed |
| `getConclusionJson()` | Full JSON audit trail |
| `getConclusionBriefJson()` | Simplified JSON summary |
| `getConclusionBrief()` | HTML-formatted summary string |
| `getClosestBlockedRuleResult()` | The first rule that caused a BLOCKED status |

### License

Apache License 2.0

---

<a name="中文"></a>
## 中文

一个轻量级、基于 JSON 配置的 Java 规则引擎，专为风控场景和业务规则评估设计。

### 特性

- **JSON 驱动配置** — 规则以纯 JSON 定义，无需修改代码即可调整规则逻辑
- **AND / OR 表达式** — 支持嵌套的与/或逻辑组合，表达复杂规则
- **条件分支** — 通过 `case_default` / `case_other` 实现规则流程控制
- **预览模式** — 规则可在 WARNING 模式下运行，不实际拦截，便于上线前验证
- **布尔条件** — 将规则作为条件门控，决定是否执行后续规则
- **表达式求值** — 通过 JEXL 表达式动态计算左值/右值（支持加减乘除等运算）
- **自定义数据加载** — 通过实现 `ValueHandler` 接入任意数据源
- **详细执行结果** — 每条规则的执行状态均可输出为 JSON，方便审计和排查

### 引入依赖

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>io.ashley</groupId>
    <artifactId>ashley</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 快速上手

#### 第一步：用 JSON 定义规则流

```json
{
  "case_other": [
    {
      "type": "and_expression",
      "and": [
        {
          "type": "rule",
          "rule_name": "年龄必须大于18岁",
          "rule_value": "age_check",
          "operator": "{\"left_value_loader\":\"io.ashley.rule.GeneralValueLoader\",\"left_value\":\"userAge\",\"right_value_loader\":\"io.ashley.rule.GeneralValueLoader\",\"right_value\":\"18\",\"comparator\":\">\"}"
        }
      ]
    }
  ]
}
```

#### 第二步：实现 `ValueHandler` 提供业务数据

```java
public class MyValueHandler implements ValueHandler {
    private final Map<String, Object> context;

    public MyValueHandler(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public Object handle(Object key, RuleEngine engine) {
        String k = key.toString();
        // 如果 key 存在于上下文中则返回对应值，否则视为字面量常量直接返回
        return context.containsKey(k) ? context.get(k) : k;
    }
}
```

#### 第三步：执行规则引擎

```java
String ruleConfig = "..."; // JSON 规则配置

RuleEngine engine = new RuleEngine();
engine.setValueHandler(new MyValueHandler(Map.of("userAge", "25")));
engine.setup(ruleConfig, "my-rule-flow");

boolean passed = engine.run();
RuleEngine.Result result = engine.getResult();

System.out.println("是否通过：" + result.getHasPassed());
System.out.println("详细结果：" + result.getConclusionJson());
```

### 规则配置说明

#### Operator 字段说明

| 字段 | 说明 |
|------|------|
| `left_value_loader` | 左值加载器的完整类名（实现 `ValueLoader` 接口） |
| `left_value` | 传入左值加载器的值或变量 key |
| `right_value_loader` | 右值加载器的完整类名 |
| `right_value` | 传入右值加载器的值或字面量 |
| `comparator` | 比较符，支持：`=`、`!=`、`>`、`<`、`contains`、`exact contains`、`notcontains`、`exactnotcontains` |
| `preview_mode` | （可选）设为 `true` 时，规则失败只产生 WARNING 而不拦截 |
| `is_bool_condition` | （可选）设为 `true` 时，规则作为条件判断（返回 YES/NO），不参与拦截 |
| `additional_params` | （可选）附加的键值对，会被带入执行结果中，便于后续处理 |

#### 比较符说明

| 比较符 | 说明 | 值类型 |
|--------|------|--------|
| `=` | 等于 | 字符串比较 |
| `!=` | 不等于 | 字符串比较 |
| `>` | 大于 | 数值比较 |
| `<` | 小于 | 数值比较 |
| `contains` | 左值列表中包含右值列表中的任意一个 | 列表（逗号分隔） |
| `exact contains` | 左值列表包含右值列表中的全部元素 | 列表 |
| `notcontains` | 右值列表中存在不在左值列表中的元素 | 列表 |
| `exactnotcontains` | 左值列表不包含右值列表中任何元素 | 列表 |

#### 流程结构说明

```json
{
  "case_default": [ /* 条件分支：满足条件时执行 and_flow 中的规则 */ ],
  "case_other":   [ /* 默认分支：没有 case_default 命中时执行 */ ]
}
```

典型用法：
- `case_default` 放置**前置条件判断**（如用户类型判断），满足时走对应的规则分支
- `case_other` 放置**主规则集**，是大多数场景的规则入口

#### 规则类型

| `type` 值 | 说明 |
|-----------|------|
| `rule` | 单条规则 |
| `and_expression` | AND 表达式，其中所有规则都必须通过 |
| `or_expression` | OR 表达式，至少一条规则通过即可 |

#### 内置 ValueLoader

| 类名 | 说明 |
|------|------|
| `io.ashley.rule.GeneralValueLoader` | 通用加载器，将 `left_value` / `right_value` 传给 `ValueHandler` 解析；未配置 handler 时直接返回原始字符串 |
| `io.ashley.rule.ExpressionValueLoader` | 表达式加载器，支持 JEXL 数学表达式，用 `label:变量名` 引用变量，如 `label:price * label:quantity` |

### 扩展：自定义 ValueLoader

实现 `io.ashley.core.ValueLoader` 接口，并在规则 JSON 中填写完整类名：

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
        return redisClient.get(key); // 从 Redis 加载数据
    }
}
```

### 执行结果说明

`engine.run()` 执行后，通过 `engine.getResult()` 获取 `Result` 对象：

| 方法 | 说明 |
|------|------|
| `getHasPassed()` | 是否通过，`true` 表示所有规则均通过 |
| `getConclusionJson()` | 完整的 JSON 执行结果，包含每条规则的状态 |
| `getConclusionBriefJson()` | 简化版 JSON 执行摘要 |
| `getConclusionBrief()` | HTML 格式的文字摘要 |
| `getClosestBlockedRuleResult()` | 第一条导致 BLOCKED 的规则结果，便于快速定位拦截原因 |

#### 规则状态值

| 状态 | 说明 |
|------|------|
| `PASSED` | 规则通过 |
| `BLOCKED` | 规则拦截 |
| `WARNING` | 规则失败但处于预览模式，不实际拦截 |
| `YES` / `NO` | 布尔条件的判断结果（`is_bool_condition=true` 时） |
| `IGNORED` | 规则被忽略（表达式中无规则命中时） |

### 开源协议

Apache License 2.0
