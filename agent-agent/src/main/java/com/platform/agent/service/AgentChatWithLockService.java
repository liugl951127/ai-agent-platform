package com.platform.agent.service;

import com.platform.agent.entity.AgentInfo;
import com.platform.agent.mapper.AgentInfoMapper;
import com.platform.common.core.R;
import com.platform.common.redisson.RedissonUtil;
import com.platform.common.redisson.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 智能体对话 - 加分布式锁版本
 * <p>
 * 演示:
 *   1) @DistributedLock:同一 agentId 同时只允许一个 ReAct 推理,避免并发踩踏
 *   2) RedissonUtil.incrWithExpire: 每用户每分钟最多 N 次,防刷
 *   3) RedissonUtil.tryAcquire: 分布式令牌桶限流(比 Sentinel 更细)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatWithLockService {

    private final ReactExecutor reactExecutor;
    private final AgentInfoMapper agentMapper;
    private final RedissonUtil redisson;

    private static final long RATE_LIMIT_PERMITS = 10;  // 周期内允许
    private static final long RATE_LIMIT_PERIOD  = 60;  // 周期(秒)

    @DistributedLock(
        key = "agent:chat:#{#agentId}",
        prefix = "lock:",
        waitTime = 5,
        leaseTime = 60
    )
    public R<String> chatWithLock(Long agentId, Long userId, String input) {
        // 1. 分布式令牌桶限流(每个用户每 60s 最多 10 次)
        String rateKey = "user:" + userId;
        if (!redisson.tryAcquire(rateKey, RATE_LIMIT_PERMITS, RATE_LIMIT_PERIOD)) {
            log.warn("限流命中: userId={}, agentId={}", userId, agentId);
            return R.fail(429, "请求过于频繁,请稍后再试");
        }

        // 2. 计数(用于实时统计)
        long calls = redisson.incr("agent:" + agentId + ":calls");

        // 3. 拿智能体
        AgentInfo agent = agentMapper.selectById(agentId);
        if (agent == null) return R.fail("智能体不存在: " + agentId);

        // 4. 调 ReAct(同一 agentId 不会并发,锁在外面)
        String reply = reactExecutor.run(agentId, input);
        log.info("对话完成: agentId={}, userId={}, 第 {} 次调用, 回复长度={}",
                 agentId, userId, calls, reply == null ? 0 : reply.length());

        return R.ok(reply);
    }
}
