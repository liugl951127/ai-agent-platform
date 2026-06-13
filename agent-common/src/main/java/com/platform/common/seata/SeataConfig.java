package com.platform.common.seata;

import org.springframework.context.annotation.Configuration;

/**
 * Seata 分布式事务自动配置占位
 * <p>
 * 各服务只需在 application.yml 中加 seata.* 配置(下面有模板),
 * seata-spring-boot-starter 会自动接管 DataSource。
 * <p>
 * 使用方式:
 * <pre>
 *   &#64;GlobalTransactional(name = "agent-platform-tx-group", rollbackFor = Exception.class)
 *   public void createAgentWithConfig(...) { ... }
 * </pre>
 */
@Configuration
public class SeataConfig {
    // 自动配置由 seata-spring-boot-starter 完成
    // 这里只放共享常量 / 工具方法(若有)
}
