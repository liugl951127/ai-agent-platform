package com.platform.agent.multi;

import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.multi.bus.InMemoryBus;
import com.platform.agent.multi.orchestrator.GroupChatOrchestrator;
import com.platform.agent.multi.orchestrator.SupervisorOrchestrator;
import com.platform.agent.multi.roles.AgentFactory;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 多智能体协作 — 端到端集成测试
 * <p>
 * 场景: "写一份 AI 行业研究报告"
 * 模拟 LLM 按预期返回 plan + sub-task + final, 验证整条流水线:
 *   - Supervisor 拆 plan
 *   - 派 researcher → writer → reviewer
 *   - 最终主 Agent 输出报告
 * <p>
 * 不依赖真实 LLM, 用 mock 模拟按规则的响应
 */
class MultiAgentServiceIntegrationTest {

    @Test
    void testFullReportFlow_Supervisor() {
        LlmFeignClient mockLlm = mock(LlmFeignClient.class);
        AtomicInteger callCount = new AtomicInteger(0);

        // 模拟 LLM 按调用顺序返回预设内容
        when(mockLlm.chat(anyLong(), anyList())).thenAnswer((InvocationOnMock inv) -> {
            int n = callCount.getAndIncrement();
            return switch (n) {
                // 1. 主 Agent 拆 plan
                case 0 -> Map.of("content", "{\"plan\":[{\"to\":\"researcher\",\"task\":\"查 AI 行业 2025 规模\"},{\"to\":\"researcher\",\"task\":\"查头部公司\"},{\"to\":\"writer\",\"task\":\"整合写报告\"}],\"summary\":\"3 步完成\"}");
                // 2-3. researcher 子任务 (各 1 次 LLM)
                case 1 -> Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"2025 全球 AI 市场规模约 5000 亿美元, 年增 30%.\"}");
                case 2 -> Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"头部公司: OpenAI, Anthropic, Google DeepMind, Meta AI.\"}");
                // 4. writer
                case 3 -> Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"# AI 行业 2026 报告\\n\\n## 规模\\n5000 亿美元\\n\\n## 头部\\nOpenAI, Anthropic...\"}");
                // 5. 主 Agent 最终汇总
                case 4 -> Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"完整报告已生成, 含 3 章节, 总长 2000 字.\"}");
                default -> Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"(更多内容)\"}");
            };
        });

        // 构造 4 个 Agent (用 mock LlmFeign)
        // 用反射包一个工厂 mock 太麻烦, 直接 new
        var bus = new InMemoryBus();
        var reflector = new com.platform.agent.multi.reflection.NoOpReflection();
        var memory = new com.platform.agent.multi.memory.DefaultMemoryStore();
        var toolInvoker = mock(com.platform.agent.multi.ToolInvoker.class);
        when(toolInvoker.describe(anyList())).thenReturn("");

        LlmAgent writer = new LlmAgent("writer", "撰稿人", "你是 writer", 1L, List.of(), mockLlm, toolInvoker, reflector, memory);
        LlmAgent researcher = new LlmAgent("researcher", "研究员", "你是 researcher", 1L, List.of(), mockLlm, toolInvoker, reflector, memory);
        LlmAgent reviewer = new LlmAgent("reviewer", "审稿人", "你是 reviewer", 1L, List.of(), mockLlm, toolInvoker, reflector, memory);
        LlmAgent coder = new LlmAgent("coder", "工程师", "你是 coder", 1L, List.of(), mockLlm, toolInvoker, reflector, memory);

        SupervisorOrchestrator sup = new SupervisorOrchestrator(bus);
        AgentContext ctx = AgentContext.builder()
            .sessionId("demo-1")
            .userInput("写一份 2026 AI 行业研究报告")
            .maxSteps(3)
            .build();
        AgentResponse resp = sup.run(writer, List.of(researcher, reviewer, coder), ctx);

        // 验证
        assertNotNull(resp);
        assertTrue(resp.getContent().length() > 0, "应输出报告");
        assertTrue(resp.getThought().contains("researcher"), "应派给 researcher");
        assertTrue(resp.getThought().contains("writer"), "应派给 writer");
        // 至少 4 次 LLM 调用 (plan + 2 researcher + writer + final, 实际可能少 1)
        assertTrue(callCount.get() >= 4, "至少调 4 次 LLM, 实际: " + callCount.get());
    }

    @Test
    void testGroupChatFlow() {
        LlmFeignClient mockLlm = mock(LlmFeignClient.class);
        AtomicInteger callCount = new AtomicInteger(0);

        when(mockLlm.chat(anyLong(), anyList())).thenAnswer((InvocationOnMock inv) -> {
            int n = callCount.getAndIncrement();
            return switch (n) {
                case 0 -> Map.of("content", "NEXT: writer | 请帮我写报告");
                case 1 -> Map.of("content", "FINAL: 报告写完啦");
                default -> Map.of("content", "FINAL: 兜底");
            };
        });

        var bus = new InMemoryBus();
        var reflector = new com.platform.agent.multi.reflection.NoOpReflection();
        var memory = new com.platform.agent.multi.memory.DefaultMemoryStore();
        var toolInvoker = mock(com.platform.agent.multi.ToolInvoker.class);
        when(toolInvoker.describe(anyList())).thenReturn("");

        LlmAgent writer = new LlmAgent("writer", "撰稿", "你是 writer", 1L, List.of(), mockLlm, toolInvoker, reflector, memory);
        LlmAgent researcher = new LlmAgent("researcher", "研究", "你是 researcher", 1L, List.of(), mockLlm, toolInvoker, reflector, memory);

        GroupChatOrchestrator chat = new GroupChatOrchestrator(bus);
        AgentContext ctx = AgentContext.builder()
            .sessionId("gc-1")
            .userInput("开始讨论")
            .maxSteps(3)
            .build();
        AgentResponse resp = chat.run(researcher, List.of(researcher, writer), ctx);

        assertTrue(resp.getContent().contains("报告写完啦"));
        assertEquals("ok", resp.getStopReason());
    }

    @Test
    void testToolCallInMultiAgent() {
        // 验证: Agent 在多智能体流程中可以调工具
        LlmFeignClient mockLlm = mock(LlmFeignClient.class);
        AtomicInteger n = new AtomicInteger(0);
        when(mockLlm.chat(anyLong(), anyList())).thenAnswer(inv -> {
            int k = n.getAndIncrement();
            return switch (k) {
                case 0 -> Map.of("content", "{\"action\":\"calculator\",\"args\":{\"expression\":\"2+3\"}}");
                case 1 -> Map.of("content", "{\"action\":\"final_answer\",\"answer\":\"=5\"}");
                default -> Map.of("content", "FINAL: done");
            };
        });

        var toolInvoker = mock(com.platform.agent.multi.ToolInvoker.class);
        when(toolInvoker.invoke(eq("calculator"), anyMap())).thenReturn(5.0);
        when(toolInvoker.describe(anyList())).thenReturn("");

        LlmAgent coder = new LlmAgent("coder", "x", "x", 1L, List.of("calculator"),
            mockLlm, toolInvoker, new com.platform.agent.multi.reflection.NoOpReflection(),
            new com.platform.agent.multi.memory.DefaultMemoryStore());

        AgentContext ctx = AgentContext.builder().sessionId("t1").userInput("算 2+3").maxSteps(3).build();
        AgentResponse resp = coder.handle(ctx);

        assertEquals(1, resp.getActions().size());
        assertEquals("calculator", resp.getActions().get(0).getName());
        assertEquals(5.0, resp.getActions().get(0).getOutput());
        assertTrue(resp.getContent().contains("=5"));
        verify(toolInvoker, times(1)).invoke("calculator", Map.of("expression", "2+3"));
    }
}
