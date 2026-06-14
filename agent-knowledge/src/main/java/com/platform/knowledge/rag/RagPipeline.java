package com.platform.knowledge.rag;

import com.platform.knowledge.rag.chunker.SemanticChunker;
import com.platform.knowledge.rag.embedding.EmbeddingClient;
import com.platform.knowledge.rag.rerank.Reranker;
import com.platform.knowledge.rag.retriever.RetrievedChunk;
import com.platform.knowledge.rag.retriever.Retriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG 完整流水线
 * <p>
 * 索引: 文档 → 语义切分 → Embedding → 向量存储
 * 检索: query → Embedding → 向量召回 topK*3 → 重排 → topK
 * <p>
 * 提供 RAG 友好的 prompt 拼装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipeline {

    private final SemanticChunker chunker;
    private final EmbeddingClient embedder;
    private final Retriever retriever;
    private final Reranker reranker;

    /**
     * 把整篇文档加入知识库
     * @param docId   文档 ID
     * @param content 文本
     * @return 生成的 chunk 数
     */
    public int ingest(String docId, String content) {
        List<String> chunks = chunker.chunk(content);
        if (chunks.isEmpty()) return 0;
        List<List<Float>> vecs = embedder.embedBatch(chunks);
        AtomicInteger pos = new AtomicInteger(0);
        for (int i = 0; i < chunks.size(); i++) {
            String c = chunks.get(i);
            List<Float> v = i < vecs.size() ? vecs.get(i) : embedder.embed(c);
            retriever.add(RetrievedChunk.builder()
                .id(docId + "-#i" + pos.getAndIncrement())
                .docId(docId)
                .content(c)
                .position(i)
                .build(), v);
        }
        log.info("RAG 索引 doc={} chunks={}", docId, chunks.size());
        return chunks.size();
    }

    /**
     * 检索
     * @param query 用户问题
     * @param topK  返回条数
     */
    public List<RetrievedChunk> search(String query, int topK) {
        List<Float> qv = embedder.embed(query);
        List<RetrievedChunk> rough = retriever.search(qv, Math.max(topK * 3, 10));
        return reranker.rerank(query, rough.subList(0, Math.min(rough.size(), topK * 3)))
                       .subList(0, Math.min(topK, rough.size()));
    }

    /**
     * RAG 风格的 prompt 拼装 — 把检索结果作为 context 拼到 system 提示
     */
    public String buildRagPrompt(String query, List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "你只能根据自身知识回答, 不确定就明说.\n用户问题: " + query;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("你是 AI 助手, 严格根据以下背景知识回答用户问题. ");
        sb.append("若知识中没有, 请明说'资料中未提及'.\n\n");
        sb.append("## 背景知识\n");
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            sb.append("[").append(i + 1).append("] (score=")
              .append(String.format("%.3f", c.getScore())).append(") ")
              .append(c.getContent()).append("\n\n");
        }
        sb.append("## 用户问题\n").append(query);
        return sb.toString();
    }

    /** 一站式: 检索 + 拼 prompt */
    public String retrieveAndBuild(String query, int topK) {
        List<RetrievedChunk> chunks = search(query, topK);
        return buildRagPrompt(query, chunks);
    }
}
