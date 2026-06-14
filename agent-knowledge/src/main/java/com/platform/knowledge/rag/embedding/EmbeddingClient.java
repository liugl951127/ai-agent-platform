package com.platform.knowledge.rag.embedding;

import java.util.List;

/**
 * Embedding 客户端接口 — 文本 → 向量
 * <p>
 * 实现:
 *   - OpenAiCompatibleEmbedding  调 OpenAI 兼容 /v1/embeddings (默认)
 *   - OllamaEmbedding            调 Ollama /api/embeddings
 *   - HashEmbedding              (兜底) hash 词袋, 不准但零依赖
 */
public interface EmbeddingClient {

    /**
     * 单条文本转向量
     * @param text 待嵌入文本
     * @return 浮点向量 (维度由实现决定, 通常 384 / 768 / 1536 / 3072)
     */
    List<Float> embed(String text);

    /** 批量 (默认实现可逐条调) */
    default List<List<Float>> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    /** 向量维度 (用于 ES mapping 预设) */
    int dimension();
}
