package com.platform.agent.multi.bus;

import com.platform.agent.multi.AgentMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * Agent 间消息总线
 * <p>
 * 两种实现:
 *   - InMemoryBus   单 JVM 内, 用 ConcurrentHashMap (开发/单测)
 *   - RedisBus      跨服务, 用 Redis Streams (生产环境, 多 agent-agent 实例)
 * <p>
 * 接口设计参考 "观察者" + "发布订阅" 模式
 */
public interface MessageBus {

    /**
     * 发布消息
     * @param msg 消息
     */
    void publish(AgentMessage msg);

    /**
     * 订阅某 Agent 的收件箱
     * @param agentName Agent 名
     * @param handler   消息处理函数
     */
    void subscribe(String agentName, Consumer<AgentMessage> handler);

    /**
     * 拉取某 Agent 的消息 (非订阅式, 用于 GroupChat 顺序处理)
     */
    List<AgentMessage> poll(String agentName, int max);

    /**
     * 取消订阅
     */
    void unsubscribe(String agentName, Consumer<AgentMessage> handler);
}
