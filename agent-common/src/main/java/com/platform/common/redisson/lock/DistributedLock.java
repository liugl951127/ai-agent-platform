package com.platform.common.redisson.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * <p>
 * 配合 {@link DistributedLockAspect} 在方法执行前后自动加锁 / 释放锁。
 * <p>
 * 用法:
 * <pre>
 *   &#64;DistributedLock(key = "agent:chat:#{#agentId}", waitTime = 3, leaseTime = 30)
 *   public String chat(Long agentId, String input) { ... }
 * </pre>
 *
 * SpEL 占位符支持: SpEL 表达式访问方法参数,例如 "#userId"、"#agentId"
 * 完整 key 形如: "lock:agent:chat:42"
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /** 锁 key(支持 SpEL,以 # 开头) */
    String key();

    /** 锁前缀,默认 "lock:" */
    String prefix() default "lock:";

    /** 等待锁超时 (秒) - -1 表示不等待,直接抛异常 */
    int waitTime() default 3;

    /** 锁自动释放时间 (秒) - 0 表示不自动释放(直到 unlock) */
    int leaseTime() default 30;

    /** 时间单位,默认秒 */
    TimeUnit unit() default TimeUnit.SECONDS;

    /** 获取锁失败时的提示 */
    String failMessage() default "系统繁忙,请稍后再试";
}
