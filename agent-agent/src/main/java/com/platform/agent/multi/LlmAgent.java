package com.platform.agent.multi;

import cn.hutool.json.JSONUtil;
import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.multi.memory.MemoryStore;
import com.platform.agent.multi.reflection.ReflectionEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * LLM Agent — 用大模型 + 工具(通过 agent-tools 远端) 实现单 Agent 能力
 * <p>
 * 算法: ReAct + Reflection + Tool
 *   1. 拼 prompt (system role + 历史 + 工具列表 + 用户输入)
 *   2. 调 LLM
 *   3. 解析: 是工具调用? → 调工具 → 把结果作为 Observation → 继续
 *              是 Final Answer? → 反思 (Reflection) → 返回
 *   4. 超过 maxSteps → 强制停止
 * <p>
 * 这个类核心 — 取代了旧的 ReactExecutor, 但接口统一为 Agent
 */
@Slf4j
public class LlmAgent implements Agent {

    private final String name;
    private final String role;
    private final String systemPrompt;
    private final Long modelId;
    private final List<String> allowedTools;
    private final LlmFeignClient llmFeign;
    private final ToolInvoker toolInvoker;
    private final ReflectionEngine reflector;
    private final MemoryStore memory;

    public LlmAgent(String name, String role, String systemPrompt, Long modelId,
                    List<String> allowedTools,
                    LlmFeignClient llmFeign, ToolInvoker toolInvoker,
                    ReflectionEngine reflector, MemoryStore memory) {
        this.name = name;
        this.role = role;
        this.systemPrompt = systemPrompt;
        this.modelId = modelId;
        this.allowedTools = allowedTools == null ? List.of() : allowedTools;
        this.llmFeign = llmFeign;
        this.toolInvoker = toolInvoker;
        this.reflector = reflector;
        this.memory = memory;
    }

    @Override public String name() { return name; }
    @Override public String role() { return role; }

    @Override
    public AgentResponse handle(AgentContext ctx) {
        long t0 = System.currentTimeMillis();
        List<AgentResponse.ActionRecord> actions = new ArrayList<>();
        Map<String,Object> artifacts = new HashMap<>();

        // 1. 准备消息
        List<Map<String,Object>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        if (!allowedTools.isEmpty()) {
            String toolsDesc = toolInvoker.describe(allowedTools);
            messages.add(Map.of("role", "system", "content", "可用工具:\n" + toolsDesc));
        }
        if (ctx.getHistory() != null) messages.addAll(ctx.getHistory());
        if (ctx.getInbox() != null && !ctx.getInbox().isEmpty()) {
            for (AgentMessage m : ctx.getInbox()) {
                messages.add(Map.of("role", "system", "content",
                    "[" + m.getFrom() + "]: " + m.getBody()));
            }
        }
        messages.add(Map.of("role", "user", "content", ctx.getUserInput()));

        // 2. ReAct 循环
        StringBuilder thought = new StringBuilder();
        String finalAnswer = null;
        String stopReason = "max_steps";
        int totalPrompt = 0, totalCompletion = 0;

        for (int step = 0; step < ctx.getMaxSteps(); step++) {
            if (System.currentTimeMillis() - t0 > ctx.getTimeoutMs()) {
                stopReason = "timeout";
                break;
            }
            Map<String,Object> llmResp;
            try {
                llmResp = llmFeign.chat(modelId, messages);
            } catch (Exception e) {
                log.error("LLM 调用失败 ({}): {}", name, e.getMessage());
                return AgentResponse.builder()
                    .content("LLM 不可达: " + e.getMessage())
                    .thought(thought.toString())
                    .stopReason("error")
                    .elapsedMs(System.currentTimeMillis() - t0)
                    .actions(actions).artifacts(artifacts).build();
            }
            totalPrompt += intVal(llmResp, "promptTokens");
            totalCompletion += intVal(llmResp, "completionTokens");
            String text = String.valueOf(llmResp.getOrDefault("content", ""));
            thought.append("\n[step ").append(step).append("] ").append(text);

            ParsedAction pa = parseAction(text);
            if (pa != null && pa.isTool) {
                AgentResponse.ActionRecord ar = AgentResponse.ActionRecord.builder()
                        .type("tool").name(pa.action).input(pa.args)
                        .ok(false).build();
                long t1 = System.currentTimeMillis();
                try {
                    Object result = toolInvoker.invoke(pa.action, pa.args);
                    ar.setOutput(result);
                    ar.setOk(true);
                    thought.append("\n  Observation[").append(pa.action).append("]: ").append(result);
                    messages.add(Map.of("role", "assistant", "content", text));
                    messages.add(Map.of("role", "user", "content",
                        "Observation(" + pa.action + "): " + truncate(JSONUtil.toJsonStr(result), 2000)));
                } catch (Exception e) {
                    ar.setOutput(e.getMessage());
                    thought.append("\n  Observation[ERR]: ").append(e.getMessage());
                    messages.add(Map.of("role", "user", "content",
                        "Observation(" + pa.action + ") FAILED: " + e.getMessage()));
                } finally {
                    ar.setElapsedMs(System.currentTimeMillis() - t1);
                    actions.add(ar);
                }
                continue;
            }

            if (pa != null && pa.isFinal) {
                finalAnswer = pa.answer;
                stopReason = "ok";
                break;
            }
            finalAnswer = text;
            stopReason = "ok";
            break;
        }

        if (finalAnswer == null) {
            finalAnswer = "(达到 maxSteps 仍未得出结论)";
        }

        AgentResponse resp = AgentResponse.builder()
            .content(finalAnswer)
            .thought(thought.toString())
            .actions(actions)
            .artifacts(artifacts)
            .promptTokens(totalPrompt)
            .completionTokens(totalCompletion)
            .elapsedMs(System.currentTimeMillis() - t0)
            .stopReason(stopReason)
            .build();

        // Reflection
        if (reflector != null && !"error".equals(stopReason)) {
            resp = reflector.reflect(this, ctx, resp);
        }

        // Memory
        if (memory != null && ctx.getSessionId() != null) {
            memory.append(ctx.getSessionId(), "user", ctx.getUserInput());
            memory.append(ctx.getSessionId(), "assistant:" + name, resp.getContent());
        }

        return resp;
    }

