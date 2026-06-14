# agent-workflow

Flowable BPMN 2.0 工作流引擎服务。

## 端口
9003

## 启动所需中间件
| 中间件 | 端口 | 必选? |
|---|---|---|
| Nacos Server | 8848 | ✅ 必选 |
| MySQL | 3306 | ✅ 必选 (Flowable 自动建 30+ 张 ACT_*/FLW_* 表) |

## 启动命令
```bash
mvn -pl agent-workflow -am spring-boot:run
```

## 启动前准备
```bash
# 1. 启中间件
docker compose up -d nacos mysql

# 2. 初始化 MySQL (注意 Flowable 会自动建表,只需建库即可)
mysql -h localhost -uroot -p -e "CREATE DATABASE IF NOT EXISTS agent_platform"
```

## 验证
```bash
# 1. 健康检查
curl http://localhost:9003/actuator/health

# 2. 启动流程实例 (需要 token)
TOKEN=$(curl -s "http://localhost:9000/auth/login?username=admin&password=123456" | jq -r .data)
curl -X POST "http://localhost:9003/workflow/start/agentChat" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"modelId":1,"input":"测试"}'

# 3. 查 MySQL,确认 Flowable 表建了
mysql -uroot -p -e "USE agent_platform; SHOW TABLES LIKE 'ACT_%';"
```

## 常见启动错误

| 错误 | 原因 | 修复 |
|---|---|---|
| `Unknown database 'agent_platform'` | MySQL 没建库 | `CREATE DATABASE agent_platform` |
| `Table 'agent_platform.ACT_RE_PROCDEF' doesn't exist` | Flowable 没权限建表 | 用 root 账号或给权限 |
| `FlowableException: Could not update database schema` | MySQL 版本不兼容 | 用 MySQL 8.0+ |

## 依赖关系图
```
agent-workflow (9003)
   ↓ 依赖
agent-common (R)
   ↓
Nacos + MySQL (Flowable 自动建表)
```
