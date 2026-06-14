package com.platform.tools.api;

import com.platform.tools.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 工具 HTTP 接口
 * <p>
 * 给 ReAct / Plan-Execute 用的远端调用入口
 */
@Slf4j
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolRegistry registry;

    /** 列出所有工具 (LLM function calling 格式) */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(defaultValue = "json") String format) {
        if ("prompt".equalsIgnoreCase(format)) {
            return Map.of("format", "prompt", "text", registry.toPromptText());
        }
        return Map.of("format", "json", "functions", registry.toLlmFunctions(),
                "count", registry.names().size());
    }

    /** 调用单个工具 */
    @PostMapping("/invoke")
    public Map<String, Object> invoke(@RequestBody InvokeRequest req) {
        try {
            Object result = registry.invoke(req.name, req.args == null ? Map.of() : req.args);
            return Map.of("ok", true, "name", req.name, "result", result);
        } catch (Exception e) {
            log.warn("工具 {} 调用失败: {}", req.name, e.getMessage());
            return Map.of("ok", false, "name", req.name, "error", e.getMessage());
        }
    }

    /** 批量调用 */
    @PostMapping("/invoke/batch")
    public List<Map<String, Object>> invokeBatch(@RequestBody List<InvokeRequest> reqs) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (InvokeRequest r : reqs) {
            try {
                Object res = registry.invoke(r.name, r.args == null ? Map.of() : r.args);
                out.add(Map.of("ok", true, "name", r.name, "result", res));
            } catch (Exception e) {
                out.add(Map.of("ok", false, "name", r.name, "error", e.getMessage()));
            }
        }
        return out;
    }

    public static class InvokeRequest {
        public String name;
        public Map<String, Object> args;
    }
}
