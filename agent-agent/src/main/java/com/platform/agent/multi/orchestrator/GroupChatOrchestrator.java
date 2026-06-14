package com.platform.agent.multi.orchestrator;

import com.platform.agent.multi.*;
import com.platform.agent.multi.bus.MessageBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * GroupChat 模式 — 多个 Agent 轮流发言
 * <p>
 * 流程:
 *   1. 第 1 轮: 主 Agent 接用户问题, 选最合适的 Agent 接力
 *   2. 该 Agent 处理, 决定:
 *      - "FINAL: 答案" → 结束
 *      - "NEXT: agent_name | 任务" → 派给下个 Agent
 *   3. 重复直到 FINAL 或达到 maxRounds
 * <p>
 * 适用: 头脑风暴 / 讨论 / 多视角分析
 */
@Slf4j
@Component
public class GroupChatOrchestrator implements Orchestrator {

    private final MessageBus bus;

    public GroupChatOrchestrator(MessageBus bus) { this.bus = bus; }

    @Override public String mode() { return "group_chat"; }

    @Override
    public AgentResponse run(Agent primary, List<Agent> team, AgentContext ctx) {
        long t0 = System.currentTimeMillis();
        int maxRounds = 5;
        Map<String,Agent> teamMap = new LinkedHashMap<>();
        teamMap.put(primary.name(), primary);
        for (Agent a : team) teamMap.put(a.name(), a);

        StringBuilder transcript = new StringBuilder();
        Agent current = primary;
        AgentContext currentCtx = ctx;
        String lastAnswer = null;

        for (int round = 0; round < maxRounds; round++) {
            // 在 context 中告诉 Agent 它可以选 FINAL 或 NEXT
            String router = String.format("""
                可用角色: %s
                决定:
                  - 若你有最终答案, 严格输出: FINAL: <你的答案>
                  - 若需要别人接力, 严格输出: NEXT: <角色名> | <任务>
                """,
                teamMap.keySet().stream().reduce((x,y) -> x + ", " + y).orElse(""));
            currentCtx.setUserInput(currentCtx.getUserInput() + "\n\n" + router);

            AgentResponse resp = current.handle(currentCtx);
            transcript.append("\n[").append(current.name()).append(" round ").append(round).append("]: ")
                     .append(truncate(resp.getContent(), 300));

            String out = resp.getContent();
            if (out == null) break;
            if (out.contains("FINAL:")) {
                lastAnswer = out.substring(out.indexOf("FINAL:") + 6).trim();
                break;
            }
            if (out.contains("NEXT:")) {
                int idx = out.indexOf("NEXT:");
                String tail = out.substring(idx + 5).trim();
                String[] parts = tail.split("\\|", 2);
                String nextName = parts[0].trim();
                Agent next = teamMap.get(nextName);
                if (next == null) {
                    lastAnswer = "未知角色: " + nextName + ", 当前答案为: " + out;
                    break;
                }
                String nextTask = parts.length > 1 ? parts[1].trim() : "请继续";
                currentCtx = AgentContext.builder()
                    .userInput("[" + current.name() + "] " + nextTask)
                    .history(currentCtx.getHistory())
                    .vars(currentCtx.getVars())
                    .sessionId(currentCtx.getSessionId())
                    .maxSteps(currentCtx.getMaxSteps())
                    .build();
                current = next;
                continue;
            }
            // 既没 FINAL 也没 NEXT → 当 FINAL
            lastAnswer = out;
            break;
        }

        return AgentResponse.builder()
            .content(lastAnswer == null ? "(群聊未达成结论)" : lastAnswer)
            .thought(transcript.toString())
            .elapsedMs(System.currentTimeMillis() - t0)
            .stopReason(lastAnswer == null ? "max_rounds" : "ok")
            .build();
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}
