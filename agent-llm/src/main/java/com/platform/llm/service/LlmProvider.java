package com.platform.llm.service;

import com.platform.llm.entity.LlmModel;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

public interface LlmProvider {
    String provider();
    Flux<String> chatStream(LlmModel model, List<Map<String,Object>> messages);
    String chat(LlmModel model, List<Map<String,Object>> messages);
}
