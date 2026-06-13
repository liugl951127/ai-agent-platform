# Sentinel 限流 & Seata 分布式事务

## Sentinel 限流

### 架构

```
[Client] --HTTP--> [Gateway SentinelFilter] --路由--> [Microservice]
                            │  网关层 URI 维度限流
                            │  /agent/** ≤ 20 QPS
                            │  /llm/**   ≤ 10 QPS
                            ▼
                  [Sentinel Dashboard] :8080
                            ▲
                            │  客户端心跳
                  [Microservice @SentinelResource]
                            │  方法级资源限流
                            │  llm:chat:byId / agent:react:run ...
```

### 限流规则(可在 Sentinel Dashboard 动态改)

| 资源 | 类型 | 阈值 | 位置 |
|---|---|---|---|
| `/agent/**` | 网关 QPS | 20 | `SentinelGatewayConfig.initRules()` |
| `/llm/**` | 网关 QPS | 10 | 同上 |
| `/workflow/**` | 网关 QPS | 5 | 同上 |
| `/chat/**` | 网关 QPS | 20 | 同上 |
| `/knowledge/**` | 网关 QPS | 15 | 同上 |
| `llm:stream:byId` | 方法 QPS | 20 (默认) | `LlmRouter.streamById` |
| `llm:chat:byId` | 方法 QPS | 20 (默认) | `LlmRouter.chatById` |
| `agent:react:run` | 方法 QPS | 5 (默认) | `ReactExecutor.run` |

### 接入新服务的 3 步

1. 加依赖(`agent-common` 已包含,直接继承):
   ```xml
   <dependency>
     <groupId>com.alibaba.cloud</groupId>
     <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
   </dependency>
   ```

2. `application.yml` 加配置:
   ```yaml
   spring.cloud.sentinel:
     transport:
       port: 8719
       dashboard: sentinel-dashboard:8080
   ```

3. 在需要保护的方法上加注解:
   ```java
   @SentinelResource(value = "my:resource", blockHandler = "myBlock")
   public Foo bar(...) { ... }
   public Foo myBlock(..., BlockException e) { return fallback; }
   ```

### 限流兜底(全局)

`agent-common/SentinelConfig.handleBlock` 统一把 `BlockException` 转成 `R.fail(429)` 返回。

### 启动 Sentinel Dashboard

```bash
docker compose -f docker-compose.app.yml up -d sentinel-dashboard
# 浏览器: http://localhost:8080  (账号 sentinel / sentinel)
```

---

## Seata 分布式事务

### 架构(AT 模式)

```
[agent-agent]    [agent-llm]    [agent-knowledge]
     │               │                │
  本地写           跨服务写          跨服务写
     │               │                │
  ┌──▼───────────────▼────────────────▼──┐
  │       Seata Server (TC)               │
  │       - 全局事务 begin/commit/rollback │
  │       - 协调各 RM undo_log            │
  └────────────────────────────────────────┘
                       │
                  MySQL: undo_log
```

### 部署 Seata Server

#### 方式 1:用项目里 docker-compose(已包含)
```bash
docker compose -f docker-compose.app.yml up -d seata-server
# 端口 8091
```

#### 方式 2:file 模式(默认,适合开发)
- 不用额外配置,所有事务日志写本地文件
- 适合单机演示

#### 方式 3:db 模式(推荐生产)
1. 把 `sql/seata.sql` 跑进 seata 库
2. 挂载自定义配置:
   ```yaml
   seata-server:
     volumes:
       - ./deploy/seata/conf:/seata-server/conf
   ```
3. `registry.conf` / `file.conf` 模板见 Seata 官方文档

### 客户端接入 4 步

1. 加依赖(`agent-common` 已包含)
2. `application.yml` 加 `seata.*` 配置(已配好)
3. 在 MySQL 业务库执行 `sql/seata.sql` 的 `undo_log` 表
4. 在入口方法加 `@GlobalTransactional`:
   ```java
   @GlobalTransactional(name = "agent-platform-tx-group", rollbackFor = Exception.class)
   public Long createAgentWithConfig(AgentInfo agent) {
       agentMapper.insert(agent);              // 本服务
       llmClient.detail(agent.getModelId());   // 跨服务(Feign 自动分支)
       // ... 任意失败全部回滚
   }
   ```

### 注意事项

- AT 模式要求**每个 RM 都有 undo_log 表**(`sql/seata.sql` 已建)
- 全局事务超时默认 60s,可在 `client.rm.lock.retryPolicy` 调
- Feign 自动透传 XID,不需要手动塞请求头
- **不要**在 `@GlobalTransactional` 里 try-catch 后吞异常,必须抛出才能触发回滚
- 跨服务调用若用 RestTemplate,需要在调用方 `RootContext.getXID()` 手动塞 header
