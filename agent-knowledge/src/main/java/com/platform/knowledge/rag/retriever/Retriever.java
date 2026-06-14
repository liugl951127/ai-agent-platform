package com.platform.knowledge.rag.retriever;

import java.util.List;

/**
 * 检索器 — query → 候选 chunks
 * <p>
 * 实现:
 *   - InMemoryVectorStore  内存版, 单测 / 演示
 *   - EsVectorStore        生产, ES 8.x dense_vector + knn
 */
public interface Retriever {

    /**
     * 添加 chunk 到索引
     */
    void add(RetrievedChunk chunk, List<Float> vector);

    /**
     * 相似度检索
     * @param query   query 向量
     * @param topK    返回前 K
     * @return 按相似度降序
     */
    List<RetrievedChunk> search(List<Float> query, int topK);

    /** 清空 */
    void clear();
}
