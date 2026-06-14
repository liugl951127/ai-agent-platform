package com.platform.agent.multi.orchestrator;

import com.platform.agent.multi.Agent;
import com.platform.agent.multi.AgentContext;
import com.platform.agent.multi.AgentResponse;

/**
 * 编排器接口 — 决定多个 Agent 如何协作
 * <p>
 * 三种实现:
 *   - SupervisorOrchestrator  主 Agent 派活给子 Agent
 *   - GroupChatOrchestrator   群聊, 轮流发言
 *   - SequentialOrchestrator  流水线, 一个接一个
 * <p>
 * 所有 Orchestrator 都接收 Agent 列表 (含一个"主" Agent) 和 用户输入
 */
public interface Orchestrator {

    /** 模式名 */
    String mode();

    /**
     * 执行编排
     * @param primary 主 Agent (承担用户对接)
     * @param team    协作 Agent 列表
     * @param ctx     上下文
     */
    AgentResponse run(Agent primary, java.util.List<Agent> team, AgentContext ctx);
}
