package com.platform.tools.calculator;

import com.platform.tools.api.Tool;
import com.platform.tools.api.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 计算器 — 数学表达式求值
 * <p>
 * 基于 JavaScript 引擎 (JDK 自带 Nashorn, JDK 17 已移除 → 改用 commons-math3 + 手写解析)
 * 实际用 commons-math3 的 ArithmeticUtils + 自实现的简单算式解析
 * <p>
 * 支持: + - * / ^ () 和数学函数 (sin/cos/log/sqrt/...)
 * 例: 表达式 "2*(3+4)^2" → 98
 */
@Component
@ToolDefinition(
    name = "calculator",
    description = "数学表达式求值, 支持 + - * / ^ () 和 sin/cos/log/sqrt/exp 等数学函数. 例: calculator({expression: '2*(3+4)^2'})",
    parameters = "{\"type\":\"object\",\"properties\":{\"expression\":{\"type\":\"string\",\"description\":\"数学表达式,如 2*(3+4)^2\"}},\"required\":[\"expression\"]}",
    category = "math"
)
public class CalculatorTool implements Tool {

    @Override
    public String name() { return "calculator"; }

    @Override
    public Object execute(Map<String, Object> args) {
        String expr = String.valueOf(args.get("expression"));
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException("expression 不能为空");
        }
        return ExprEvaluator.eval(expr);
    }
}
