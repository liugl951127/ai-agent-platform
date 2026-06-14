package com.platform.knowledge.rag.chunker;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义切分器 — 把长文档切成适合向量化的 chunk
 * <p>
 * 策略:
 *   - 优先按段落 (空行) 切
 *   - 段落超长 → 按句子切 (中英文标点)
 *   - 单句超长 → 滑窗 (重叠 200 字符)
 *   - 默认目标 chunk 大小: 500 字符, 最大 1500
 * <p>
 * 优点: 比固定字符切分更"语义完整", 检索召回率更高
 */
@Component
public class SemanticChunker {

    private static final int TARGET_SIZE = 500;
    private static final int MAX_SIZE = 1500;
    private static final int OVERLAP = 200;
    private static final Pattern SENTENCE_END =
        Pattern.compile("([。！？!?\\.\\n]+)");

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        // 1. 段落切
        String[] paragraphs = text.split("\\n\\s*\\n");
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;
            if (para.length() <= MAX_SIZE) {
                out.add(para);
            } else {
                // 2. 句子切
                List<String> sentences = splitSentences(para);
                StringBuilder buf = new StringBuilder();
                for (String s : sentences) {
                    if (buf.length() + s.length() > TARGET_SIZE && buf.length() > 0) {
                        out.add(buf.toString().trim());
                        // 保留 overlap
                        if (buf.length() > OVERLAP) {
                            buf = new StringBuilder(buf.substring(buf.length() - OVERLAP));
                        } else {
                            buf = new StringBuilder();
                        }
                    }
                    buf.append(s);
                }
                if (buf.length() > 0) out.add(buf.toString().trim());
            }
        }
        return out;
    }

    private List<String> splitSentences(String para) {
        List<String> out = new ArrayList<>();
        Matcher m = SENTENCE_END.matcher(para);
        int last = 0;
        while (m.find()) {
            int end = m.end();
            out.add(para.substring(last, end));
            last = end;
        }
        if (last < para.length()) {
            String tail = para.substring(last);
            if (tail.length() > MAX_SIZE) {
                // 滑窗
                for (int i = 0; i < tail.length(); i += TARGET_SIZE) {
                    out.add(tail.substring(i, Math.min(tail.length(), i + TARGET_SIZE)));
                }
            } else {
                out.add(tail);
            }
        }
        return out;
    }
}
