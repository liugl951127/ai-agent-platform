package com.platform.agent.multi;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工具调用器 — 转发到 agent-tools 服务
 * <p>
 * 设计: HTTP 而非 Feign
 *   - 避免 agent-tools 启动失败时 agent-agent 也启不来
 *   - 调用失败时 (网络/超时) 返回明确的错误信息, 让 LLM 知道
 */
@Slf4j
@Component
public class ToolInvoker {

    @Value("${agent.tools.base-url:http://agent-tools:9008}")
    private String baseUrl;

    @Value("${agent.tools.timeout-ms:30000}")
    private int timeoutMs;

    public Object invoke(String name, Map<String, Object> args) {
        try (HttpResponse resp = HttpRequest.post(baseUrl + "/tools/invoke")
                .timeout(timeoutMs)
                .body(JSONUtil.toJsonStr(Map.of("name", name, "args", args == null ? Map.of() : args)))
                .header("Content-Type", "application/json")
                .execute()) {
            String body = resp.body();
            if (resp.getStatus() != 200) {
                throw new RuntimeException("tool HTTP " + resp.getStatus() + ": " + truncate(body, 500));
            }
            Map<?,?> m = JSONUtil.parseObj(body);
            if (Boolean.FALSE.equals(m.get("ok"))) {
                throw new RuntimeException("tool 报错: " + m.get("error"));
            }
            return m.get("result");
        } catch (Exception e) {
            log.warn("工具 {} 调用失败: {}", name, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /** 拉取所有工具 schema, 过滤出 allowed 的, 拼成 LLM prompt 文本 */
    public String describe(List<String> allowed) {
        try (HttpResponse resp = HttpRequest.get(baseUrl + "/tools/list?format=json")
                .timeout(timeoutMs).execute()) {
            if (resp.getStatus() != 200) {
                return "(工具服务不可用: HTTP " + resp.getStatus() + ")";
            }
            Map<?,?> m = JSONUtil.parseObj(resp.body());
            List<?> fns = (List<?>) m.get("functions");
            if (fns == null) fns = List.of();
            StringBuilder sb = new StringBuilder();
            for (Object f : fns) {
                @SuppressWarnings("unchecked")
                Map<String,Object> fn = (Map<String,Object>) ((Map<String,Object>) f).get("function");
                if (!allowed.contains(fn.get("name"))) continue;
                sb.append("- ").append(fn.get("name")).append(": ")
                  .append(fn.get("description")).append("\n  参数: ")
                  .append(JSONUtil.toJsonStr(fn.get("parameters"))).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("拉取工具列表失败: {}", e.getMessage());
            return "(工具服务不可达)";
        }
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }
}
