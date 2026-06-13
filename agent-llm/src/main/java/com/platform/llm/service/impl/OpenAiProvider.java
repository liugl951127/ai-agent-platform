package com.platform.llm.service.impl;

import com.platform.llm.entity.LlmModel;
import com.platform.llm.service.LlmProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiProvider implements LlmProvider {

    @Override
    public String provider() { return "OPENAI"; }

    @Override
    public Flux<String> chatStream(LlmModel m, List<Map<String,Object>> messages) {
        WebClient client = WebClient.builder()
                .baseUrl(m.getApiBase() == null ? "https://api.openai.com" : m.getApiBase())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + m.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        Map<String,Object> body = Map.of(
            "model", m.getModelName(),
            "messages", messages,
            "temperature", m.getTemperature(),
            "max_tokens", m.getMaxTokens(),
            "stream", true
        );
        return client.post().uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class);
    }

    @Override
    public String chat(LlmModel m, List<Map<String,Object>> messages) {
        WebClient client = WebClient.builder()
                .baseUrl(m.getApiBase() == null ? "https://api.openai.com" : m.getApiBase())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + m.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        Map<String,Object> body = Map.of(
            "model", m.getModelName(),
            "messages", messages,
            "temperature", m.getTemperature(),
            "max_tokens", m.getMaxTokens()
        );
        Map<?,?> resp = client.post().uri("/v1/chat/completions")
                .bodyValue(body).retrieve().bodyToMono(Map.class).block();
        try {
            List<?> choices = (List<?>) resp.get("choices");
            Map<?,?> ch0 = (Map<?,?>) choices.get(0);
            Map<?,?> msg = (Map<?,?>) ch0.get("message");
            return String.valueOf(msg.get("content"));
        } catch (Exception e) { return ""; }
    }
}
