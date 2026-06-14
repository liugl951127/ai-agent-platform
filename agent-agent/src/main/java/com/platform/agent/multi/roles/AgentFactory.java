package com.platform.agent.multi.roles;

import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.multi.LlmAgent;
import com.platform.agent.multi.ToolInvoker;
import com.platform.agent.multi.memory.MemoryStore;
import com.platform.agent.multi.reflection.ReflectionEngine;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色工厂 — 创建内置 4 个 Agent
 * <p>
 * 角色设计 (写报告场景):
 *   - Researcher  联网查资料, 给 Writer 提供事实
 *   - Writer      把 Research 整理成文
 *   - Reviewer    审稿, 指出问题让 Writer 改
 *   - Coder       (可选) 写代码 / 跑 SQL
 */
@Component
public class AgentFactory {

    private final LlmFeignClient llmFeign;
    private final ToolInvoker toolInvoker;
    private final ReflectionEngine reflector;
    private final MemoryStore memory;
    private final Long defaultModelId;

    public AgentFactory(LlmFeignClient llmFeign, ToolInvoker toolInvoker,
                        ReflectionEngine reflector, MemoryStore memory,
                        @org.springframework.beans.factory.annotation.Value("${agent.default-model-id:1}") Long defaultModelId) {
        this.llmFeign = llmFeign;
        this.toolInvoker = toolInvoker;
        this.reflector = reflector;
        this.memory = memory;
        this.defaultModelId = defaultModelId;
    }

    public LlmAgent researcher() {
        return new LlmAgent("researcher", "研究员",
            "你是一名研究员, 擅长通过网络搜索和文档查证获取事实和数据. 给出信息时必须注明来源. 若不确定, 明确说'不确定'.",
            defaultModelId, List.of("http_fetch", "json"),
            llmFeign, toolInvoker, reflector, memory);
    }

    public LlmAgent writer() {
        return new LlmAgent("writer", "撰稿人",
            "你是一名专业撰稿人, 擅长把研究材料组织成结构清晰、逻辑严谨的报告. 报告结构: 摘要 + 章节 + 结论.",
            defaultModelId, List.of("json"),
            llmFeign, toolInvoker, reflector, memory);
    }

    public LlmAgent reviewer() {
        return new LlmAgent("reviewer", "审稿人",
            "你是一名严格的审稿人. 评估: 准确性 / 完整性 / 逻辑 / 可读性. 给出 1-10 分 + 具体改进建议.",
            defaultModelId, List.of("json"),
            llmFeign, toolInvoker, reflector, memory);
    }

    public LlmAgent coder() {
        return new LlmAgent("coder", "工程师",
            "你是一名工程师, 擅长写代码 / 跑 SQL / 解析数据. 输出可运行的代码或数据结果.",
            defaultModelId, List.of("calculator", "sql_query", "json", "datetime"),
            llmFeign, toolInvoker, reflector, memory);
    }
}
