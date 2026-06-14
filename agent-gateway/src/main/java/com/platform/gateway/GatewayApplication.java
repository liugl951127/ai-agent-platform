package com.platform.gateway;

import com.platform.common.security.JwtUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * API Gateway (Spring Cloud Gateway / WebFlux)
 * <p>
 * 路由各微服务的统一入口,JWT 鉴权、限流、灰度都在这里。
 * <p>
 * 注意: gateway 用的是 WebFlux (响应式), 不能直接用 servlet 相关的拦截器。
 * 所以这里用 @Import 显式导入 JwtUtil, 不扫 com.platform.common
 * (common 里的 TenantInterceptor / AuditLogAspect 等是 servlet 的)
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import(JwtUtil.class)
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
