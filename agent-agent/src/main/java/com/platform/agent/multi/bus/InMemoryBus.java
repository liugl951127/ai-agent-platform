package com.platform.agent.multi.bus;

import com.platform.agent.multi.AgentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 内存版消息总线 — 适合开发/单测, 单 JVM
 * <p>
 * 用 @ConditionalOnMissingBean 标注, 有 RedisBus 时自动让位
 */
@Slf4j
@Component
@ConditionalOnMissingBean(MessageBus.class)
public class InMemoryBus implements MessageBus {

    /** agent 名 → 消息队列 */
    private final Map<String, Queue<AgentMessage>> inboxes = new ConcurrentHashMap<>();
    /** agent 名 → 订阅者列表 (用于 pub/sub 模式) */
    private final Map<String, List<Consumer<AgentMessage>>> subs = new ConcurrentHashMap<>();

    @Override
    public void publish(AgentMessage msg) {
        if (msg.getTo() == null || msg.getTo().isBlank()) {
            log.warn("消息无 to 字段, 丢弃: {}", msg);
            return;
        }
        if ("all".equalsIgnoreCase(msg.getTo())) {
            // 群发
            for (String name : inboxes.keySet()) {
                if (!name.equals(msg.getFrom())) {
                    inboxes.get(name).add(msg);
                }
            }
        } else {
            inboxes.computeIfAbsent(msg.getTo(), k -> new ConcurrentLinkedQueue<>()).add(msg);
        }
        // 同步通知订阅者
        List<Consumer<AgentMessage>> handlers = subs.get(msg.getTo());
        if (handlers != null) {
            for (Consumer<AgentMessage> h : handlers) {
                try { h.accept(msg); } catch (Exception e) { log.warn("订阅者处理失败: {}", e.getMessage()); }
            }
        }
    }

    @Override
    public void subscribe(String agentName, Consumer<AgentMessage> handler) {
        inboxes.computeIfAbsent(agentName, k -> new ConcurrentLinkedQueue<>());
        subs.computeIfAbsent(agentName, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @Override
    public List<AgentMessage> poll(String agentName, int max) {
        Queue<AgentMessage> q = inboxes.get(agentName);
        if (q == null || q.isEmpty()) return Collections.emptyList();
        List<AgentMessage> out = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            AgentMessage m = q.poll();
            if (m == null) break;
            out.add(m);
        }
        return out;
    }

    @Override
    public void unsubscribe(String agentName, Consumer<AgentMessage> handler) {
        List<Consumer<AgentMessage>> list = subs.get(agentName);
        if (list != null) list.remove(handler);
    }

    /** 测试用: 清空所有 */
    public void clear() {
        inboxes.clear();
        subs.clear();
    }
}
