package com.platform.knowledge.rag;

import com.platform.knowledge.rag.chunker.SemanticChunker;
import com.platform.knowledge.rag.embedding.HashEmbedding;
import com.platform.knowledge.rag.rerank.KeywordBoostReranker;
import com.platform.knowledge.rag.retriever.InMemoryVectorStore;
import com.platform.knowledge.rag.retriever.RetrievedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RagPipelineTest {

    private RagPipeline rag;

    @BeforeEach
    void setUp() {
        rag = new RagPipeline(
            new SemanticChunker(),
            new HashEmbedding(),
            new InMemoryVectorStore(),
            new KeywordBoostReranker()
        );
    }

    @Test
    void testIngestAndSearch() {
        String doc = "Spring AI 框架支持 LLM 集成。\n\n" +
            "它还支持 RAG 模式, 把外部知识接入到 LLM 的 prompt 中。\n\n" +
            "Embedding 模型用来把文本转成向量。\n\n" +
            "向量存储用 ES 或其他专用数据库。";
        int n = rag.ingest("doc1", doc);
        assertTrue(n >= 3);

        var chunks = rag.search("什么是 Spring AI", 2);
        assertEquals(2, chunks.size());
        // 第一个应包含 "Spring AI"
        boolean found = chunks.stream().anyMatch(c -> c.getContent().contains("Spring AI"));
        assertTrue(found, "检索结果应包含 Spring AI 关键词的段落");
    }

    @Test
    void testRerankKeywordBoost() {
        rag.ingest("a", "Spring AI framework for Java");
        rag.ingest("b", "Python framework");
        var results = rag.search("Spring AI", 2);
        assertTrue(results.size() > 0);
        // "Spring AI" 文档应排第一 (重排后)
        assertTrue(results.get(0).getContent().contains("Spring AI"));
    }

    @Test
    void testBuildRagPrompt() {
        var chunk = RetrievedChunk.builder().id("1").docId("d").content("Spring AI 是框架").score(0.8).build();
        String prompt = rag.buildRagPrompt("什么是 Spring AI", List.of(chunk));
        assertTrue(prompt.contains("背景知识"));
        assertTrue(prompt.contains("Spring AI 是框架"));
        assertTrue(prompt.contains("0.8"));
    }

    @Test
    void testBuildRagPromptEmpty() {
        String prompt = rag.buildRagPrompt("x", List.of());
        assertTrue(prompt.contains("用户问题"));
    }

    @Test
    void testRetrieveAndBuild() {
        rag.ingest("d", "RAG 是检索增强生成. 它把外部知识拼到 LLM 的 context.");
        String prompt = rag.retrieveAndBuild("RAG 是什么", 2);
        assertTrue(prompt.length() > 0);
    }

    @Test
    void testMultipleDocs() {
        rag.ingest("d1", "Java 是一门静态类型语言");
        rag.ingest("d2", "Python 是一门动态类型语言");
        rag.ingest("d3", "Spring AI 是 Java 框架");
        var r = rag.search("Java 框架", 3);
        assertTrue(r.size() > 0);
    }
}
