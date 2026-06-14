package com.platform.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.platform.agent.entity.AgentInfo;
import com.platform.agent.entity.AgentTool;
import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.mapper.AgentInfoMapper;
import com.platform.agent.mapper.AgentToolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReactExecutor 单元测试 — 使用 LlmFeignClient mock
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactExecutor 单元测试")
class ReactExecutorTest {

    @Mock AgentInfoMapper agentMapper;
    @Mock AgentToolMapper toolMapper;
    @Mock LlmFeignClient llmFeign;

    @InjectMocks ReactExecutor executor;

    @BeforeEach
    void setUp() {}

    @Test
    @DisplayName("无工具,LLM 直接给答案")
    void simpleChat() {
        AgentInfo agent = new AgentInfo();
        agent.setId(1L);
        agent.setModelId(1L);
        agent.setSystemPrompt("你是助手");
        when(agentMapper.selectById(1L)).thenReturn(agent);
        Map<String, Object> llmResp = new HashMap<>();
        llmResp.put("data", "你好,我是 AI 助手");
        when(llmFeign.chat(eq(1L), anyList())).thenReturn(llmResp);

        String reply = executor.run(1L, "你好");
        assertEquals("你好,我是 AI 助手", reply);
    }

    @Test
    @DisplayName("LLM 调用失败,返回错误信息(不抛异常)")
    void llmFailureHandled() {
        AgentInfo agent = new AgentInfo();
        agent.setId(2L);
        agent.setModelId(1L);
        when(agentMapper.selectById(2L)).thenReturn(agent);
        when(llmFeign.chat(eq(1L), anyList())).thenThrow(new RuntimeException("connection refused"));

        String reply = executor.run(2L, "hi");
        assertNotNull(reply);
        assertTrue(reply.contains("LLM 服务不可用") || reply.contains("connection refused"));
    }
}
