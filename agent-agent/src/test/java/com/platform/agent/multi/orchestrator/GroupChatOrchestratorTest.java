package com.platform.agent.multi.orchestrator;

import com.platform.agent.multi.*;
import com.platform.agent.multi.bus.InMemoryBus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GroupChat 编排器测试
 */
class GroupChatOrchestratorTest {

    @Test
    void testFinalAnswer() {
        Agent a = new Stub("a", List.of(Map.of("content", "FINAL: 大家都同意")));
        GroupChatOrchestrator orc = new GroupChatOrchestrator(new InMemoryBus());
        AgentContext ctx = AgentContext.builder()
            .sessionId("s").userInput("讨论一下").maxSteps(3).build();
        AgentResponse r = orc.run(a, List.of(a), ctx);
        assertTrue(r.getContent().contains("同意"));
        assertEquals("ok", r.getStopReason());
    }

    @Test
    void testNextRouting() {
        Agent a = new Stub("a", List.of(Map.of("content", "NEXT: b | 请继续分析")));
        Agent b = new Stub("b", List.of(Map.of("content", "FINAL: 分析完毕")));
        GroupChatOrchestrator orc = new GroupChatOrchestrator(new InMemoryBus());
        AgentContext ctx = AgentContext.builder()
            .sessionId("s").userInput("start").maxSteps(3).build();
        AgentResponse r = orc.run(a, List.of(a, b), ctx);
        assertTrue(r.getContent().contains("分析完毕"));
    }

    @Test
    void testUnknownAgent() {
        Agent a = new Stub("a", List.of(Map.of("content", "NEXT: ghost | x")));
        GroupChatOrchestrator orc = new GroupChatOrchestrator(new InMemoryBus());
        AgentContext ctx = AgentContext.builder()
            .sessionId("s").userInput("x").maxSteps(3).build();
        AgentResponse r = orc.run(a, List.of(a), ctx);
        assertTrue(r.getContent().contains("未知角色"));
    }

    @Test
    void testPlainText() {
        // 没有 FINAL/NEXT, 当 final
        Agent a = new Stub("a", List.of(Map.of("content", "直接给出结论")));
        GroupChatOrchestrator orc = new GroupChatOrchestrator(new InMemoryBus());
        AgentContext ctx = AgentContext.builder()
            .sessionId("s").userInput("x").maxSteps(3).build();
        AgentResponse r = orc.run(a, List.of(a), ctx);
        assertEquals("直接给出结论", r.getContent());
    }

    @Test
    void testMode() {
        GroupChatOrchestrator orc = new GroupChatOrchestrator(new InMemoryBus());
        assertEquals("group_chat", orc.mode());
    }

    static class Stub implements Agent {
        private final String n;
        private final List<Map<String,Object>> res;
        private int idx = 0;
        Stub(String n, List<Map<String,Object>> r) { this.n = n; this.res = r; }
        @Override public String name() { return n; }
        @Override public String role() { return n; }
        @Override public AgentResponse handle(AgentContext ctx) {
            if (idx >= res.size()) return AgentResponse.builder().content("").build();
            return AgentResponse.builder().content(String.valueOf(res.get(idx++).get("content"))).build();
        }
    }
}
