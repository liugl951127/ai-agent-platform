package com.platform.tools.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具定义注解 — 描述工具的能力、参数 schema
 * <p>
 * 注册到 {@link com.platform.tools.registry.ToolRegistry} 后,
 * ReAct / Plan-Execute Agent 会自动把它转成 LLM function calling 格式
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolDefinition {

    /** 工具名 (默认用实现类 name() 方法返回值) */
    String name() default "";

    /** 一句话描述 (LLM 用来决定何时调此工具) */
    String description();

    /**
     * 参数 schema, JSON Schema 风格 (OpenAI function calling 同款)
     * 例: {"type":"object","properties":{"city":{"type":"string","description":"城市名"}},"required":["city"]}
     */
    String parameters();

    /**
     * 分类标签, 便于检索
     * 例: "math", "web", "system", "database"
     */
    String category() default "general";
}
