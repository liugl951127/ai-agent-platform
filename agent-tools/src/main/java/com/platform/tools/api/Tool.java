package com.platform.tools.api;

import java.util.Map;

/**
 * 工具接口 — 所有 Agent 可调用的能力
 * <p>
 * 实现类用 {@link ToolDefinition} 注解描述给 LLM 的 schema
 * <p>
 * 调用方传入 args (Map 形式), 返回结果也用 Map 包装, 统一可序列化
 */
public interface Tool {

    /** 工具名 — LLM 在 JSON 中通过此名调用, 必须全局唯一 */
    String name();

    /**
     * 执行工具
     *
     * @param args 调用方传入的参数 (来自 LLM 输出的 JSON)
     * @return 执行结果, 必须是简单类型 (String/Number/Boolean/Map/List) 才能 JSON 序列化
     * @throws Exception 任何异常都会被 ReActExecutor 捕获并作为 Observation 返回给 LLM
     */
    Object execute(Map<String, Object> args) throws Exception;
}
