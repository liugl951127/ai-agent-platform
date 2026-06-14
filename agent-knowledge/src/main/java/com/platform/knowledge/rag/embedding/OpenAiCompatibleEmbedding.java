package com.platform.knowledge.rag.embedding;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * OpenAI 兼容 Embedding 客户端
 * <p>
 * 通过 application.yml 配 embedding.openai.* 启用
 * 兼容: OpenAI / Azure / 通义千问 / DeepSeek / Ollama(openai 兼容模式)
 * <p>
 * 默认 endpoint: https://api.openai.com/v1/embeddings
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "embedding.openai", name = "enabled", havingValue = "true")
public class OpenAiCompatibleEmbedding implements EmbeddingClient {

    @Value("${embedding.openai.base-url:https://api.openai.com}")
    private String baseUrl;
    @Value("${embedding.openai.api-key:}")
    private String apiKey;
    @Value("${embedding.openai.model:text-embedding-3-small}")
    private String model;
    @Value("${embedding.openai.timeout-ms:30000}")
    private long timeoutMs;

    private int dim = 1536;  // 由实际响应更新

    @Override
    public List<Float> embed(String text) {
        try (HttpResponse resp = HttpRequest.post(baseUrl + "/v1/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout((int) timeoutMs)
                .body(JSONUtil.toJsonStr(Map.of("input", text, "model", model)))
                .execute()) {
            if (resp.getStatus() != 200) {
                throw new RuntimeException("Embedding HTTP " + resp.getStatus() + ": " + resp.body());
            }
            JSONObject body = JSONUtil.parseObj(resp.body());
            JSONArray data = body.getJSONArray("data");
            if (data == null || data.isEmpty()) throw new RuntimeException("空响应");
            JSONArray vec = data.getJSONObject(0).getJSONArray("embedding");
            dim = vec.size();
            List<Float> out = new ArrayList<>(dim);
            for (int i = 0; i < dim; i++) out.add(vec.getFloat(i));
            return out;
        } catch (Exception e) {
            log.warn("OpenAI Embedding 失败, 降级到 hash: {}", e.getMessage());
            return new HashEmbedding().embed(text);
        }
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        // 一次最多 100 条
        List<List<Float>> out = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += 100) {
            List<String> batch = texts.subList(i, Math.min(texts.size(), i + 100));
            try (HttpResponse resp = HttpRequest.post(baseUrl + "/v1/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout((int) (timeoutMs * 2))
                    .body(JSONUtil.toJsonStr(Map.of("input", batch, "model", model)))
                    .execute()) {
                if (resp.getStatus() != 200) {
                    throw new RuntimeException("Embedding batch HTTP " + resp.getStatus());
                }
                JSONObject body = JSONUtil.parseObj(resp.body());
                JSONArray data = body.getJSONArray("data");
                for (int j = 0; j < data.size(); j++) {
                    JSONArray vec = data.getJSONObject(j).getJSONArray("embedding");
                    List<Float> v = new ArrayList<>();
                    for (int k = 0; k < vec.size(); k++) v.add(vec.getFloat(k));
                    out.add(v);
                }
            } catch (Exception e) {
                log.warn("OpenAI Embedding batch 失败, 降级: {}", e.getMessage());
                HashEmbedding h = new HashEmbedding();
                batch.forEach(t -> out.add(h.embed(t)));
            }
        }
        return out;
    }

    @Override public int dimension() { return dim; }
}
