package com.platform.common.redisson;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置 (反射初始化版)
 * <p>
 * 通过 {@code @ConditionalOnClass(name = "org.redisson.Redisson")} 保护:
 *   - 没引 redisson 的服务 (agent-system / agent-knowledge) 启动时这个 @Configuration
 *     不被注册, 不会因为 import 失败而启动崩溃
 *   - 引了 redisson 的服务 (agent-auth / agent-agent) 才加载并反射初始化 RedissonClient
 * <p>
 * 业务代码注入 {@code RedissonClient} 时, 若本服务没引 redisson, Spring 会报
 * NoSuchBeanDefinitionException — 这是正确的反馈
 * <p>
 * 用反射而非直接 import 的原因: 避免 agent-common 在没 redisson 依赖时编译失败
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.redisson.Redisson")
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String host;
    @Value("${spring.redis.port:6379}")
    private int port;
    @Value("${spring.redis.password:}")
    private String password;
    @Value("${spring.redis.database:0}")
    private int database;
    @Value("${spring.redis.timeout:3000ms}")
    private String timeout;
    @Value("${redisson.config:}")
    private String customConfig;

    /** 通过反射初始化的 RedissonClient 实例 (object 类型, 避免 import) */
    private Object redissonClient;

    @PostConstruct
    public void init() {
        try {
            Class<?> configClass = Class.forName("org.redisson.config.Config");
            Class<?> redissonClass = Class.forName("org.redisson.Redisson");
            Object config;
            if (customConfig != null && !customConfig.isBlank()) {
                config = configClass.getMethod("fromJSON", String.class).invoke(null, customConfig);
            } else {
                config = configClass.getDeclaredConstructor().newInstance();
                Object singleConfig = configClass.getMethod("useSingleServer").invoke(config);
                singleConfig.getClass().getMethod("setAddress", String.class).invoke(singleConfig, "redis://" + host + ":" + port);
                singleConfig.getClass().getMethod("setDatabase", int.class).invoke(singleConfig, database);
                singleConfig.getClass().getMethod("setConnectionMinimumIdleSize", int.class).invoke(singleConfig, 4);
                singleConfig.getClass().getMethod("setConnectionPoolSize", int.class).invoke(singleConfig, 16);
                singleConfig.getClass().getMethod("setConnectTimeout", int.class).invoke(singleConfig, (int) parseMs(timeout));
                singleConfig.getClass().getMethod("setTimeout", int.class).invoke(singleConfig, (int) parseMs(timeout));
                if (password != null && !password.isBlank()) {
                    singleConfig.getClass().getMethod("setPassword", String.class).invoke(singleConfig, password);
                }
            }
            redissonClient = redissonClass.getMethod("create", configClass).invoke(null, config);
            log.info("RedissonClient 初始化成功 (反射方式): {}:{}", host, port);
        } catch (Exception e) {
            log.error("RedissonClient 初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (redissonClient != null) {
            try {
                redissonClient.getClass().getMethod("shutdown").invoke(redissonClient);
            } catch (Exception e) {
                log.warn("RedissonClient shutdown 失败: {}", e.getMessage());
            }
        }
    }

    public Object getRedissonClient() {
        return redissonClient;
    }

    private long parseMs(String t) {
        if (t == null || t.isBlank()) return 3000L;
        return t.endsWith("ms") ? Long.parseLong(t.substring(0, t.length() - 2))
                                : Long.parseLong(t.replace("s", ""));
    }
}
