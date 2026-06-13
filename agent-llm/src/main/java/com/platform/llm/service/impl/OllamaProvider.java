package com.platform.llm.service.impl;

import com.platform.llm.entity.LlmModel;
import com.platform.llm.service.LlmProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

@Component
public class OllamaProvider implements LlmProvider {
    @Override public String provider() { return "OLLAMA"; }

    @Override
    public Flux<String> chatStream(LlmModel m, List<Map<String,Object>> messages) {
        WebClient client = WebClient.builder().baseUrl(m.getApiBase()).build();
        return client.post().uri("/api/chat")
                .bodyValue(Map.of("model", m.getModelName(), "messages", messages, "stream", true))
                .retrieve().bodyToFlux(String.class);
    }
    @Override
    public String chat(LlmModel m, List<Map<String,Object>> messages) {
        WebClient client = WebClient.builder().baseUrl(m.getApiBase()).build();
        Map<?,?> resp = client.post().uri("/api/chat")
                .bodyValue(Map.of("model", m.getModelName(), "messages", messages, "stream", false))
                .retrieve().bodyToMono(Map.class).block();
        try {
            Map<?,?> msg = (Map<?,?>) resp.get("message");
            return String.valueOf(msg.get("content"));
        } catch (Exception e) { return ""; }
    }
}
