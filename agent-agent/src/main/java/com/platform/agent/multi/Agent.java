package com.platform.agent.multi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent 能力单元 — 任何能"感知 → 思考 → 行动"的实体
 * <p>
 * 三种实现:
 *   - LlmAgent        (大模型 + 工具)   — 最常用
 *   - HttpAgent       (调远端服务, 跨进程子 Agent)
 *   - CompositeAgent  (组合多个 Agent, Supervisor 内部用)
 * <p>
 * 设计原则:
 *   - 单一职责: 一个 Agent 只做一件事 (Researcher / Coder / Reviewer)
 *   - 可观察: 每次 handle 输出含 thought / actions / result, 方便调试
 *   - 可中断: ctx 里有 maxSteps / timeout 限制
 */
public interface Agent {

    /** Agent 唯一名 (Orchestrator / Bus 寻址) */
    String name();

    /** 角色描述 (给其它 Agent / 用户看) */
    String role();

    /**
     * 处理一条消息, 返回结果
     */
    AgentResponse handle(AgentContext ctx);
}
