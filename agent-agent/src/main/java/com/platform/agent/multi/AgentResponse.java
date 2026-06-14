package com.platform.agent.multi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent 响应
 * <p>
 * 字段:
 *   - content    最终输出 (Markdown 文本, 给用户/下游 Agent 看)
 *   - thought    推理过程 (CoT 透明化, 调试用)
 *   - actions    执行过的动作 (工具调用 / 委派)
 *   - artifacts  产出物 (报告 / 代码 / 数据, 复杂对象)
 *   - tokens     Token 用量
 *   - elapsedMs  耗时
 *   - stopReason 结束原因: "ok" / "max_steps" / "error" / "timeout"
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AgentResponse {
    @Builder.Default private String content = "";
    @Builder.Default private String thought = "";
    @Builder.Default private List<ActionRecord> actions = new ArrayList<>();
    @Builder.Default private Map<String,Object> artifacts = new java.util.HashMap<>();
    @Builder.Default private int promptTokens = 0;
    @Builder.Default private int completionTokens = 0;
    @Builder.Default private long elapsedMs = 0;
    @Builder.Default private String stopReason = "ok";

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActionRecord {
        private String type;      // "tool" / "delegate" / "plan"
        private String name;      // 工具名 / Agent 名
        private Map<String,Object> input;
        private Object output;
        private long elapsedMs;
        private boolean ok;
    }
}
