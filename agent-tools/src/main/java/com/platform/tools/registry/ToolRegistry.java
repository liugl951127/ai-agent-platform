package com.platform.tools.registry;

import cn.hutool.json.JSONUtil;
import com.platform.tools.api.Tool;
import com.platform.tools.api.ToolDefinition;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具注册中心
 * <p>
 * Spring 启动时扫描所有 {@link com.platform.tools.api.Tool} Bean,
 * 收集 @ToolDefinition 注解, 暴露给 Agent 框架
 * <p>
 * 设计要点:
 *   - 线程安全 (ConcurrentHashMap)
 *   - AOP 代理类能识别 (AopUtils.getTargetClass)
 *   - 提供多种检索方式: name / category / 全量
 *   - 导出 LLM 友好的 JSON Schema 列表
 */
@Slf4j
@Component
public class ToolRegistry {

    private final ApplicationContext ctx;
    /** name → Tool */
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    /** name → 注解描述 */
    private final Map<String, ToolDefinition> definitions = new ConcurrentHashMap<>();
    /** category → tools */
    private final Map<String, List<String>> byCategory = new ConcurrentHashMap<>();

    public ToolRegistry(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @PostConstruct
    public void init() {
        // 拿所有 Tool bean (含 AOP 代理后的目标类)
        Map<String, Tool> beans = ctx.getBeansOfType(Tool.class);
        for (Map.Entry<String, Tool> e : beans.entrySet()) {
            Tool t = e.getValue();
            Class<?> targetClass = AopUtils.getTargetClass(t);
            ToolDefinition def = targetClass.getAnnotation(ToolDefinition.class);
            if (def == null) {
                log.warn("Tool bean {} 缺少 @ToolDefinition 注解, 已跳过", targetClass.getSimpleName());
                continue;
            }
            String name = def.name().isBlank() ? t.name() : def.name();
            if (name == null || name.isBlank()) {
                log.warn("Tool {} 缺 name(), 跳过", targetClass.getSimpleName());
                continue;
            }
            if (tools.containsKey(name)) {
                log.warn("工具名重复: {} (后注册的覆盖前者), 来源: {}", name, targetClass.getSimpleName());
            }
            tools.put(name, t);
            definitions.put(name, def);
            byCategory.computeIfAbsent(def.category(), k -> new ArrayList<>()).add(name);
        }
        log.info("ToolRegistry 初始化完成, 共注册 {} 个工具: {}", tools.size(), tools.keySet());
    }

    /**
     * 执行工具
     */
    public Object invoke(String name, Map<String, Object> args) throws Exception {
        Tool t = tools.get(name);
        if (t == null) {
            throw new IllegalArgumentException("未注册的工具: " + name + ", 已注册: " + tools.keySet());
        }
        return t.execute(args);
    }

    public Optional<Tool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<String> names() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    public List<String> byCategory(String category) {
        return byCategory.getOrDefault(category, Collections.emptyList());
    }

    /**
     * 转成 OpenAI function calling 格式, 直接喂给 LLM
     * <pre>
     * [
     *   {
     *     "type": "function",
     *     "function": {
     *       "name": "calculator",
     *       "description": "...",
     *       "parameters": { ... JSON Schema ... }
     *     }
     *   }
     * ]
     * </pre>
     */
    public List<Map<String, Object>> toLlmFunctions() {
        List<Map<String, Object>> out = new ArrayList<>(tools.size());
        for (Map.Entry<String, ToolDefinition> e : definitions.entrySet()) {
            ToolDefinition def = e.getValue();
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", e.getKey());
            fn.put("description", def.description());
            // parameters 是 JSON 字符串, 转回 Map
            try {
                fn.put("parameters", JSONUtil.parse(def.parameters()));
            } catch (Exception ex) {
                log.warn("工具 {} 的 parameters JSON 解析失败, 用空对象替代", e.getKey());
                fn.put("parameters", Map.of("type", "object", "properties", Map.of()));
            }
            out.add(Map.of("type", "function", "function", fn));
        }
        return out;
    }

    /**
     * 转成 LLM ReAct 提示词友好的文本格式 (备选, 不用 function calling 时用)
     */
    public String toPromptText() {
        return definitions.entrySet().stream().map(e -> {
            ToolDefinition d = e.getValue();
            return String.format("- %s: %s\n  参数: %s", e.getKey(), d.description(), d.parameters());
        }).collect(Collectors.joining("\n\n"));
    }
}
