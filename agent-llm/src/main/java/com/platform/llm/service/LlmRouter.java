package com.platform.llm.service;

import com.platform.llm.entity.LlmModel;
import com.platform.llm.mapper.LlmModelMapper;
import com.platform.llm.service.impl.OllamaProvider;
import com.platform.llm.service.impl.OpenAiProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Flux<String> streamById(Long modelId, List<Map<String,Object>> messages) {
        LlmModel m = modelMapper.selectById(modelId);
        if (m == null) return Flux.error(new RuntimeException("模型不存在"));
        return registry.get(m.getProvider()).chatStream(m, messages);
    }

    public String chatById(Long modelId, List<Map<String,Object>> messages) {
        LlmModel m = modelMapper.selectById(modelId);
        return registry.get(m.getProvider()).chat(m, messages);
    }
}
