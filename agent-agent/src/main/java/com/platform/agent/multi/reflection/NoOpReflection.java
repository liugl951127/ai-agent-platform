package com.platform.agent.multi.reflection;

import com.platform.agent.multi.Agent;
import com.platform.agent.multi.AgentContext;
import com.platform.agent.multi.AgentResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认不反思 — 节省 token, 适合大多数场景
 * <p>
 * 通过 @ConditionalOnMissingBean 让位给 LLMSelfCritiqueReflection (如果配了)
 */
@Component
@ConditionalOnMissingBean(ReflectionEngine.class)
public class NoOpReflection implements ReflectionEngine {
    @Override
    public AgentResponse reflect(Agent agent, AgentContext ctx, AgentResponse resp) {
        return resp;
    }
}
