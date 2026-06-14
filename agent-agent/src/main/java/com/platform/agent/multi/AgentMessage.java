package com.platform.agent.multi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 间消息 (A2A 协议)
 * <p>
 * 类似邮件 / 即时通讯: from / to / topic / body
 * <p>
 * topic 示例:
 *   - "task.assign"  上游派活
 *   - "task.result"  下游回报
 *   - "chat"         GroupChat 发言
 *   - "vote"         投票表决
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AgentMessage {
    private String id;             // UUID
    private String from;           // 发送方 Agent 名
    private String to;             // 目标 Agent 名 / "all" 群发
    private String topic;          // 消息主题
    private String body;           // 文本内容
    @Builder.Default
    private Map<String,Object> attachments = new HashMap<>();
    private long timestamp;
}
