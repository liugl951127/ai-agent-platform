package com.platform.common.audit;

import java.lang.annotation.*;

/**
 * 操作审计注解
 * <p>
 * 配合 AuditLogAspect 自动记录:
 *   - 调用人 / 租户 / IP / UA
 *   - 模块 / 操作 / 资源 ID
 *   - 请求参数 (脱敏)
 *   - 响应 / 异常
 *   - 耗时
 * <p>
 * 用法:
 * <pre>
 *   &#64;AuditLog(module = "智能体", action = "CREATE", resourceType = "agent", resourceId = "#agent.id")
 *   &#64;PostMapping
 *   public R&lt;?&gt; save(&#64;RequestBody Agent agent) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 业务模块 */
    String module() default "";

    /** 操作类型: CREATE / UPDATE / DELETE / QUERY / EXPORT / LOGIN / LOGOUT */
    String action() default "";

    /** 资源类型 */
    String resourceType() default "";

    /** 资源 ID (支持 SpEL) */
    String resourceId() default "";

    /** 是否记录请求参数(默认 true) */
    boolean saveRequest() default true;

    /** 是否记录响应数据(默认 false,响应可能很大) */
    boolean saveResponse() default false;
}
