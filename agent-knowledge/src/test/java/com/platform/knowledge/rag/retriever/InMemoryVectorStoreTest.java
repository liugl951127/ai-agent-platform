package com.platform.knowledge.rag.retriever;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryVectorStoreTest {

    private final InMemoryVectorStore store = new InMemoryVectorStore();

    @Test
    void testAddAndSearch() {
        var a = makeVec(1, 0, 0);
        var b = makeVec(0, 1, 0);
        var c = makeVec(0, 0, 1);
        store.add(chunk("a", "A"), a);
        store.add(chunk("b", "B"), b);
        store.add(chunk("c", "C"), c);

        var results = store.search(a, 2);
        assertEquals(2, results.size());
        assertEquals("A", results.get(0).getContent());
        assertEquals(1.0, results.get(0).getScore(), 0.01);
    }

    @Test
    void testTopKLimit() {
        for (int i = 0; i < 10; i++) {
            store.add(chunk("d" + i, "d" + i), makeVec(i, 0, 0));
        }
        var results = store.search(makeVec(0, 0, 0), 5);
        assertEquals(5, results.size());
    }

    @Test
    void testClear() {
        store.add(chunk("x", "x"), makeVec(1, 0, 0));
        assertEquals(1, store.search(makeVec(1, 0, 0), 10).size());
        store.clear();
        assertEquals(0, store.search(makeVec(1, 0, 0), 10).size());
    }

    @Test
    void testEmptySearch() {
        assertTrue(store.search(makeVec(1, 0, 0), 5).isEmpty());
    }

    private static RetrievedChunk chunk(String id, String content) {
        return RetrievedChunk.builder().id(id).docId(id).content(content).build();
    }

    private static java.util.List<Float> makeVec(int x, int y, int z) {
        return java.util.List.of((float) x, (float) y, (float) z);
    }
}
