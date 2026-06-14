package com.platform.knowledge.rag.retriever;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索到的 chunk
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RetrievedChunk {
    private String id;             // chunk 唯一 ID (e.g. docId-#3)
    private String docId;          // 所属文档
    private String content;        // chunk 文本
    private double score;          // 相似度 (余弦, 0~1)
    private int position;          // 文档内位置
}
