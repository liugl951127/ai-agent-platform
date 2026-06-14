package com.platform.knowledge.rag.embedding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashEmbeddingTest {

    private final HashEmbedding emb = new HashEmbedding();

    @Test
    void testDeterministic() {
        var v1 = emb.embed("hello world");
        var v2 = emb.embed("hello world");
        assertEquals(v1, v2);
    }

    @Test
    void testDimension() {
        var v = emb.embed("test");
        assertEquals(256, v.size());
    }

    @Test
    void testNormalized() {
        var v = emb.embed("hello world hello world");
        double norm = 0;
        for (float f : v) norm += f * f;
        assertEquals(1.0, Math.sqrt(norm), 0.01);
    }

    @Test
    void testSimilarity() {
        var a = emb.embed("Spring AI is a framework for Java");
        var b = emb.embed("Spring AI is a framework for Java");
        var c = emb.embed("completely unrelated text about cooking");
        double simAB = cos(a, b);
        double simAC = cos(a, c);
        assertTrue(simAB > simAC, "相同文本应比无关文本相似度高, AB=" + simAB + " AC=" + simAC);
        assertEquals(1.0, simAB, 0.01);
    }

    @Test
    void testEmpty() {
        var v = emb.embed("");
        assertEquals(256, v.size());
    }

    @Test
    void testNull() {
        var v = emb.embed(null);
        assertEquals(256, v.size());
    }

    private static double cos(java.util.List<Float> a, java.util.List<Float> b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            na += a.get(i) * a.get(i);
            nb += b.get(i) * b.get(i);
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
