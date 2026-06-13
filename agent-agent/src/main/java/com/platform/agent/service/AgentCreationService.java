package com.platform.agent.service;

import com.platform.agent.entity.AgentInfo;
import com.platform.agent.feign.LlmModelClient;
import com.platform.agent.mapper.AgentInfoMapper;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

/**
 * 智能体创建服务 - 跨服务分布式事务示例
 * <p>
 * 流程:
 *   1) 本服务 (agent-agent) 写 agent_info
 *   2) Feign 调 agent-llm 校验模型存在
 *   3) HTTP 调 agent-knowledge 初始化知识库
 * 任意步骤失败 → @GlobalTransactional 自动回滚所有分支
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCreationService {

    private final AgentInfoMapper agentMapper;
    private final LlmModelClient llmClient;

    @GlobalTransactional(name = "agent-platform-tx-group", rollbackFor = Exception.class)
    public Long createAgentWithConfig(AgentInfo agent) {
        // step 1: 本服务写入
        agentMapper.insert(agent);
        log.info("TX step1: agent_info 写入 id={}", agent.getId());

        // step 2: 跨服务校验 (Feign)
        Map<String, Object> modelResp = llmClient.detail(agent.getModelId());
        Object data = modelResp.get("data");
        if (data == null) {
            throw new RuntimeException("模型不存在: " + agent.getModelId());
        }
        log.info("TX step2: 模型校验通过 modelId={}", agent.getModelId());

        // step 3: 跨服务初始化 (HTTP)
        try {
            RestTemplate rt = new RestTemplate();
            Map<String,Object> resp = rt.postForObject(
                "http://agent-knowledge/knowledge/init",
                Map.of("agentId", agent.getId(), "name", agent.getName() + "-kb"),
                Map.class);
            log.info("TX step3: knowledge init resp={}", resp);
        } catch (Exception e) {
            log.error("TX step3 失败, 触发回滚: {}", e.getMessage());
            throw new RuntimeException("知识库初始化失败,事务回滚", e);
        }

        return agent.getId();
    }
}
