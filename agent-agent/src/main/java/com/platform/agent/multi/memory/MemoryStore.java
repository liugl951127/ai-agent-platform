package com.platform.agent.multi.memory;

import java.util.List;
import java.util.Map;

/**
 * Memory 抽象 — Agent 跨轮次记忆
 * <p>
 * 三层:
 *   - Working    当前对话 (内存, 跟随 session)
 *   - Short-term 中期记忆 (Redis, session 维度, 7 天)
 *   - Long-term  长期画像 (MySQL, 用户维度, 永久)
 * <p>
 * 接口统一, 内部按 sessionId / userId 分发
 */
public interface MemoryStore {

    /** 追加一条消息 */
    void append(String sessionId, String role, String content);

    /** 拉取最近 N 条 (Working Memory) */
    List<Map<String,Object>> recent(String sessionId, int n);

    /** 长期画像 — 累积的事实/偏好 */
    Map<String,Object> getProfile(String userId);
    void updateProfile(String userId, String key, Object value);

    /** 清空 session */
    void clear(String sessionId);
}
