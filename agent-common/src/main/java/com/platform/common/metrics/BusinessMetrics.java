package com.platform.common.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 业务指标(Micrometer + Prometheus)
 * <p>
 * 通过 Prometheus 抓取端点 /actuator/prometheus 暴露给监控
 * <p>
 * 提供的指标:
 *   - agent_chat_total           counter   智能体对话总数
 *   - agent_chat_latency_seconds timer     智能体对话耗时直方图
 *   - llm_call_total             counter   LLM 调用总数(按 provider/model 分标签)
 *   - llm_call_latency_seconds   timer     LLM 调用耗时
 *   - audit_log_total            counter   审计日志写入总数
 *   - gray_release_total         counter   灰度命中总数
 *   - tenant_active_count        gauge     当前活跃租户数
 */
@Component
@ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 智能体对话 */
    public Counter agentChatTotal() {
        return Counter.builder("agent_chat_total")
                .description("智能体对话总数")
                .register(registry);
    }

    public Timer agentChatLatency() {
        return Timer.builder("agent_chat_latency_seconds")
                .description("智能体对话耗时")
                .publishPercentileHistogram()
                .register(registry);
    }

    /** LLM 调用 */
    public Counter llmCallTotal(String provider, String model) {
        return Counter.builder("llm_call_total")
                .description("LLM 调用总数")
                .tag("provider", provider)
                .tag("model", model == null ? "unknown" : model)
                .register(registry);
    }

    public Timer llmCallLatency(String provider) {
        return Timer.builder("llm_call_latency_seconds")
                .description("LLM 调用耗时")
                .tag("provider", provider)
                .publishPercentileHistogram()
                .register(registry);
    }

    /** 审计日志 */
    public Counter auditLogTotal(String module, String action) {
        return Counter.builder("audit_log_total")
                .description("审计日志总数")
                .tag("module", module)
                .tag("action", action)
                .register(registry);
    }

    /** 灰度命中 */
    public Counter grayReleaseTotal(String resource) {
        return Counter.builder("gray_release_total")
                .description("灰度命中数")
                .tag("resource", resource)
                .register(registry);
    }

    /** 记录方法耗时(泛用) */
    public <T> T record(String name, String tag, java.util.function.Supplier<T> action) {
        return Timer.builder(name)
                .tag("op", tag)
                .publishPercentileHistogram()
                .register(registry)
                .record(action);
    }

    /** 简易版 - 不返回结果 */
    public void record(Runnable action) {
        action.run();
    }
}