    private ParsedAction parseAction(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("{") && t.endsWith("}")) {
            try {
                Map<?,?> m = JSONUtil.parseObj(t);
                Object action = m.get("action");
                if (action != null) {
                    Map<String,Object> args = m.get("args") instanceof Map
                        ? (Map<String,Object>) m.get("args") : new HashMap<>();
                    if ("final_answer".equals(action) || "final".equals(action)) {
                        Object ans = m.get("answer");
                        if (ans == null) ans = m.get("content");
                        ParsedAction p = new ParsedAction();
                        p.isFinal = true;
                        p.answer = String.valueOf(ans);
                        return p;
                    }
                    ParsedAction p = new ParsedAction();
                    p.isTool = true;
                    p.action = String.valueOf(action);
                    p.args = args;
                    return p;
                }
            } catch (Exception ignore) { }
        }
        java.util.regex.Matcher ma = java.util.regex.Pattern.compile(
            "Action\\s*:\\s*([a-zA-Z_][\\w]*)\\s*\\((.+?)\\)", java.util.regex.Pattern.DOTALL).matcher(t);
        if (ma.find()) {
            String tool = ma.group(1);
            String raw = ma.group(2).trim();
            Map<String,Object> args = new HashMap<>();
            if (raw.startsWith("{")) {
                try { args = JSONUtil.parseObj(raw); } catch (Exception e) {
                    args.put("expression", raw);
                }
            } else {
                args.put("input", raw);
            }
            ParsedAction p = new ParsedAction();
            p.isTool = true; p.action = tool; p.args = args;
            return p;
        }
        java.util.regex.Matcher mf = java.util.regex.Pattern.compile(
            "Final\\s*Answer\\s*:\\s*(.+)", java.util.regex.Pattern.DOTALL).matcher(t);
        if (mf.find()) {
            ParsedAction p = new ParsedAction();
            p.isFinal = true; p.answer = mf.group(1).trim();
            return p;
        }
        return null;
    }

    private static int intVal(Map<String,Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static class ParsedAction {
        boolean isTool;
        boolean isFinal;
        String action;
        Map<String,Object> args;
        String answer;
    }
}
