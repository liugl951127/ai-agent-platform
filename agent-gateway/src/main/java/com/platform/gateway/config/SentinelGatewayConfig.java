package com.platform.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Sentinel 网关限流配置
 * <p>
 * 由 spring-cloud-alibaba-sentinel-gateway starter 自动接管:
 *   - 限流规则通过 Nacos / 配置文件加载 (application.yml 的 spring.cloud.sentinel.datasource.*)
 *   - 兜底返回: 429
 * <p>
 * 本类提供 BlockRequestHandler (SCA 自动注册) 和 GatewayFilter
 */
@Configuration
public class SentinelGatewayConfig {

    @Bean
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }
}
