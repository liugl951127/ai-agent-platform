package com.platform.knowledge.rag.retriever;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存版向量存储 — 暴力余弦相似度
 * <p>
 * 适合:
 *   - 单测 / 演示
 *   - 小规模知识库 (< 10w chunks)
 * <p>
 * 生产: 用 EsVectorStore (ES 8.x dense_vector + HNSW)
 */
@Slf4j
@Component
@ConditionalOnMissingBean(Retriever.class)
public class InMemoryVectorStore implements Retriever {

    /** id → {chunk, vector} */
    private final Map<String, Entry> index = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Override
    public void add(RetrievedChunk chunk, List<Float> vector) {
        synchronized (lock) {
            index.put(chunk.getId(), new Entry(chunk, vector));
        }
    }

    @Override
    public List<RetrievedChunk> search(List<Float> query, int topK) {
        if (query == null || query.isEmpty()) return List.of();
        List<double[]> scored = new ArrayList<>();
        synchronized (lock) {
            for (Entry e : index.values()) {
                double s = cosine(query, e.vector);
                scored.add(new double[]{s, System.identityHashCode(e)});
                e.chunk.setScore(s);
            }
        }
        // 按 score 降序
        return index.values().stream()
            .sorted((a, b) -> Double.compare(b.chunk.getScore(), a.chunk.getScore()))
            .limit(topK)
            .map(e -> e.chunk)
            .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        synchronized (lock) {
            index.clear();
        }
    }

    private static double cosine(List<Float> a, List<Float> b) {
        int n = Math.min(a.size(), b.size());
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a.get(i) * b.get(i);
            na += a.get(i) * a.get(i);
            nb += b.get(i) * b.get(i);
        }
        if (na < 1e-9 || nb < 1e-9) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static class Entry {
        final RetrievedChunk chunk;
        final List<Float> vector;
        Entry(RetrievedChunk c, List<Float> v) { chunk = c; vector = v; }
    }
}
