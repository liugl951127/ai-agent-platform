package com.platform.common.redisson;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 工具类
 * <p>
 * 聚合常用操作,业务代码直接注入使用。
 * <p>
 * 能力:
 *   - 分布式锁(可重入 / 公平 / 读写 / RedLock)
 *   - 分布式限流器(令牌桶)
 *   - 分布式原子计数器
 *   - 分布式集合 / 队列 / Map
 *   - 分布式主题(Pub/Sub)
 *   - 延迟队列
 */
@Slf4j
@Component
public class RedissonUtil {

    private final RedissonClient redisson;

    public RedissonUtil(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @PostConstruct
    public void init() { log.info("RedissonUtil initialized"); }
    @PreDestroy
    public void destroy() { log.info("RedissonUtil destroyed"); }

    // ============================================================
    // 1. 分布式锁
    // ============================================================

    /**
     * 简单加锁执行(可重入锁)
     * <p>
     * 注意: 不要在 tryLock 失败分支里执行,否则锁会被提前释放。
     */
    public <T> T executeWithLock(String key, int waitSeconds, int leaseSeconds, Supplier<T> action) {
        RLock lock = redisson.getLock("lock:" + key);
        boolean ok;
        try {
            ok = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("加锁被中断: " + key, e);
        }
        if (!ok) throw new RuntimeException("获取锁失败: " + key);
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    /** 公平锁(按请求顺序排队) */
    public RLock fairLock(String key) { return redisson.getFairLock("lock:" + key); }

    /** 读锁(共享) */
    public RReadWriteLock rwLock(String key) { return redisson.getReadWriteLock("lock:" + key); }

    // ============================================================
    // 2. 分布式限流器(令牌桶) - 比 Sentinel 更细粒度的"按资源"
    // ============================================================

    /**
     * 尝试获取 1 个令牌(令牌桶模式)
     * @param key     限流资源
     * @param permits 每 period 内允许的令牌数
     * @param period  周期(秒)
     */
    public boolean tryAcquire(String key, long permits, long period) {
        RRateLimiter limiter = redisson.getRateLimiter("rl:" + key);
        // OVERALL: 整个集群共享同一桶;PER_CLIENT: 每客户端独立
        limiter.trySetRate(RateType.OVERALL, permits, period, RateIntervalUnit.SECONDS);
        return limiter.tryAcquire();
    }

    // ============================================================
    // 3. 分布式原子计数器(可过期)
    // ============================================================

    /** 自增并返回新值 */
    public long incr(String key) { return redisson.getAtomicLong("cnt:" + key).incrementAndGet(); }

    /** 自减并返回新值 */
    public long decr(String key) { return redisson.getAtomicLong("cnt:" + key).decrementAndGet(); }

    /** 带过期时间的计数器(用于滑动窗口计数 / 防刷) */
    public long incrWithExpire(String key, long expireSeconds) {
        RAtomicLong counter = redisson.getAtomicLong("cnt:" + key);
        long v = counter.incrementAndGet();
        counter.expire(expireSeconds, TimeUnit.SECONDS);
        return v;
    }

    // ============================================================
    // 4. 分布式集合
    // ============================================================

    public <V> RSet<V> set(String key) { return redisson.getSet("set:" + key); }
    public <K, V> RMap<K, V> map(String key) { return redisson.getMap("map:" + key); }
    public <V> RList<V> list(String key) { return redisson.getList("list:" + key); }
    public <V> RQueue<V> queue(String key) { return redisson.getQueue("queue:" + key); }
    public <V> RBlockingQueue<V> blockingQueue(String key) { return redisson.getBlockingQueue("bqueue:" + key); }

    // ============================================================
    // 5. 分布式 Pub/Sub
    // ============================================================

    public RTopic topic(String name) { return redisson.getTopic("topic:" + name); }

    public long publish(String topic, Object msg) {
        return topic(topic).publish(msg);
    }

    // ============================================================
    // 6. 延迟队列(消息指定延迟时间)
    // ============================================================

    /** 投递一个延迟消息(到 delayQueueName 队列) */
    public <V> void offerDelayed(String delayQueueName, V payload, long delay, TimeUnit unit) {
        RDelayedQueue<V> dq = redisson.getDelayedQueue(redisson.getQueue("dq:" + delayQueueName));
        dq.offer(payload, delay, unit);
    }
}
