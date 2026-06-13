# 多租户 / 审计 / 灰度 / 监控

> 状态:✅ 已集成

## 1. 多租户隔离 (Multi-Tenancy)

### 架构

```
[Client] --X-Tenant-Id--> [Gateway] --透传--> [Microservice]
                                                  │
                                          TenantInterceptor
                                          (解析并塞入 TenantContext)
                                                  │
                                  ┌───────────────┼───────────────┐
                                  ▼                               ▼
                          MyBatis MetaObjectHandler         TenantSqlInterceptor
                          (INSERT 时自动填 tenant_id)       (SELECT/UPDATE/DELETE 自动加 WHERE tenant_id=?)
```

### 接入 3 步

1. **SQL 加 tenant_id 列**:执行 `sql/migration_tenant_audit_gray.sql`
2. **HTTP Header 传 X-Tenant-Id**:网关透传(已在 AgentFilter / 网关层加)
3. **业务代码无需改动**:MyBatis 自动处理

### 手动指定租户(框架内部调用)

```java
TenantContext.setTenantId(2L);
try {
    agentMapper.selectList(null);   // SQL 自动追加 tenant_id = 2
} finally {
    TenantContext.clear();
}
```

### 跳过租户过滤

- 跨服务内部调用(Feign)→ 通过 `TenantContext` 透传
- 系统级任务(无租户上下文)→ 拦截器检测到 `null` 自动跳过,不会报错
- 已经在 SQL 里写了 `tenant_id` → 拦截器检测到关键词跳过,不会重复追加

---

## 2. 操作审计 (Audit)

### 用法

```java
@AuditLog(
    module = "智能体",
    action = "CREATE",
    resourceType = "agent",
    resourceId = "#{#agent.id}",   // SpEL
    saveRequest = true,
    saveResponse = false           // 默认 false,响应可能很大
)
@PostMapping("/save")
public R<?> save(@RequestBody AgentInfo agent) { ... }
```

### 自动记录字段

| 字段 | 来源 |
|---|---|
| tenantId / userId | TenantContext + X-User-Id header |
| method / url | HttpServletRequest |
| ip / ua | getRemoteAddr + User-Agent |
| requestArgs | 方法参数 JSON(敏感字段脱敏) |
| responseData | 可选 |
| costMs | 耗时 |
| status / errorMsg | 1=成功 / 0=失败 |
| createTime | LocalDateTime.now() |

### 敏感字段自动脱敏

`password / token / apiKey / secret / privateKey` 等 9 个字段自动替换为 `"******"`。

### 日志落地

默认写 JSON 行(便于 ELK 采集):
```
2024-xx AUDIT {"id":...,"tenantId":2,"action":"CREATE","status":1,...}
```

接入 DB:实现一个 `AuditLogWriter` Bean 替代 `AuditLogAspect.writeLog`,INSERT 到 `sys_audit_log` 表即可。

---

## 3. 灰度发布 (Gray Release)

### 概念

通过 `@GrayRelease` + 灰度规则,让**指定用户/租户/IP/比例**走新代码路径,其余走老路径。

### 用法

```java
@GrayRelease(resource = "/agent/chat")
public String chat(Long agentId, String input) {
    if (GrayContext.isGray()) {
        return newPathChat(agentId, input);  // 新逻辑
    }
    return oldPathChat(agentId, input);      // 老逻辑
}
```

### 灰度规则(在 `sys_gray_rule` 表)

| strategy | matchValue 例子 | 含义 |
|---|---|---|
| USER_ID | "100,200,300" | 这些 userId 命中 |
| TENANT_ID | "2" | 租户 2 命中 |
| IP | "192.168.1.1" | 客户端 IP 命中 |
| RATIO | "20" | 20% 流量命中(按 uid 哈希) |

### 规则管理

`GrayReleaseAspect.RULES` 是内存 Map(演示用)。生产替换为:
- 从 `sys_gray_rule` 表读
- 5 秒定时刷新
- 或监听 Nacos 配置变化动态更新

---

## 4. 监控 (Prometheus + Grafana)

### 启动

```bash
docker compose -f docker-compose.app.yml up -d prometheus grafana
```

### 访问

- **Prometheus**:http://localhost:9090
- **Grafana**:http://localhost:3000 (admin/admin)
  - 预置 dashboard: **AI Agent Platform - Overview**
  - 自动从 `/etc/grafana/provisioning` 加载 datasource + dashboard

### Spring Boot 暴露的指标

Spring Boot Actuator + micrometer-registry-prometheus 自动暴露:

- **HTTP**:`http_server_requests_seconds_count / _bucket / _sum`
- **JVM**:`jvm_memory_used_bytes / jvm_gc_pause_seconds_count / jvm_threads_states_threads`
- **Tomcat**:`tomcat_threads_busy / tomcat_connections_active`
- **系统**:`system_cpu_usage / system_load_average_1m`
- **业务**:`agent_chat_total / agent_chat_latency_seconds / llm_call_total / llm_call_latency_seconds / audit_log_total / gray_release_total`

### 业务指标埋点

```java
@Service
@RequiredArgsConstructor
public class AgentChatWithLockService {
    private final BusinessMetrics metrics;

    public R<String> chat(...) {
        metrics.agentChatTotal().increment();
        Timer.Sample s = Timer.start();
        try {
            String reply = react.run(agentId, input);
            return R.ok(reply);
        } finally {
            s.stop(metrics.agentChatLatency());
        }
    }
}
```

### Grafana 关键面板

- HTTP QPS / P99 / 错误率
- 智能体对话 QPS / P99 延迟
- LLM 调用(按 provider 拆)
- 审计日志(按模块拆)
- 灰度命中(按资源拆)
- JVM GC / 内存

---

## 5. 一键验证

```bash
# 1. 起全栈
docker compose -f docker-compose.app.yml up -d

# 2. 等 1 分钟,健康检查
curl http://localhost:9000/actuator/health
curl http://localhost:9090/api/v1/targets   # Prometheus Targets 全绿

# 3. 看指标
curl http://localhost:9000/actuator/prometheus | head -20

# 4. 触发一次对话
TOKEN=$(curl -s "http://localhost:9000/auth/login?username=admin&password=123456" | jq -r .data)
curl -X POST "http://localhost:9000/agent/chat/safe?agentId=1&input=hello" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: 1"

# 5. 看 Prometheus / Grafana 实时数据
open http://localhost:3000
```
