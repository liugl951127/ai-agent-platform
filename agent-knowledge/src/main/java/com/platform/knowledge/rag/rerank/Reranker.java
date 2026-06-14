package com.platform.knowledge.rag.rerank;

import com.platform.knowledge.rag.retriever.RetrievedChunk;

import java.util.List;

/**
 * 重排器 — 对向量召回的候选做精排
 * <p>
 * 实现:
 *   - KeywordBoostReranker     简单关键词加权 (零依赖)
 *   - CrossEncoderReranker     调 Cross-Encoder (高准确, 需外网)
 */
public interface Reranker {
    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates);
}
