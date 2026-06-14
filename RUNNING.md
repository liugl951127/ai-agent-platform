# RUNNING — 各服务独立启动指南

> 9 个微服务,每个都可以**独立**启动,只需满足它自己的依赖。

## 启动矩阵一览

| 服务 | 端口 | Nacos | MySQL | Redis | ES | Seata | Ollama | Flowable | agent-llm | agent-knowledge |
|---|---|---|---|---|---|---|---|---|---|---|
| **agent-gateway**       | 9000 | ✅ | | | | | | | | |
| **agent-auth**          | 9001 | ✅ | ✅ | ✅ | | ⚪ | | | | |
| **agent-system**        | 9007 | ✅ | | | | | | | | |
| **agent-llm**           | 9002 | ✅ | ✅ | | | | ⚪ | | | |
| **agent-workflow**      | 9003 | ✅ | ✅ | | | | | ✅(自动) | | |
| **agent-knowledge**     | 9004 | ✅ | | | ✅ | | | | | |
| **agent-conversation**  | 9006 | ✅ | ✅ | | | ⚪ | | | | |
| **agent-agent**         | 9005 | ✅ | ✅ | | | ⚪ | | | ⚪ | ⚪ |

- ✅ 必选
- ⚪ 可选(不起服务能启动,但相关功能不可用)

## 3 步启动任意单个服务

### 步骤 1:启动它需要的中间件(部分)

最小化启动某个服务时,**只**起它矩阵里 ✅ 的中间件。例如只跑 `agent-system`:

```bash
docker compose up -d nacos       # 只起 Nacos
```

### 步骤 2:编译整个项目(用 `-am` 自动构建依赖)

```bash
mvn -pl agent-system -am clean install -DskipTests
```

`-am` = "also-make",会**自动**先构建 `agent-common`(agent-system 的依赖)。

### 步骤 3:启动该服务

```bash
mvn -pl agent-system spring-boot:run
```

启动后:
```bash
curl http://localhost:9007/actuator/health
# {"status":"UP"}
```

## 推荐调试顺序(从轻到重)

按**依赖最少**到**依赖最多**的顺序,逐步验证整套系统:

| # | 启动什么 | 验证什么 | 难度 |
|---|---|---|---|
| 1 | `agent-system` | Spring Cloud 链路通,Nacos 注册 | ⭐ |
| 2 | `agent-gateway` | Gateway 路由通,JWT 鉴权生效 | ⭐⭐ |
| 3 | `agent-llm` | LLM 调通 (需 Ollama 或 OpenAI Key) | ⭐⭐ |
| 4 | `agent-knowledge` | ES 索引/搜索通 | ⭐⭐ |
| 5 | `agent-auth` | 登录拿到 token | ⭐⭐ |
| 6 | `agent-workflow` | Flowable 流程跑通 | ⭐⭐⭐ |
| 7 | `agent-conversation` | 会话存储查 | ⭐⭐ |
| 8 | `agent-agent` | ReAct 智能体对话通 | ⭐⭐⭐⭐ |

## 完整启动(开发环境)

```bash
# 1. 起所有中间件
docker compose up -d nacos mysql redis elasticsearch

# 2. 初始化 MySQL
mysql -h localhost -uroot -p < sql/init.sql

# 3. 启所有服务 (按顺序,每个开一个终端)
mvn -pl agent-gateway      spring-boot:run   # 9000
mvn -pl agent-auth         spring-boot:run   # 9001
mvn -pl agent-llm          spring-boot:run   # 9002
mvn -pl agent-workflow     spring-boot:run   # 9003
mvn -pl agent-knowledge    spring-boot:run   # 9004
mvn -pl agent-agent        spring-boot:run   # 9005
mvn -pl agent-conversation spring-boot:run   # 9006
mvn -pl agent-system       spring-boot:run   # 9007
```

## 完整启动(生产环境)

```bash
# 用 Docker Compose 起所有 (中间件 + 8 个微服务镜像)
docker compose -f docker-compose.app.yml up -d
```

详见 [docs/deployment.md](./docs/deployment.md)

## 验证脚本

```bash
# 1. 所有服务健康检查
for port in 9000 9001 9002 9003 9004 9005 9006 9007; do
  echo "=== $port ==="
  curl -s -m 3 http://localhost:$port/actuator/health
  echo ""
done

# 2. 登录 + 列出智能体 (端到端)
TOKEN=$(curl -s "http://localhost:9000/auth/login?username=admin&password=123456" | jq -r .data)
curl http://localhost:9005/agent/list -H "Authorization: Bearer $TOKEN"
```

## 排错

服务起不来时,看启动日志的 ERROR 行,**90% 是下面之一**:

1. **Nacos 连不上** → 检查 `docker compose ps nacos` 状态
2. **MySQL 连不上** → 检查 `docker compose ps mysql` 状态,跑 init.sql
3. **ES 连不上** → `docker compose ps elasticsearch`,看启动日志
4. **Redis 连不上** → `docker compose ps redis`
5. **类找不到** → 重新 `mvn clean install -pl <模块> -am`
6. **端口被占** → `netstat -ano | findstr <端口>` (Windows) / `lsof -i :<端口>` (Linux)
7. **循环依赖** → 看是不是 `agent-A` 用了 `agent-B` 的类,`agent-B` 又用了 `agent-A` 的类(我前面已经把 `agent-agent` 的 LlmRouter 改成 Feign,避免这种问题)

具体每个服务的详细排错见各自的 `README.md`。
