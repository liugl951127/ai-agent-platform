package com.platform.common.tenant;

/**
 * 多租户上下文(基于 ThreadLocal)
 * <p>
 * 用法:
 *   - 在拦截器 / 网关 / Controller 入口:  TenantContext.setTenantId(...)
 *   - 在 MyBatis 拦截器:                    TenantContext.getTenantId() → 拼到 SQL
 *   - 在 Service:                          TenantContext.getTenantId()  → 业务校验
 *   - 在请求出口:                          TenantContext.clear()
 */
public final class TenantContext {
    private TenantContext() {}

    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_CODE = new ThreadLocal<>();

    public static void setTenantId(Long id)   { TENANT.set(id); }
    public static Long   getTenantId()         { return TENANT.get(); }
    public static void   setTenantCode(String c) { TENANT_CODE.set(c); }
    public static String getTenantCode()       { return TENANT_CODE.get(); }
    public static void   clear() {
        TENANT.remove();
        TENANT_CODE.remove();
    }
}
