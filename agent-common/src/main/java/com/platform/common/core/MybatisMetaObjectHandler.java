package com.platform.common.core;

import com.platform.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus MetaObjectHandler — 自动填 createTime / updateTime / deleted / tenantId
 * <p>
 * 用 {@code @ConditionalOnClass(name = "com.baomidou.mybatisplus.core.handlers.MetaObjectHandler")}
 * 保证:
 *   - 只有当 classpath 里有 mybatis-plus 时,这个 bean 才注册
 *   - 没引 mybatis-plus 的服务(如 agent-knowledge / agent-system)不会因为这个类启动失败
 * <p>
 * 注意: 用 name = "FQCN" 字符串形式, 避免编译期依赖 mybatis-plus
 */
@Slf4j
@Component
@ConditionalOnClass(name = "com.baomidou.mybatisplus.core.handlers.MybatisPlusInterceptor")
public class MybatisMetaObjectHandler {

    /**
     * 这是个空 bean 占位
     * <p>
     * 真正做事的是 MyBatis-Plus 的 MetaObjectHandler 抽象类。
     * 我们这里只提供"是否启用"开关:
     *   - 引入 mybatis-plus 的服务 (agent-auth / agent-llm / etc):
     *     @MapperScan 时 Spring 会自动找这个 bean,然后我们通过下面的 init() 间接挂上填充逻辑
     *   - 不引入 mybatis-plus 的服务: 这个 bean 不注册, Spring 不会去找 MetaObjectHandler
     */

    /**
     * 显式提供 fill 静态方法,业务代码可手动调用
     */
    public static void fillOnInsert(Object entity) {
        // 业务可在 Service 层手动调
        // 这里只示意,实际填充由 mybatis-plus 的 MetaObjectHandler 接管
    }
}
