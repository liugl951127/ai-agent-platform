package com.platform.llm.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.platform.llm.entity.LlmModel;
import com.platform.llm.mapper.LlmModelMapper;
import com.platform.llm.service.impl.OllamaProvider;
import com.platform.llm.service.impl.OpenAiProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmRouter {
    private final OpenAiProvider openAi;
    private final OllamaProvider ollama;
    private final LlmModelMapper modelMapper;
    private final Map<String, LlmProvider> registry = new HashMap<>();

    @PostConstruct
    public void init() {
        registry.put("OPENAI", openAi);
        registry.put("OLLAMA", ollama);
    }

    /**
     * 流式对话 - 方法级限流
     * 资源名: llm:stream:byId
     */
    @SentinelResource(
        value = "llm:stream:byId",
        blockHandler = "streamBlockHandler",
        fallback = "streamFallback"
    )
    public Flux<String> streamById(Long modelId, List<Map<String,Object>> messages) {
        LlmModel m = modelMapper.selectById(modelId);
        if (m == null) return Flux.error(new RuntimeException("模型不存在"));
        return registry.get(m.getProvider()).chatStream(m, messages);
    }

    public Flux<String> streamBlockHandler(Long modelId, List<Map<String,Object>> messages, BlockException e) {
        log.warn("LLM 流式被限流: modelId={}", modelId);
        return Flux.just("⚠️ 当前请求过多,请稍后再试");
    }
    public Flux<String> streamFallback(Long modelId, List<Map<String,Object>> messages, Throwable e) {
        log.error("LLM 流式异常: {}", e.getMessage());
        return Flux.just("服务异常: " + e.getMessage());
    }

    /**
     * 非流式对话 - 方法级限流
     * 资源名: llm:chat:byId
     */
    @SentinelResource(
        value = "llm:chat:byId",
        blockHandler = "chatBlockHandler"
    )
    public String chatById(Long modelId, List<Map<String,Object>> messages) {
        LlmModel m = modelMapper.selectById(modelId);
        return registry.get(m.getProvider()).chat(m, messages);
    }

    public String chatBlockHandler(Long modelId, List<Map<String,Object>> messages, BlockException e) {
        log.warn("LLM 对话被限流: modelId={}", modelId);
        return "⚠️ 当前请求过多,请稍后再试";
    }
}
