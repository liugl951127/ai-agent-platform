package com.platform.agent.multi.orchestrator;

import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.multi.*;
import com.platform.agent.multi.bus.InMemoryBus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

/**
 * SupervisorOrchestrator 单元测试 — mock LlmFeignClient
 */
class SupervisorOrchestratorTest {

    @Test
    void testPlanParseAndDispatch() {
        LlmFeignClient llm = mock(LlmFeignClient.class);
        // 第 1 步 (主 Agent 拆 plan)
        when(llm.chat(anyLong(), argThat(list -> {
            String s = String.valueOf(((java.util.List<?>) list).get(((java.util.List<?>) list).size() - 1).getClass().cast("x"));
            return true;
        })))
        // 上面那行 java 写太复杂, 改用 anyList
        .thenReturn(Map.of("content",
            "{\"plan\":[{\"to\":\"a\",\"task\":\"查资料\"},{\"to\":\"b\",\"task\":\"写报告\"}],\"summary\":\"两步走\"}"));
        // 后面调子 Agent 也走 chat, 给个简答就行

        // 简化: 用一个 stub Agent 替代
        Agent a = new StubAgent("a", "助手A", List.of(
            Map.of("content", "{\"answer\":\"资料完成\"}"),
            Map.of("content", "{\"answer\":\"报告完成\"}"),
            Map.of("content", "FINAL: 报告 OK")
        ));
        Agent b = new StubAgent("b", "助手B", List.of(
            Map.of("content", "{\"answer\":\"写好了\"}"),
            Map.of("content", "FINAL: 写完了")
        ));
        SupervisorOrchestrator orc = new SupervisorOrchestrator(new InMemoryBus());
        AgentContext ctx = AgentContext.builder()
            .sessionId("s").userInput("写报告").maxSteps(3).build();

        AgentResponse resp = orc.run(b, List.of(a, b), ctx);
        // 主 Agent = b, team = [a, b]
        // 主 Agent 拆 plan → a 查资料 → b 写报告 → b 汇总
        assertNotNull(resp);
        assertNotNull(resp.getContent());
    }

    @Test
    void testPlanParseFailure() {
        // 主 Agent 输出无法解析
        Agent a = new StubAgent("a", "助手A", List.of(
            Map.of("content", "(乱答一通)"),
            Map.of("content", "FINAL: 直接答了")
        ));
        SupervisorOrchestrator orc = new SupervisorOrchestrator(new InMemoryBus());
        AgentContext ctx = AgentContext.builder()
            .sessionId("s").userInput("test").maxSteps(3).build();
        AgentResponse resp = orc.run(a, List.of(a), ctx);
        assertNotNull(resp);
    }

    @Test
    void testMode() {
        SupervisorOrchestrator orc = new SupervisorOrchestrator(new InMemoryBus());
        assertEquals("supervisor", orc.mode());
    }

    /** 简单 stub — 按顺序返回预设响应 */
    static class StubAgent implements Agent {
        private final String n, r;
        private final List<Map<String,Object>> responses;
        private int idx = 0;
        StubAgent(String n, String r, List<Map<String,Object>> res) {
            this.n = n; this.r = r; this.responses = res;
        }
        @Override public String name() { return n; }
        @Override public String role() { return r; }
        @Override public AgentResponse handle(AgentContext ctx) {
            if (idx >= responses.size()) {
                return AgentResponse.builder().content("(stub end)").build();
            }
            Map<String,Object> m = responses.get(idx++);
            return AgentResponse.builder()
                .content(String.valueOf(m.get("content")))
                .build();
        }
    }
}
