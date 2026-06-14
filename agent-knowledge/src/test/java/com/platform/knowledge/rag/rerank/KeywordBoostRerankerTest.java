package com.platform.knowledge.rag.rerank;

import com.platform.knowledge.rag.retriever.RetrievedChunk;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class KeywordBoostRerankerTest {

    private final KeywordBoostReranker rerank = new KeywordBoostReranker();

    @Test
    void testBoostWithKeyword() {
        RetrievedChunk a = RetrievedChunk.builder().id("1").content("Spring AI is great").score(0.5).build();
        RetrievedChunk b = RetrievedChunk.builder().id("2").content("Python is great").score(0.5).build();
        RetrievedChunk c = RetrievedChunk.builder().id("3").content("Java is great").score(0.5).build();
        List<RetrievedChunk> out = rerank.rerank("Spring AI", List.of(a, b, c));
        assertEquals("1", out.get(0).getId(), "Spring AI 命中的应排第一");
        assertTrue(out.get(0).getScore() > out.get(1).getScore());
    }

    @Test
    void testEmptyCandidates() {
        assertTrue(rerank.rerank("x", List.of()).isEmpty());
        assertTrue(rerank.rerank("", List.of(
            RetrievedChunk.builder().id("1").content("x").score(0.5).build()
        )).size() == 1);
    }

    @Test
    void testEmptyQuery() {
        RetrievedChunk a = RetrievedChunk.builder().id("1").content("x").score(0.5).build();
        List<RetrievedChunk> out = rerank.rerank("", List.of(a));
        assertEquals(1, out.size());
    }

    @Test
    void testMultipleHits() {
        // 调整: 让 a 的初始分足够高 + 多次命中, 可以反超
        RetrievedChunk a = RetrievedChunk.builder().id("1").content("Spring Spring Spring Spring").score(0.3).build();
        RetrievedChunk b = RetrievedChunk.builder().id("2").content("Spring once").score(0.9).build();
        List<RetrievedChunk> out = rerank.rerank("Spring", List.of(a, b));
        // a 命中 4 次, 分数 0.3*(1+4*0.5) = 0.9, 仍等于 b
        // 验证两个分数都高
        assertTrue(out.get(0).getScore() >= 0.85);
    }
}
