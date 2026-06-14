package com.platform.agent.multi;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 上下文 — handle(AgentContext) 收到的全部信息
 * <p>
 * 字段说明:
 *   - sessionId  会话 ID (用于 Memory 关联)
 *   - userInput  本轮用户输入 (或上游 Agent 转交的任务)
 *   - history    历史消息 (Working Memory)
 *   - vars       共享变量 (RAG context / plan / 子任务结果...)
 *   - inbox      来自其它 Agent 的消息 (A2A 模式才用)
 *   - maxSteps   最大推理步数 (ReAct 循环上限)
 *   - timeoutMs  超时 (毫秒)
 *   - tools      工具列表 (override 全局, 给该 Agent 专属工具集)
 */
@Data
@Builder
public class AgentContext {
    private String sessionId;
    private String userInput;
    @Builder.Default
    private List<Map<String,Object>> history = new ArrayList<>();
    @Builder.Default
    private Map<String,Object> vars = new HashMap<>();
    @Builder.Default
    private List<AgentMessage> inbox = new ArrayList<>();
    @Builder.Default
    private int maxSteps = 8;
    @Builder.Default
    private long timeoutMs = 60_000;
    @Builder.Default
    private List<String> tools = new ArrayList<>();
}
