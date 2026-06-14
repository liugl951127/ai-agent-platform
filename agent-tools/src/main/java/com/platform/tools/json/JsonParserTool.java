package com.platform.tools.json;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.platform.tools.api.Tool;
import com.platform.tools.api.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSON 工具 — 让 Agent 解析 / 查询 / 修改 JSON
 * <p>
 * 操作:
 *   - query:  从 JSON 抽取字段 (支持点路径: "data.user.name")
 *   - count:  统计数组长度 / 对象字段数
 *   - filter: 从数组中过滤满足条件的元素
 *   - keys:   列出对象所有字段名
 */
@Component
@ToolDefinition(
    name = "json",
    description = "JSON 处理工具. 操作: query(点路径取字段), count, filter (数组过滤), keys. 用法: json({op:'query', json:'{...}', path:'a.b.c'})",
    parameters = "{\"type\":\"object\",\"properties\":{" +
        "\"op\":{\"type\":\"string\",\"enum\":[\"query\",\"count\",\"filter\",\"keys\"],\"description\":\"操作\"}," +
        "\"json\":{\"type\":\"string\",\"description\":\"JSON 字符串\"}," +
        "\"path\":{\"type\":\"string\",\"description\":\"点路径, 例 a.b.0.name (query 时必填)\"}," +
        "\"whereField\":{\"type\":\"string\",\"description\":\"过滤字段 (filter 时)\"}," +
        "\"whereOp\":{\"type\":\"string\",\"enum\":[\"eq\",\"ne\",\"gt\",\"lt\",\"contains\"],\"description\":\"比较运算符\"}," +
        "\"whereValue\":{\"type\":\"description\",\"description\":\"比较值\"}" +
        "},\"required\":[\"op\",\"json\"]}",
    category = "general"
)
public class JsonParserTool implements Tool {

    @Override
    public String name() { return "json"; }

    @Override
    public Object execute(Map<String, Object> args) {
        String op = String.valueOf(args.get("op"));
        Object parsed = JSONUtil.parse(String.valueOf(args.get("json")));

        return switch (op) {
            case "query" -> {
                String path = String.valueOf(args.get("path"));
                yield queryPath(parsed, path);
            }
            case "count" -> {
                if (parsed instanceof JSONArray a) yield a.size();
                if (parsed instanceof JSONObject o) yield o.size();
                yield 0;
            }
            case "keys" -> {
                if (parsed instanceof JSONObject o) yield o.keySet();
                throw new IllegalArgumentException("keys 操作只对对象有效");
            }
            case "filter" -> {
                String f = String.valueOf(args.get("whereField"));
                String op2 = String.valueOf(args.get("whereOp"));
                Object v = args.get("whereValue");
                if (!(parsed instanceof JSONArray arr)) throw new IllegalArgumentException("filter 只对数组有效");
                JSONArray out = new JSONArray();
                for (Object item : arr) {
                    if (!(item instanceof JSONObject jo)) continue;
                    Object fv = jo.get(f);
                    if (matches(fv, op2, v)) out.add(jo);
                }
                yield out;
            }
            default -> throw new IllegalArgumentException("不支持的 op: " + op);
        };
    }

    private Object queryPath(Object root, String path) {
        Object cur = root;
        for (String seg : path.split("\\.")) {
            if (cur == null) return null;
            if (cur instanceof JSONObject jo) cur = jo.get(seg);
            else if (cur instanceof JSONArray arr) {
                try { cur = arr.get(Integer.parseInt(seg)); }
                catch (NumberFormatException e) { return null; }
            } else return null;
        }
        return cur;
    }

    private boolean matches(Object fv, String op, Object v) {
        return switch (op) {
            case "eq" -> String.valueOf(fv).equals(String.valueOf(v));
            case "ne" -> !String.valueOf(fv).equals(String.valueOf(v));
            case "gt" -> asDouble(fv) > asDouble(v);
            case "lt" -> asDouble(fv) < asDouble(v);
            case "contains" -> fv != null && String.valueOf(fv).contains(String.valueOf(v));
            default -> false;
        };
    }

    private double asDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); }
        catch (Exception e) { return 0; }
    }
}
