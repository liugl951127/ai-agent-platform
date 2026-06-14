package com.platform.agent.multi.plan;

import cn.hutool.json.JSONUtil;
import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.multi.*;
import com.platform.agent.multi.bus.MessageBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Plan-and-Execute 模式 — 复杂任务先拆解再执行
 * <p>
 * 算法:
 *   1. Planner 拆子任务 [{id, task, deps, expected_output}, ...]
 *   2. 按依赖顺序执行 (DAG 拓扑序, 简单版串行)
 *   3. 每步用对应 Agent 执行, 结果塞回 vars
 *   4. Reflector 检查整体完成度, 必要时追加补充任务
 * <p>
 * 适用: 复杂多步任务, 比如"调研 + 写报告 + 翻译"这种
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanAndExecute {

    private final LlmFeignClient llmFeign;
    private final MessageBus bus;

    public AgentResponse run(Agent primary, List<Agent> team, AgentContext ctx, Long plannerModelId) {
        long t0 = System.currentTimeMillis();
        Map<String, Agent> teamMap = new HashMap<>();
        for (Agent a : team) teamMap.put(a.name(), a);

        // 1. Plan
        String planPrompt = """
            你是 %s. 你的团队:
            %s
            用户目标: %s

            请拆解为可执行子任务, 严格按 JSON 输出:
            {
              "steps": [
                {"id": 1, "agent": "<角色名>", "task": "具体子任务", "deps": []},
                ...
              ],
              "rationale": "拆分理由"
            }
            """.formatted(primary.name(),
                team.stream().map(a -> "- " + a.name()).reduce((x,y) -> x + "\n" + y).orElse(""),
                ctx.getUserInput());
        List<Map<String,Object>> planMsgs = List.of(
            Map.of("role", "system", "content", "你是任务规划师, 输出严格的 JSON."),
            Map.of("role", "user", "content", planPrompt)
        );
        String planText = "";
        try {
            Map<String,Object> r = llmFeign.chat(plannerModelId, planMsgs);
            planText = String.valueOf(r.get("content"));
        } catch (Exception e) {
            log.warn("Plan 阶段 LLM 失败: {}", e.getMessage());
        }
        List<PlanStep> steps = parseSteps(planText);
        if (steps.isEmpty()) {
            return primary.handle(ctx);
        }
        log.info("Plan 拆出 {} 个子任务", steps.size());

        // 2. 拓扑执行
        Map<String, String> stepResults = new LinkedHashMap<>();
        StringBuilder trace = new StringBuilder("Plan: ").append(planText).append("\n");
        int totalPrompt = 0, totalCompletion = 0;
        for (PlanStep s : steps) {
            Agent sub = teamMap.get(s.agent);
            if (sub == null) {
                trace.append("\n[skip] unknown agent: ").append(s.agent);
                continue;
            }
            trace.append("\n[step ").append(s.id).append("] → ").append(s.agent).append(": ").append(s.task);
            AgentContext subCtx = AgentContext.builder()
                .userInput(s.task + "\n\n历史步骤结果:\n" + summarizePrev(stepResults))
                .vars(new HashMap<>(ctx.getVars()))
                .history(ctx.getHistory())
                .sessionId(ctx.getSessionId())
                .maxSteps(ctx.getMaxSteps())
                .build();
            AgentResponse subResp = sub.handle(subCtx);
            stepResults.put("step-" + s.id, subResp.getContent());
            totalPrompt += subResp.getPromptTokens();
            totalCompletion += subResp.getCompletionTokens();
            trace.append("\n[result] ").append(truncate(subResp.getContent(), 200));
        }

        // 3. 主 Agent 汇总
        String finalPrompt = "你是 %s. 用户目标: %s\n\n所有子任务已完成:\n%s\n\n请输出最终交付物."
            .formatted(primary.name(), ctx.getUserInput(), formatAll(stepResults));
        AgentContext finalCtx = AgentContext.builder()
            .userInput(finalPrompt)
            .vars(ctx.getVars())
            .history(ctx.getHistory())
            .sessionId(ctx.getSessionId())
            .build();
        AgentResponse finalResp = primary.handle(finalCtx);
        return AgentResponse.builder()
            .content(finalResp.getContent())
            .thought(trace + "\n[final] " + finalResp.getThought())
            .actions(finalResp.getActions())
            .artifacts(Map.of("steps", steps, "stepResults", stepResults))
            .promptTokens(totalPrompt + finalResp.getPromptTokens())
            .completionTokens(totalCompletion + finalResp.getCompletionTokens())
            .elapsedMs(System.currentTimeMillis() - t0)
            .build();
    }

    private List<PlanStep> parseSteps(String text) {
        if (text == null) return List.of();
        int a = text.indexOf('{'), b = text.lastIndexOf('}');
        if (a < 0 || b < 0) return List.of();
        try {
            Map<?,?> m = JSONUtil.parseObj(text.substring(a, b + 1));
            List<?> raw = (List<?>) m.get("steps");
            if (raw == null) return List.of();
            List<PlanStep> out = new ArrayList<>();
            for (Object o : raw) {
                Map<?,?> r = (Map<?,?>) o;
                PlanStep s = new PlanStep();
                s.id = ((Number) r.get("id")).intValue();
                s.agent = String.valueOf(r.get("agent"));
                s.task = String.valueOf(r.get("task"));
                out.add(s);
            }
            return out;
        } catch (Exception e) {
            log.warn("Plan 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private String summarizePrev(Map<String, String> results) {
        if (results.isEmpty()) return "(无)";
        StringBuilder sb = new StringBuilder();
        results.forEach((k, v) -> sb.append("## ").append(k).append("\n").append(v).append("\n\n"));
        return sb.toString();
    }

    private String formatAll(Map<String, String> results) {
        StringBuilder sb = new StringBuilder();
        results.forEach((k, v) -> sb.append("## ").append(k).append("\n").append(v).append("\n\n"));
        return sb.toString();
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }

    static class PlanStep {
        int id;
        String agent;
        String task;
        List<String> deps;
    }
}
