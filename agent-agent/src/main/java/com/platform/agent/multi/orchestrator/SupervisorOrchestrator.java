package com.platform.agent.multi.orchestrator;

import cn.hutool.json.JSONUtil;
import com.platform.agent.multi.*;
import com.platform.agent.multi.bus.MessageBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Supervisor 模式 — 主 Agent 派活给子 Agent
 * <p>
 * 算法:
 *   1. 用户输入 → 主 Agent
 *   2. 主 Agent 输出 plan: [{to:"researcher", task:"..."}, {to:"writer", task:"..."}]
 *   3. 解析 plan, 串行/并行调子 Agent (本实现串行, 简单可预期)
 *   4. 把子 Agent 结果累加到主 Agent 的 vars
 *   5. 主 Agent 拿全部结果, 写最终回答
 * <p>
 * 适用场景: 任务可拆解, 需要专家协作
 */
@Slf4j
@Component
public class SupervisorOrchestrator implements Orchestrator {

    private final MessageBus bus;

    public SupervisorOrchestrator(MessageBus bus) { this.bus = bus; }

    @Override public String mode() { return "supervisor"; }

    @Override
    public AgentResponse run(Agent primary, List<Agent> team, AgentContext ctx) {
        long t0 = System.currentTimeMillis();
        Map<String,Agent> teamMap = new HashMap<>();
        for (Agent a : team) teamMap.put(a.name(), a);

        // 1. 主 Agent 拆 plan
        String planPrompt = """
            你是 %s, 角色: %s
            你的团队成员:
            %s
            用户需求: %s
            请把任务拆成可并行/串行执行的子任务, 严格按 JSON 输出 (不要其他内容):
            {
              "plan": [
                {"to": "<agent 名>", "task": "具体子任务", "deps": []},
                ...
              ],
              "summary": "整体规划思路"
            }
            """.formatted(primary.name(), primary.role(),
                describeTeam(team),
                ctx.getUserInput());

        AgentContext planCtx = AgentContext.builder()
            .userInput(planPrompt)
            .maxSteps(3)
            .sessionId(ctx.getSessionId())
            .build();
        AgentResponse planResp = primary.handle(planCtx);
        log.info("Supervisor.plan: {}", planResp.getContent());

        // 2. 解析 plan
        Plan p = parsePlan(planResp.getContent());
        if (p == null || p.plan.isEmpty()) {
            // 解析失败, 直接让主 Agent 答
            return primary.handle(ctx);
        }

        // 3. 依次执行 (简单串行)
        Map<String,Object> subResults = new LinkedHashMap<>();
        StringBuilder trace = new StringBuilder("Plan: ").append(planResp.getContent()).append("\n");
        for (PlanStep step : p.plan) {
            Agent sub = teamMap.get(step.to);
            if (sub == null) {
                trace.append("\n[skip] unknown agent: ").append(step.to);
                continue;
            }
            trace.append("\n[dispatch] → ").append(step.to).append(": ").append(step.task);
            AgentContext subCtx = AgentContext.builder()
                .userInput(step.task)
                .vars(new HashMap<>(subResults))
                .sessionId(ctx.getSessionId())
                .history(ctx.getHistory())
                .maxSteps(ctx.getMaxSteps())
                .build();
            AgentResponse subResp = sub.handle(subCtx);
            subResults.put(step.to + ":" + (subResults.size() / Math.max(1, teamMap.size()) + 1), subResp.getContent());
            subResults.put(step.to + ":last", subResp.getContent());
            trace.append("\n[result] ← ").append(step.to).append(": ").append(truncate(subResp.getContent(), 200));
        }

        // 4. 主 Agent 汇总
        String finalPrompt = """
            你是 %s. 你的团队已完成所有子任务, 请综合输出最终答案:
            用户需求: %s

            子任务结果:
            %s

            请给出完整、有条理的最终回答.
            """.formatted(primary.name(), ctx.getUserInput(), formatSubResults(subResults));

        AgentContext finalCtx = AgentContext.builder()
            .userInput(finalPrompt)
            .vars(ctx.getVars())
            .sessionId(ctx.getSessionId())
            .history(ctx.getHistory())
            .maxSteps(ctx.getMaxSteps())
            .build();
        AgentResponse finalResp = primary.handle(finalCtx);
        return AgentResponse.builder()
            .content(finalResp.getContent())
            .thought(trace + "\n[final] " + finalResp.getThought())
            .actions(finalResp.getActions())
            .artifacts(Map.of("plan", p, "subResults", subResults))
            .promptTokens(planResp.getPromptTokens() + finalResp.getPromptTokens())
            .completionTokens(planResp.getCompletionTokens() + finalResp.getCompletionTokens())
            .elapsedMs(System.currentTimeMillis() - t0)
            .stopReason(finalResp.getStopReason())
            .build();
    }

    private String describeTeam(List<Agent> team) {
        return team.stream()
            .map(a -> "- " + a.name() + " (" + a.role() + ")")
            .reduce((x,y) -> x + "\n" + y).orElse("");
    }

    private Plan parsePlan(String text) {
        if (text == null) return null;
        int a = text.indexOf('{'), b = text.lastIndexOf('}');
        if (a < 0 || b < 0) return null;
        try {
            Map<?,?> m = JSONUtil.parseObj(text.substring(a, b + 1));
            List<?> raw = (List<?>) m.get("plan");
            if (raw == null) return null;
            List<PlanStep> steps = new ArrayList<>();
            for (Object o : raw) {
                Map<?,?> r = (Map<?,?>) o;
                PlanStep s = new PlanStep();
                s.to = String.valueOf(r.get("to"));
                s.task = String.valueOf(r.get("task"));
                steps.add(s);
            }
            Plan p = new Plan();
            p.plan = steps;
            p.summary = String.valueOf(m.get("summary"));
            return p;
        } catch (Exception e) {
            log.warn("plan 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String formatSubResults(Map<String,Object> results) {
        StringBuilder sb = new StringBuilder();
        results.forEach((k, v) -> sb.append("## ").append(k).append("\n").append(v).append("\n\n"));
        return sb.toString();
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }

    static class Plan { List<PlanStep> plan; String summary; }
    static class PlanStep { String to; String task; List<String> deps; }
}
