package com.platform.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.platform.common.tenant.TenantSqlInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 统一配置
 * <p>
 * 各服务只需在 @MapperScan 指向自己的 mapper 包,本配置自动接管。
 * 启用:
 *   - 分页插件
 *   - 多租户 SQL 重写(TenantSqlInterceptor)
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 多租户 SQL 重写
        interceptor.addInnerInterceptor(new TenantSqlInterceptor());
        return interceptor;
    }
}
