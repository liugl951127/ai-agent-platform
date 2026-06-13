package com.platform.common.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置
 * <p>
 * 读取 application.yml 中的 spring.redis 配置 + redisson.config 自定义 JSON。
 * 当 redisson.config 为空时,根据 spring.redis 推断单点/集群/哨兵模式。
 * <p>
 * 单点示例 (application.yml):
 * <pre>
 *   spring:
 *     redis:
 *       host: localhost
 *       port: 6379
 *       password:           # 留空表示无密码
 *       database: 0
 * </pre>
 */
@Configuration
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

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config;
        if (customConfig != null && !customConfig.isBlank()) {
            // 优先用 yml 中 redisson.config 提供的 JSON / YAML
            config = Config.fromJSON(customConfig);
        } else {
            config = defaultSingleConfig();
        }
        return Redisson.create(config);
    }

    private Config defaultSingleConfig() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout((int) parseMs(timeout))
                .setTimeout((int) parseMs(timeout));
        if (password != null && !password.isBlank()) {
            config.useSingleServer().setPassword(password);
        }
        return config;
    }

    private long parseMs(String t) {
        if (t == null || t.isBlank()) return 3000L;
        return t.endsWith("ms") ? Long.parseLong(t.substring(0, t.length() - 2))
                                : Long.parseLong(t.replace("s", ""));
    }
}
