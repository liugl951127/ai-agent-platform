package com.platform.agent.multi.reflection;

import cn.hutool.json.JSONUtil;
import com.platform.agent.feign.LlmFeignClient;
import com.platform.agent.multi.Agent;
import com.platform.agent.multi.AgentContext;
import com.platform.agent.multi.AgentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * LLM 自检式反思 — 调一次 LLM 给原答案打分/建议
 * <p>
 * 流程:
 *   1. 把原答案 + 评判标准 发给 LLM
 *   2. LLM 返回 JSON: {score: 1-10, issues: [...], improved: "改后答案"}
 *   3. score >= 7 → 用 improved 替换
 *      score < 7  → 保持原答案, 加反思记录
 *   4. 最多 1 轮反思 (避免递归)
 */
@Slf4j
public class LLMSelfCritiqueReflection implements ReflectionEngine {

    private final LlmFeignClient llmFeign;
    private final Long modelId;
    private final double threshold;

    public LLMSelfCritiqueReflection(LlmFeignClient llmFeign, Long modelId, double threshold) {
        this.llmFeign = llmFeign;
        this.modelId = modelId;
        this.threshold = threshold;
    }

    @Override
    public AgentResponse reflect(Agent agent, AgentContext ctx, AgentResponse resp) {
        String prompt = """
            你是一个严格的评审员. 下面是 Agent 对用户问题的回答, 请评估:
              - 准确性: 事实/数据是否正确
              - 完整性: 是否回答了用户的所有关切
              - 清晰度: 结构是否清楚, 是否可读

            用户原始问题: %s

            Agent 回答:
            ---
            %s
            ---

            请严格按 JSON 格式返回 (不要加任何其他内容):
            {"score": 1-10 的整数, "issues": ["问题1", "问题2", ...], "improved": "改后完整答案 (若无需改则与原答案相同)"}
            """.formatted(ctx.getUserInput(), resp.getContent());

        try {
            Map<String,Object> r = llmFeign.chat(modelId, List.of(
                Map.of("role", "system", "content", "你是严格的代码/答案评审员, 严格按 JSON 输出"),
                Map.of("role", "user", "content", prompt)
            ));
            String text = String.valueOf(r.getOrDefault("content", ""));
            // 提取 JSON
            int a = text.indexOf('{'), b = text.lastIndexOf('}');
            if (a < 0 || b < 0) return resp;
            Map<?,?> critique = JSONUtil.parseObj(text.substring(a, b + 1));
            int score = ((Number) critique.get("score")).intValue();
            String improved = String.valueOf(critique.get("improved"));
            StringBuilder newThought = new StringBuilder(resp.getThought())
                .append("\n\n[Reflection score=").append(score).append("/10]");
            if (score < threshold) {
                @SuppressWarnings("unchecked")
                List<String> issues = (List<String>) critique.get("issues");
                if (issues != null && !issues.isEmpty()) newThought.append(" issues=").append(issues);
                newThought.append(" (score<").append(threshold).append(", keep original)");
            } else {
                newThought.append(" (improved accepted)");
            }
            return AgentResponse.builder()
                .content(improved)
                .thought(newThought.toString())
                .actions(resp.getActions())
                .artifacts(resp.getArtifacts())
                .promptTokens(resp.getPromptTokens() + intVal(r, "promptTokens"))
                .completionTokens(resp.getCompletionTokens() + intVal(r, "completionTokens"))
                .elapsedMs(resp.getElapsedMs())
                .stopReason(resp.getStopReason())
                .build();
        } catch (Exception e) {
            log.warn("Reflection 失败, 保留原答案: {}", e.getMessage());
            return resp;
        }
    }

    private static int intVal(Map<String,Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }
}
