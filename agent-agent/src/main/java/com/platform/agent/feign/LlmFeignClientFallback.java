package com.platform.agent.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LlmFeignClient 降级实现 — agent-llm 不可达时使用
 * <p>
 * 用 @Component + FeignClient.fallback 绑定, Spring Cloud 会自动包装
 * <p>
 * 不依赖 Nacos, 不调远端, 返回一个明确的错误 — 让调用方知道
 * <p>
 * 注意: 这里返回的内容, 不会作为真实 LLM 响应, 仅作"不可用"信号
 */
@Slf4j
@Component
public class LlmFeignClientFallback implements LlmFeignClient {

    @Override
    public Map<String, Object> chat(Long modelId, List<Map<String, Object>> messages) {
        log.warn("[fallback] LlmFeignClient.chat 被降级调用, modelId={}, messages={}", modelId, messages.size());
        // 返回结构与 LlmFeignClient 期望一致 — content/promptTokens/completionTokens
        return Map.of(
            "content", "[fallback] agent-llm 服务不可用, 无法生成回答",
            "ok", false,
            "fallback", true
        );
    }
}
