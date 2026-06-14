# agent-agent

智能体编排服务 (ReAct + Function Call + 工具调用)。

## 端口
9005

## 启动所需中间件
| 中间件 | 端口 | 必选? |
|---|---|---|
| Nacos Server | 8848 | ✅ 必选 |
| MySQL | 3306 | ✅ 必选 (agent_info / agent_tool 表) |
| agent-llm (服务) | 9002 | ⚠️ 可选 (没起则 /agent/chat 调用失败) |
| agent-knowledge (服务) | 9004 | ⚠️ 可选 (RAG 走 HTTP 调用,失败降级) |
| Seata Server | 8091 | ⚠️ 可选 |

## 启动命令
```bash
mvn -pl agent-agent -am spring-boot:run
```

## 启动前准备
```bash
# 1. 启中间件
docker compose up -d nacos mysql

# 2. 初始化库 (含默认 agent/llm/tool 示例数据)
mysql -h localhost -uroot -p < sql/init.sql
```

## 验证
```bash
# 1. 健康检查
curl http://localhost:9005/actuator/health

# 2. 列出智能体
curl http://localhost:9005/agent/list

# 3. 对话 (需要 agent-llm 也启着)
TOKEN=$(curl -s "http://localhost:9000/auth/login?username=admin&password=123456" | jq -r .data)
curl -X POST "http://localhost:9005/agent/chat?agentId=1&input=你好" \
  -H "Authorization: Bearer $TOKEN"

# 4. 分布式锁版对话 (需要 Redis)
curl -X POST "http://localhost:9005/agent/chat/safe?agentId=1&input=你好" \
  -H "Authorization: Bearer $TOKEN" -H "X-User-Id: 1"
```

## 常见启动错误

| 错误 | 原因 | 修复 |
|---|---|---|
| `FeignException: agent-llm` | agent-llm 没起 | 启 agent-llm,或不调 /agent/chat |
| `Table 'agent_platform.agent_info' doesn't exist` | 没跑 init.sql | 跑 init.sql |
| `RedissonConnectionException` | Redis 没起 | 启 Redis |

## 依赖关系图
```
agent-agent (9005)
   ↓ 依赖
agent-common (R / RedissonUtil / @DistributedLock)
   ↓
Nacos + MySQL
   ↓ Feign HTTP
agent-llm (9002) + agent-knowledge (9004) (可选)
```
