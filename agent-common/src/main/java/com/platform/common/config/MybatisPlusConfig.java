package com.platform.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 统一配置
 * <p>
 * 各服务只需在 @MapperScan 指向自己的 mapper 包,本配置自动接管。
 * <p>
 * 启用:
 *   - MyBatis-Plus 分页插件 (PaginationInnerInterceptor)
 * <p>
 * 多租户拦截器 (TenantSqlInterceptor) 用 @Intercepts 注解,MyBatis 自动扫描注册,
 * 不需要在 MybatisPlusInterceptor 里 addInnerInterceptor (因为它实现的是经典
 * org.apache.ibatis.plugin.Interceptor,不是 MP 的 InnerInterceptor)。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
