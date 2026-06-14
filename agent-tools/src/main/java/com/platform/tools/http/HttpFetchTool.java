package com.platform.tools.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.platform.tools.api.Tool;
import com.platform.tools.api.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 抓取工具 — 让 Agent 能联网查资料
 * <p>
 * 支持: GET / POST / 任意 Header / JSON Body / 超时 / 截断响应 (避免 token 爆炸)
 * <p>
 * 安全: 默认禁用 localhost / 私网 IP (防 SSRF), 用 SSRFGuard 检查
 */
@Component
@ToolDefinition(
    name = "http_fetch",
    description = "HTTP 请求工具, 支持 GET/POST/PUT/DELETE, 可设 header / JSON body / 超时 / 响应截断. 返回 {status, body, headers}. 用法: http_fetch({url:'https://...', method:'GET', maxLength:5000})",
    parameters = "{\"type\":\"object\",\"properties\":{" +
        "\"url\":{\"type\":\"string\",\"description\":\"完整 URL, 必须 http/https\"}," +
        "\"method\":{\"type\":\"string\",\"enum\":[\"GET\",\"POST\",\"PUT\",\"DELETE\"],\"description\":\"HTTP 方法, 默认 GET\"}," +
        "\"headers\":{\"type\":\"object\",\"description\":\"额外 header, 键值对\",\"additionalProperties\":{\"type\":\"string\"}}," +
        "\"body\":{\"type\":\"description\":\"请求体 (POST/PUT)\",\"oneOf\":[{\"type\":\"string\"},{\"type\":\"object\"}]}," +
        "\"timeout\":{\"type\":\"integer\",\"description\":\"超时毫秒, 默认 15000\"}," +
        "\"maxLength\":{\"type\":\"integer\",\"description\":\"响应体最大字符数, 超过截断, 默认 8000\"}" +
        "},\"required\":[\"url\"]}",
    category = "web"
)
public class HttpFetchTool implements Tool {

    @Override
    public String name() { return "http_fetch"; }

    @Override
    public Object execute(Map<String, Object> args) {
        String url = String.valueOf(args.get("url"));
        String method = String.valueOf(args.getOrDefault("method", "GET")).toUpperCase();
        int timeout = ((Number) args.getOrDefault("timeout", 15000)).intValue();
        int maxLen = ((Number) args.getOrDefault("maxLength", 8000)).intValue();

        SSRFGuard.check(url);

        HttpRequest req = HttpRequest.of(url).method(method.equals("GET") ? cn.hutool.http.Method.GET :
                method.equals("POST") ? cn.hutool.http.Method.POST :
                method.equals("PUT") ? cn.hutool.http.Method.PUT :
                cn.hutool.http.Method.DELETE)
                .timeout(timeout);

        // headers
        Object headers = args.get("headers");
        if (headers instanceof Map<?,?> hm) {
            for (Map.Entry<?,?> e : hm.entrySet()) {
                req.header(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }

        // body
        if (args.containsKey("body") && !"GET".equals(method)) {
            Object body = args.get("body");
            if (body instanceof Map || body instanceof java.util.List) {
                req.body(cn.hutool.json.JSONUtil.toJsonStr(body));
                req.header("Content-Type", "application/json");
            } else {
                req.body(String.valueOf(body));
            }
        }

        try (HttpResponse resp = req.execute()) {
            String body = resp.body();
            boolean truncated = false;
            if (body != null && body.length() > maxLen) {
                body = body.substring(0, maxLen) + "\n... (truncated, original length=" + body.length() + ")";
                truncated = true;
            }
            Map<String, Object> out = new HashMap<>();
            out.put("status", resp.getStatus());
            out.put("body", body == null ? "" : body);
            Map<String, String> hh = new HashMap<>();
            resp.headers().forEach((k, vs) -> {
                if (vs != null && !vs.isEmpty()) hh.put(k, vs.get(0));
            });
            out.put("headers", hh);
            out.put("truncated", truncated);
            return out;
        }
    }
}
