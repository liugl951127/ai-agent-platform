package com.platform.tools.calculator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Calculator 表达式求值器测试
 */
class ExprEvaluatorTest {

    private void check(String expr, double expected) {
        double v = ExprEvaluator.eval(expr);
        assertEquals(expected, v, 1e-9, "expr=" + expr);
    }

    @Test
    void testBasicOps() {
        check("1+2", 3);
        check("10-3", 7);
        check("4*5", 20);
        check("20/4", 5);
        check("2+3*4", 14);
        check("(2+3)*4", 20);
    }

    @Test
    void testPower() {
        check("2^10", 1024);
        check("2^3^2", 512);  // 右结合: 2^(3^2) = 2^9 = 512
        check("3^2+4", 13);
    }

    @Test
    void testUnary() {
        check("-5", -5);
        check("-(2+3)", -5);
        check("--5", 5);
    }

    @Test
    void testMathFuncs() {
        check("sqrt(16)", 4);
        check("sin(0)", 0);
        check("cos(0)", 1);
        check("log(100)", 2);
        check("ln(e)", 1);
        check("abs(-7)", 7);
    }

    @Test
    void testConstants() {
        check("pi", Math.PI);
        check("2*pi", 2 * Math.PI);
    }

    @Test
    void testNestedParens() {
        check("((1+2)*(3+4))", 21);
        check("(((((1)))))", 1);
    }

    @Test
    void testWhitespace() {
        check("  1  +  2 ", 3);
        check("\t1\t+\t2\t", 3);
    }

    @Test
    void testDivideByZero() {
        assertThrows(ArithmeticException.class, () -> ExprEvaluator.eval("1/0"));
    }

    @Test
    void testInvalidExpression() {
        assertThrows(IllegalArgumentException.class, () -> ExprEvaluator.eval("1+"));
        assertThrows(IllegalArgumentException.class, () -> ExprEvaluator.eval("((1+2)"));
        assertThrows(IllegalArgumentException.class, () -> ExprEvaluator.eval("foo()"));
        assertThrows(IllegalArgumentException.class, () -> ExprEvaluator.eval(""));
    }

    @Test
    void testDecimal() {
        check("0.1+0.2", 0.3);
        check("1.5*2", 3);
    }
}
