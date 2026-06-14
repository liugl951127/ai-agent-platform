package com.platform.agent.service;

import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.platform.agent.entity.AgentInfo;
import com.platform.agent.entity.AgentTool;
import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.mapper.AgentInfoMapper;
import com.platform.agent.mapper.AgentToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * ReAct 推理执行器 — 通过 Feign 远程调用 agent-llm
 * <p>
 * 之前的版本直接 import com.platform.llm.service.LlmRouter,导致 agent-agent
 * 单跑时强依赖 agent-llm 编译产物。改为 Feign 后,即使 agent-llm 进程没起,
 * 本服务也能起来(只是 /agent/chat 调用时会失败)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactExecutor {

    private final AgentInfoMapper agentMapper;
    private final AgentToolMapper toolMapper;
    private final LlmFeignClient llmFeign;

    @SentinelResource(
        value = "agent:react:run",
        blockHandler = "runBlockHandler",
        fallback = "runFallback"
    )
    public String run(Long agentId, String userInput) {
        AgentInfo agent = agentMapper.selectById(agentId);
        // 1. RAG (HTTP 调 agent-knowledge)
        String context = "";
        if (agent.getKnowledgeId() != null) {
            try {
                RestTemplate rt = new RestTemplate();
                Map<?,?> r = rt.postForObject(
                    "http://agent-knowledge/knowledge/search",
                    Map.of("kbId", agent.getKnowledgeId(), "q", userInput, "topK", 3),
                    Map.class);
                context = String.valueOf(r.get("data"));
            } catch (Exception e) { log.warn("RAG 失败: {}", e.getMessage()); }
        }
        // 2. 工具描述
        String toolsJson = "[]";
        if (agent.getToolIds() != null && !agent.getToolIds().isEmpty()) {
            List<Long> ids = Arrays.stream(agent.getToolIds().split(","))
                .map(Long::parseLong).toList();
            List<Map<String,Object>> tools = new ArrayList<>();
            for (AgentTool t : toolMapper.selectBatchIds(ids)) {
                tools.add(Map.of(
                    "name", t.getCode(),
                    "description", t.getDescription(),
                    "parameters", JSONUtil.parse(t.getParamSchema())));
            }
            toolsJson = JSONUtil.toJsonStr(tools);
        }
        // 3. Prompt
        String prompt = """
            你是一个智能体,可以使用以下工具: %s
            相关背景知识: %s
            请用 Thought/Action/Observation/Final Answer 方式推理;
            若需要调用工具,严格输出 JSON: {"action":"<tool>","args":{...}}
            用户问题: %s
            """.formatted(toolsJson, context, userInput);

        List<Map<String,Object>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content", agent.getSystemPrompt() == null ? "" : agent.getSystemPrompt()));
        messages.add(Map.of("role","user","content", prompt));

        // 4. ReAct loop
        for (int i = 0; i < 5; i++) {
            String reply;
            try {
                // Feign 远程调 agent-llm
                Map<String, Object> resp = llmFeign.chat(agent.getModelId(), messages);
                reply = String.valueOf(resp.get("data"));
            } catch (Exception e) {
                log.error("调 LLM 失败: agentId={}, err={}", agentId, e.getMessage());
                return "LLM 服务不可用: " + e.getMessage();
            }
            if (reply != null && reply.contains("\"action\"")) {
                Map<?,?> act = JSONUtil.parse(reply).toBean(Map.class);
                String tool = String.valueOf(act.get("action"));
                Map<?,?> args = (Map<?,?>) act.get("args");
                String observation = invokeTool(tool, args);
                messages.add(Map.of("role","assistant","content", reply));
                messages.add(Map.of("role","user","content","Observation: " + observation));
            } else {
                return reply;
            }
        }
        return "推理超出最大步数";
    }

    public String runBlockHandler(Long agentId, String userInput, BlockException e) {
        log.warn("智能体对话被限流: agentId={}", agentId);
        return "⚠️ 智能体服务繁忙,请稍后再试";
    }
    public String runFallback(Long agentId, String userInput, Throwable e) {
        log.error("智能体执行异常: agentId={}, err={}", agentId, e.getMessage());
        return "智能体执行失败: " + e.getMessage();
    }

    private String invokeTool(String code, Map<?,?> args) {
        AgentTool tool = toolMapper.selectOne(
            new QueryWrapper<AgentTool>().eq("code", code));
        if (tool == null) return "tool not found";
        try {
            String beanName = tool.getHandler();
            Object bean = SpringContextHolder.getBean(beanName);
            return SpringContextHolder.invoke(bean, "execute", args);
        } catch (Exception e) {
            return "tool error: " + e.getMessage();
        }
    }
}
