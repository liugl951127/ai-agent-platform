package com.platform.knowledge.rag.chunker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SemanticChunkerTest {

    private final SemanticChunker chunker = new SemanticChunker();

    @Test
    void testEmpty() {
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   ").isEmpty());
    }

    @Test
    void testSingleParagraph() {
        String text = "Spring AI 是一个用于 AI 工程的应用框架。它支持 LLM、Embedding、向量存储等能力。";
        var chunks = chunker.chunk(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void testMultipleParagraphs() {
        String text = "第一段内容。\n\n第二段内容。\n\n第三段。";
        var chunks = chunker.chunk(text);
        assertEquals(3, chunks.size());
    }

    @Test
    void testLongParagraphSplits() {
        // 构造 6000 字的文本 (超过 MAX_SIZE 1500)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) sb.append("这是第").append(i).append("句话。");
        var chunks = chunker.chunk(sb.toString());
        assertTrue(chunks.size() > 1, "应切分多块, 实际: " + chunks.size());
        // 每块不超过 MAX_SIZE (允许滑窗重叠 200, 所以最大 1700)
        for (String c : chunks) assertTrue(c.length() <= 1700, "块过长: " + c.length());
    }

    @Test
    void testEnglishPunctuation() {
        String text = "Spring AI is a framework. It supports LLM. Embedding is also supported. Vector store integration is included.";
        var chunks = chunker.chunk(text);
        assertTrue(chunks.size() >= 1);
        // 切分应基于句号
        assertTrue(chunks.get(0).length() <= 1500);
    }

    @Test
    void testNoLoss() {
        // 切分后拼接应能恢复大部分原文
        String text = "第一段。\n\n第二段。\n\n第三段。";
        var chunks = chunker.chunk(text);
        String joined = String.join(" ", chunks);
        // 每段核心都应在
        assertTrue(joined.contains("第一段"));
        assertTrue(joined.contains("第二段"));
        assertTrue(joined.contains("第三段"));
    }
}
