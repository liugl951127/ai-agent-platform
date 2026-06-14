package com.platform.knowledge.rag.rerank;

import com.platform.knowledge.rag.retriever.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 关键词加权重排 — 对每条候选计算 query 词命中数, 加权原始 score (默认实现)
 * <p>
 * score_new = score * (1 + hits * 0.5)
 * <p>
 * 适合零依赖场景, 准确率比向量差, 但能"拉回"含 query 关键词但向量距离远的候选
 */
@Component
public class KeywordBoostReranker implements Reranker {

    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        String[] qTokens = tokenize(query);
        List<Scored> scored = new ArrayList<>();
        for (RetrievedChunk c : candidates) {
            String content = c.getContent() == null ? "" : c.getContent().toLowerCase();
            int hits = 0;
            for (String t : qTokens) {
                if (t.isEmpty()) continue;
                int idx = 0;
                while ((idx = content.indexOf(t, idx)) != -1) {
                    hits++;
                    idx += t.length();
                }
            }
            double newScore = c.getScore() * (1.0 + hits * 0.5);
            c.setScore(newScore);
            scored.add(new Scored(c, newScore));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<RetrievedChunk> out = new ArrayList<>(candidates.size());
        for (Scored s : scored) out.add(s.chunk);
        return out;
    }

    private String[] tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("\\s+|[\\p{Punct}]"))
            .filter(s -> s.length() > 1)
            .toArray(String[]::new);
    }

    private static class Scored {
        RetrievedChunk chunk;
        double score;
        Scored(RetrievedChunk c, double s) { chunk = c; score = s; }
    }
}
