package com.platform.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import java.sql.Connection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多租户 MyBatis 拦截器
 * <p>
 * 自动给所有 SELECT / UPDATE / DELETE 语句追加 WHERE tenant_id = ?
 * <p>
 * 用法: 在各服务的 MybatisPlusConfig 中注册:
 *   interceptor.addInnerInterceptor(new TenantSqlInterceptor());
 * <p>
 * 跳过机制:
 *   - 当前线程未设置 TenantContext (跨线程 / 内部调用) → 跳过
 *   - SQL 中已包含 tenant_id → 跳过(避免重复)
 *   - 主键 ID 查询(按 id)→ 不强制,只追加 where
 */
@Slf4j
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TenantSqlInterceptor implements Interceptor {

    private static final Pattern TENANT_ID_IN_SQL = Pattern.compile("tenant_id", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_CLAUSE = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // 没设置租户 → 不动 SQL(可能是内部调用 / 系统任务)
            return invocation.proceed();
        }

        StatementHandler handler = (StatementHandler) invocation.getTarget();
        MetaObject meta = SystemMetaObject.forObject(handler);
        MappedStatement ms = (MappedStatement) meta.getValue("delegate.mappedStatement");
        if (ms == null) return invocation.proceed();

        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql();
        if (TENANT_ID_IN_SQL.matcher(sql).find()) {
            // 已有 tenant_id,不动(可能业务自己写了)
            return invocation.proceed();
        }
        if (!isReadOrWriteSql(sql)) {
            return invocation.proceed();
        }

        String newSql = appendTenantCondition(sql, tenantId);
        // 改写 SQL
        meta.setValue("delegate.boundSql.sql", newSql);
        log.debug("TenantSqlInterceptor: rewrite SQL for tenantId={}: {}", tenantId, newSql);
        return invocation.proceed();
    }

    private boolean isReadOrWriteSql(String sql) {
        String head = sql.trim().substring(0, 6).toUpperCase();
        return head.startsWith("SELECT") || head.startsWith("UPDATE") || head.startsWith("DELETE");
    }

    private String appendTenantCondition(String sql, Long tenantId) {
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        if (WHERE_CLAUSE.matcher(trimmed).find()) {
            return trimmed + " AND tenant_id = " + tenantId;
        } else {
            return trimmed + " WHERE tenant_id = " + tenantId;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {}
}
