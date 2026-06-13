package com.platform.common.redisson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedissonUtil 单元测试")
class RedissonUtilTest {

    @Mock RedissonClient redisson;
    @Mock RLock rLock;
    @Mock RRateLimiter limiter;
    @Mock RAtomicLong atomicLong;
    @Mock RSet<Object> rSet;
    @Mock RMap<Object, Object> rMap;
    @Mock RTopic topic;

    RedissonUtil util;

    @BeforeEach
    void setUp() {
        util = new RedissonUtil(redisson);
        util.init();
    }

    // ---------------- 锁 ----------------

    @Test
    @DisplayName("executeWithLock:获取锁成功 → 执行 → 释放")
    void executeWithLockOk() throws InterruptedException {
        when(redisson.getLock("lock:test")).thenReturn(rLock);
        when(rLock.tryLock(3, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        AtomicInteger ran = new AtomicInteger(0);
        Supplier<Integer> s = () -> { ran.incrementAndGet(); return 42; };

        Integer out = util.executeWithLock("test", 3, 30, s);
        assertEquals(42, out);
        assertEquals(1, ran.get());
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("executeWithLock:获取锁超时 → 抛异常,不执行")
    void executeWithLockTimeout() throws InterruptedException {
        when(redisson.getLock("lock:test")).thenReturn(rLock);
        when(rLock.tryLock(3, 30, TimeUnit.SECONDS)).thenReturn(false);

        Supplier<Integer> s = () -> { fail("不应执行"); return 0; };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> util.executeWithLock("test", 3, 30, s));
        assertTrue(ex.getMessage().contains("获取锁失败"));
        verify(rLock, never()).unlock();
    }

    // ---------------- 限流器 ----------------

    @Test
    @DisplayName("tryAcquire:令牌可用返回 true")
    void tryAcquireOk() {
        when(redisson.getRateLimiter("rl:user:1")).thenReturn(limiter);
        when(limiter.trySetRate(RateType.OVERALL, 10, 60, RateIntervalUnit.SECONDS)).thenReturn(true);
        when(limiter.tryAcquire()).thenReturn(true);

        assertTrue(util.tryAcquire("user:1", 10, 60));
        verify(limiter).tryAcquire();
    }

    @Test
    @DisplayName("tryAcquire:令牌耗尽返回 false")
    void tryAcquireDeny() {
        when(redisson.getRateLimiter("rl:user:2")).thenReturn(limiter);
        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(limiter.tryAcquire()).thenReturn(false);

        assertFalse(util.tryAcquire("user:2", 10, 60));
    }

    // ---------------- 原子计数器 ----------------

    @Test
    @DisplayName("incr / decr")
    void incrDecr() {
        when(redisson.getAtomicLong("cnt:agent:1:calls")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(5L);
        when(atomicLong.decrementAndGet()).thenReturn(4L);

        assertEquals(5L, util.incr("agent:1:calls"));
        assertEquals(4L, util.decr("agent:1:calls"));
    }

    @Test
    @DisplayName("incrWithExpire:自增 + 过期时间")
    void incrWithExpire() {
        when(redisson.getAtomicLong("cnt:user:1")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(1L);

        long v = util.incrWithExpire("user:1", 60);
        assertEquals(1L, v);
        verify(atomicLong).expire(60L, TimeUnit.SECONDS);
    }

    // ---------------- Pub/Sub ----------------

    @Test
    @DisplayName("publish:发送消息并返回订阅者数量")
    void publish() {
        when(redisson.getTopic("topic:agent.events")).thenReturn(topic);
        when(topic.publish(any())).thenReturn(3L);

        long n = util.publish("agent.events", "hello");
        assertEquals(3L, n);
        verify(topic).publish("hello");
    }

    // ---------------- 集合 ----------------

    @Test
    @DisplayName("set / map 透传")
    void collections() {
        when(redisson.getSet("set:t")).thenReturn(rSet);
        when(redisson.getMap("map:m")).thenReturn(rMap);
        assertSame(rSet, util.set("t"));
        assertSame(rMap, util.map("m"));
    }
}
