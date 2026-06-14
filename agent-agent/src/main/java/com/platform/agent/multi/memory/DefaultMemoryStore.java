package com.platform.agent.multi.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 MemoryStore — 适合开发/单测
 * <p>
 * 生产环境应换成 Redis 短期 + MySQL 长期
 * <p>
 * 设计: Working (ConcurrentHashMap<sessionId, Deque>) + Profile (userId → Map)
 * 容量限制: 每个 session 最多保留 100 条
 */
@Slf4j
@Component
@ConditionalOnMissingBean(MemoryStore.class)
public class DefaultMemoryStore implements MemoryStore {

    private static final int MAX_PER_SESSION = 100;
    private final Map<String, Deque<Map<String,Object>>> working = new ConcurrentHashMap<>();
    private final Map<String, Map<String,Object>> profile = new ConcurrentHashMap<>();

    @Override
    public void append(String sessionId, String role, String content) {
        if (sessionId == null) return;
        Deque<Map<String,Object>> q = working.computeIfAbsent(sessionId, k -> new ArrayDeque<>(MAX_PER_SESSION));
        synchronized (q) {
            if (q.size() >= MAX_PER_SESSION) q.pollFirst();
            q.addLast(Map.of("role", role, "content", content, "ts", System.currentTimeMillis()));
        }
    }

    @Override
    public List<Map<String,Object>> recent(String sessionId, int n) {
        if (sessionId == null) return List.of();
        Deque<Map<String,Object>> q = working.get(sessionId);
        if (q == null) return List.of();
        synchronized (q) {
            return new ArrayList<>(q).subList(Math.max(0, q.size() - n), q.size());
        }
    }

    @Override
    public Map<String,Object> getProfile(String userId) {
        if (userId == null) return Map.of();
        return profile.getOrDefault(userId, Map.of());
    }

    @Override
    public void updateProfile(String userId, String key, Object value) {
        if (userId == null) return;
        profile.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    @Override
    public void clear(String sessionId) {
        if (sessionId == null) return;
        working.remove(sessionId);
    }
}
