package com.platform.agent.multi;

import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.multi.memory.DefaultMemoryStore;
import com.platform.agent.multi.reflection.NoOpReflection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LlmAgent 单元测试 — mock LlmFeignClient, 验证 ReAct 循环
 */
@ExtendWith(MockitoExtension.class)
class LlmAgentTest {

    @Mock LlmFeignClient llmFeign;
    @Mock ToolInvoker toolInvoker;

    LlmAgent agent;
    DefaultMemoryStore memory;

    @BeforeEach
    void setUp() {
        memory = new DefaultMemoryStore();
        agent = new LlmAgent(
            "tester", "测试智能体",
            "你是一个测试 Agent",
            1L, List.of("calculator"),
            llmFeign, toolInvoker, new NoOpReflection(), memory
        );
    }

    @Test
    void testSimpleAnswer() {
        // LLM 直接给出 final
        when(llmFeign.chat(anyLong(), anyList())).thenReturn(Map.of(
            "content", "这是答案: 42",
            "promptTokens", 10, "completionTokens", 5
        ));

        AgentContext ctx = AgentContext.builder()
            .sessionId("s1").userInput("1+1等于几?")
            .maxSteps(5).build();
        AgentResponse resp = agent.handle(ctx);

        assertEquals("ok", resp.getStopReason());
        assertTrue(resp.getContent().contains("42"));
        assertEquals(10, resp.getPromptTokens());
    }

    @Test
    void testToolCallThenAnswer() {
        // 第 1 步: LLM 决定调工具
        when(llmFeign.chat(anyLong(), anyList()))
            .thenReturn(Map.of("content", "{\"action\":\"calculator\",\"args\":{\"expression\":\"2+3\"}}", "promptTokens", 20, "completionTokens", 8))
            // 第 2 步: 拿到 Observation 后 LLM 给出 final
            .thenReturn(Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"2+3=5\"}", "promptTokens", 30, "completionTokens", 10));

        when(toolInvoker.invoke(eq("calculator"), anyMap())).thenReturn(5.0);
        when(toolInvoker.describe(anyList())).thenReturn("- calculator: 算术\n  参数: {...}");

        AgentContext ctx = AgentContext.builder()
            .sessionId("s2").userInput("算 2+3").maxSteps(5).build();
        AgentResponse resp = agent.handle(ctx);

        assertEquals(1, resp.getActions().size());
        assertEquals("calculator", resp.getActions().get(0).getName());
        assertTrue(resp.getActions().get(0).isOk());
        assertEquals(5.0, resp.getActions().get(0).getOutput());
        assertTrue(resp.getContent().contains("2+3=5"));
        verify(toolInvoker, times(1)).invoke("calculator", Map.of("expression", "2+3"));
    }

    @Test
    void testMaxStepsReached() {
        // LLM 一直调工具, 永远不给 final
        when(llmFeign.chat(anyLong(), anyList()))
            .thenReturn(Map.of("content", "{\"action\":\"calculator\",\"args\":{\"expression\":\"1\"}}"));
        when(toolInvoker.invoke(anyString(), anyMap())).thenReturn(1.0);
        when(toolInvoker.describe(anyList())).thenReturn("");

        AgentContext ctx = AgentContext.builder()
            .sessionId("s3").userInput("loop").maxSteps(3).build();
        AgentResponse resp = agent.handle(ctx);

        assertEquals("max_steps", resp.getStopReason());
        assertEquals(3, resp.getActions().size());
    }

    @Test
    void testToolError() {
        when(llmFeign.chat(anyLong(), anyList()))
            .thenReturn(Map.of("content", "{\"action\":\"bad_tool\",\"args\":{}}"))
            .thenReturn(Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"好的\"}"));
        when(toolInvoker.invoke(eq("bad_tool"), anyMap())).thenThrow(new RuntimeException("找不到工具"));
        when(toolInvoker.describe(anyList())).thenReturn("");

        AgentContext ctx = AgentContext.builder()
            .sessionId("s4").userInput("test").maxSteps(3).build();
        AgentResponse resp = agent.handle(ctx);

        assertEquals(1, resp.getActions().size());
        assertFalse(resp.getActions().get(0).isOk());
        assertTrue(resp.getContent().contains("好的"));
    }

    @Test
    void testThoughtActionStyle() {
        // 风格 2: Thought/Action 风格
        when(llmFeign.chat(anyLong(), anyList()))
            .thenReturn(Map.of("content",
                "Thought: 需要算 5*5\nAction: calculator({\"expression\":\"5*5\"})\nObservation: 25\nThought: 答完了\nFinal Answer: 25"))
            .thenReturn(Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"25\"}"));
        when(toolInvoker.invoke(eq("calculator"), anyMap())).thenReturn(25.0);
        when(toolInvoker.describe(anyList())).thenReturn("");

        AgentContext ctx = AgentContext.builder()
            .sessionId("s5").userInput("5*5?").maxSteps(3).build();
        AgentResponse resp = agent.handle(ctx);

        assertTrue(resp.getContent().contains("25"));
    }

    @Test
    void testMemoryWrite() {
        when(llmFeign.chat(anyLong(), anyList())).thenReturn(Map.of("content", "x"));
        AgentContext ctx = AgentContext.builder()
            .sessionId("mem-1").userInput("Q1").maxSteps(3).build();
        agent.handle(ctx);
        // 验证 memory 写了
        var recent = memory.recent("mem-1", 10);
        assertEquals(2, recent.size());
        assertEquals("Q1", recent.get(0).get("content"));
    }
}
