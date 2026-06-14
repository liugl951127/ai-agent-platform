package com.platform.common.redisson.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 * <p>
 * 拦截 &#64;DistributedLock 注解,自动加锁 → 执行方法 → 释放锁。
 * Redisson 看门狗机制(leaseTime = 0 时)会每 10s 续期,避免业务未完成锁就过期。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
public class DistributedLockAspect {

    private final RedissonClient redisson;

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.platform.common.redisson.lock.DistributedLock)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        DistributedLock anno = method.getAnnotation(DistributedLock.class);

        String key = parseKey(anno.key(), method, pjp.getArgs());
        String fullKey = anno.prefix() + key;
        RLock lock = redisson.getLock(fullKey);

        boolean acquired = false;
        try {
            if (anno.leaseTime() > 0) {
                acquired = lock.tryLock(anno.waitTime(), anno.leaseTime(), anno.unit());
            } else {
                // 看门狗模式:leaseTime=-1,Redisson 内部默认 30s 自动续期
                acquired = lock.tryLock(anno.waitTime(), -1, TimeUnit.MILLISECONDS);
            }
            if (!acquired) {
                log.warn("分布式锁获取失败: key={}, waitTime={}s", fullKey, anno.waitTime());
                throw new DistributedLockException(anno.failMessage());
            }
            log.debug("分布式锁获取成功: key={}", fullKey);
            return pjp.proceed();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                try { lock.unlock(); log.debug("分布式锁释放: key={}", fullKey); }
                catch (Exception e) { log.warn("分布式锁释放失败: key={}, err={}", fullKey, e.getMessage()); }
            }
        }
    }

    /** 解析 SpEL 表达式,从方法参数中取值 */
    private String parseKey(String spel, Method method, Object[] args) {
        if (!spel.contains("#")) return spel;
        EvaluationContext ctx = new StandardEvaluationContext();
        String[] names = NAME_DISCOVERER.getParameterNames(method);
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
        }
        Expression expr = PARSER.parseExpression(spel);
        Object val = expr.getValue(ctx);
        return val == null ? "null" : val.toString();
    }
}
