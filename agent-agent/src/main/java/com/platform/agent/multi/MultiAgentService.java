package com.platform.agent.multi;

import com.platform.agent.multi.orchestrator.Orchestrator;
import com.platform.agent.multi.orchestrator.SupervisorOrchestrator;
import com.platform.agent.multi.orchestrator.GroupChatOrchestrator;
import com.platform.agent.multi.roles.AgentFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 多智能体统一入口
 * <p>
 * 业务方调用: multiAgent.run(mode, "session-001", "写一份AI行业研究报告")
 * 内部:
 *   - mode=supervisor: Supervisor + 4 角色
 *   - mode=group_chat: GroupChat + 4 角色
 *   - mode=single:     单个 Agent (默认 researcher)
 * <p>
 * Demo 场景: "写一份 2026 AI 行业研究报告" — 默认走 supervisor
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiAgentService {

    private final AgentFactory factory;
    private final SupervisorOrchestrator supervisor;
    private final GroupChatOrchestrator groupChat;

    public AgentResponse run(String mode, String sessionId, String userInput) {
        log.info("multiAgent.run mode={} session={} input={}", mode, sessionId, truncate(userInput, 100));
        List<Agent> team = defaultTeam();
        Agent primary = team.get(0);  // writer 是主 (适合写报告场景)
        AgentContext ctx = AgentContext.builder()
            .sessionId(sessionId)
            .userInput(userInput)
            .maxSteps(8)
            .timeoutMs(120_000)
            .build();
        Orchestrator orc = pickOrchestrator(mode);
        return orc.run(primary, team, ctx);
    }

    public List<Agent> defaultTeam() {
        return List.of(factory.writer(), factory.researcher(), factory.reviewer(), factory.coder());
    }

    public Orchestrator pickOrchestrator(String mode) {
        if (mode == null) return supervisor;
        return switch (mode.toLowerCase()) {
            case "group_chat", "group" -> groupChat;
            case "supervisor" -> supervisor;
            default -> supervisor;
        };
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}
