package com.platform.tools.calculator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单数学表达式求值器 (递归下降, 支持 + - * / ^ () 和数学函数)
 * <p>
 * 避免引外部脚本引擎 (Nashorn JDK 17 移除), 自实现一个 100 行的求值器
 * 精度: double, 适合一般计算, 金融场景请用 BigDecimal
 * <p>
 * 语法:
 *   expr   = term (('+'|'-') term)*
 *   term   = factor (('*'|'/') factor)*
 *   factor = unary ('^' factor)?          # 右结合
 *   unary  = ('+'|'-') unary | atom
 *   atom   = number | func '(' expr ')' | '(' expr ')'
 *   func   = sin | cos | tan | log | ln | sqrt | exp | abs
 */
public final class ExprEvaluator {

    private final String s;
    private int pos = 0;

    private ExprEvaluator(String s) { this.s = s.replaceAll("\\s+", ""); }

    public static double eval(String expr) {
        if (expr == null || expr.isBlank()) throw new IllegalArgumentException("空表达式");
        ExprEvaluator e = new ExprEvaluator(expr);
        double v = e.parseExpr();
        e.skipTail();
        if (e.pos < e.s.length()) {
            throw new IllegalArgumentException("表达式尾部多余: '" + e.s.substring(e.pos) + "'");
        }
        return v;
    }

    private double parseExpr() {
        double v = parseTerm();
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '+') { pos++; v += parseTerm(); }
            else if (c == '-') { pos++; v -= parseTerm(); }
            else break;
        }
        return v;
    }

    private double parseTerm() {
        double v = parseFactor();
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '*') { pos++; v *= parseFactor(); }
            else if (c == '/') {
                pos++;
                double d = parseFactor();
                if (d == 0) throw new ArithmeticException("除零");
                v /= d;
            } else break;
        }
        return v;
    }

    private double parseFactor() {
        double v = parseUnary();
        if (pos < s.length() && s.charAt(pos) == '^') {
            pos++;
            v = Math.pow(v, parseFactor()); // 右结合
        }
        return v;
    }

    private double parseUnary() {
        if (pos < s.length() && s.charAt(pos) == '+') { pos++; return parseUnary(); }
        if (pos < s.length() && s.charAt(pos) == '-') { pos++; return -parseUnary(); }
        return parseAtom();
    }

    private static final Pattern NUM = Pattern.compile("^\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

    private double parseAtom() {
        if (pos >= s.length()) throw new IllegalArgumentException("表达式意外结束 @ " + pos);
        char c = s.charAt(pos);

        // (expr)
        if (c == '(') {
            pos++;
            double v = parseExpr();
            expect(')');
            return v;
        }

        // 数字
        if (Character.isDigit(c) || c == '.') {
            Matcher m = NUM.matcher(s.substring(pos));
            if (!m.find()) throw new IllegalArgumentException("无效数字 @ " + pos);
            pos += m.end();
            return Double.parseDouble(m.group());
        }

        // 标识符: 函数或常量
        if (Character.isLetter(c) || c == '_') {
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) pos++;
            String id = s.substring(start, pos);
            // 函数调用
            if (pos < s.length() && s.charAt(pos) == '(') {
                pos++;
                double arg = parseExpr();
                expect(')');
                return applyFunc(id, arg);
            }
            // 常量
            return switch (id.toLowerCase()) {
                case "pi" -> Math.PI;
                case "e"  -> Math.E;
                default -> throw new IllegalArgumentException("未知标识符: " + id);
            };
        }
        throw new IllegalArgumentException("意外字符: '" + c + "' @ " + pos);
    }

    private double applyFunc(String fn, double a) {
        return switch (fn.toLowerCase()) {
            case "sin" -> Math.sin(a);
            case "cos" -> Math.cos(a);
            case "tan" -> Math.tan(a);
            case "log" -> Math.log10(a);
            case "ln"  -> Math.log(a);
            case "sqrt" -> Math.sqrt(a);
            case "abs" -> Math.abs(a);
            case "exp" -> Math.exp(a);
            case "asin" -> Math.asin(a);
            case "acos" -> Math.acos(a);
            case "atan" -> Math.atan(a);
            default -> throw new IllegalArgumentException("未知函数: " + fn);
        };
    }

    private void expect(char c) {
        if (pos >= s.length() || s.charAt(pos) != c) {
            throw new IllegalArgumentException("期望 '" + c + "' @ " + pos);
        }
        pos++;
    }

    private void skipTail() {
        while (pos < s.length() && s.charAt(pos) == ';') pos++;
    }
}
