package com.platform.agent.multi.reflection;

import com.platform.agent.multi.Agent;
import com.platform.agent.multi.AgentContext;
import com.platform.agent.multi.AgentResponse;

/**
 * 反思引擎 — Agent 答完后自检: "答案够好吗? 需要改吗?"
 * <p>
 * 两种策略:
 *   - NoOpReflection      不反思 (默认, 节省 token)
 *   - LLMSelfCritique     再问 LLM "这段回答是否满足 X 标准? 给改进建议"
 * <p>
 * 最多反思 2 轮, 避免无限循环
 */
public interface ReflectionEngine {

    /**
     * 对 Agent 的回答做反思, 返回可能改进后的回答
     * @param agent   产生回答的 Agent (用于调 LLM)
     * @param ctx     原始上下文
     * @param resp    Agent 的初版回答
     * @return 改进后的回答 (或原回答, 决定不反思)
     */
    AgentResponse reflect(Agent agent, AgentContext ctx, AgentResponse resp);
}
