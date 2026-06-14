package com.platform.knowledge.rag.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.*;

/**
 * 兜底 Embedding: hash 词袋
 * <p>
 * 原理: 文本分词 → 哈希到 256 维 → L2 归一化
 * <p>
 * 缺点: 语义不准, 同义词距离远
 * 优点: 零外部依赖, 单测可用, 启动即可用
 * <p>
 * 优先级最低: 配了 OpenAI/Ollama 就会被替代
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "embeddingClient")
public class HashEmbedding implements EmbeddingClient {

    private static final int DIM = 256;

    @Override
    public List<Float> embed(String text) {
        if (text == null) return zero();
        String[] tokens = tokenize(text);
        float[] v = new float[DIM];
        for (String t : tokens) {
            int bucket = Math.floorMod(t.hashCode(), DIM);
            v[bucket] += 1.0f;
        }
        // L2 归一化
        double norm = 0;
        for (float f : v) norm += f * f;
        norm = Math.sqrt(norm);
        List<Float> out = new ArrayList<>(DIM);
        if (norm < 1e-9) {
            for (int i = 0; i < DIM; i++) out.add(0f);
        } else {
            for (float f : v) out.add((float) (f / norm));
        }
        return out;
    }

    @Override public int dimension() { return DIM; }

    private List<Float> zero() {
        List<Float> z = new ArrayList<>(DIM);
        for (int i = 0; i < DIM; i++) z.add(0f);
        return z;
    }

    /** 简单分词: 英文按空格 + 转小写, 中文按字符 */
    private String[] tokenize(String text) {
        text = text.toLowerCase();
        // 中文字符单字也算 token
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                buf.append(c);
            } else {
                if (buf.length() > 0) {
                    out.add(buf.toString());
                    buf.setLength(0);
                }
            }
        }
        if (buf.length() > 0) out.add(buf.toString());
        return out.toArray(new String[0]);
    }
}
