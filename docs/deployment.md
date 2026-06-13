# 部署指南

## 方式 1:本地开发(推荐先用这个)

```bash
# 1. 起中间件
docker compose up -d

# 2. 等 MySQL 容器跑完 init.sql (约 30s),查看日志:
docker compose logs -f mysql | grep "ready for connections"

# 3. 编译并启动后端 (各模块单独启,或写个脚本一键起)
mvn -pl agent-gateway      spring-boot:run
mvn -pl agent-auth         spring-boot:run
mvn -pl agent-llm          spring-boot:run
mvn -pl agent-workflow     spring-boot:run
mvn -pl agent-knowledge    spring-boot:run
mvn -pl agent-agent        spring-boot:run
mvn -pl agent-conversation spring-boot:run

# 4. 启前端
cd agent-ui && npm i && npm run dev

# 5. 浏览器打开 http://localhost:8080,admin/123456 登录
```

## 方式 2:全 Docker 编排

适合生产 / 演示 / 跨机器部署。

```bash
# 1. 在仓库根目录一次性构建所有镜像
for m in agent-gateway agent-auth agent-llm agent-workflow \
         agent-knowledge agent-agent agent-conversation agent-system; do
  docker build -t $m:latest -f $m/Dockerfile .
done

# 2. 启动全套(中间件 + 8 个微服务)
docker compose -f docker-compose.app.yml up -d

# 3. 验证
docker compose -f docker-compose.app.yml ps
curl http://localhost:9000/auth/login?username=admin\&password=123456

# 4. 看日志
docker compose -f docker-compose.app.yml logs -f agent-llm
```

## 方式 3:K8s 部署(进阶)

参考 `deploy/k8s/` 目录(待补充):
- namespace.yaml
- 每个微服务的 Deployment + Service
- Nacos / MySQL / Redis / ES 用 Helm 装

## 端口占用总览

| 服务 | 端口 | 协议 |
|---|---|---|
| agent-ui | 8080 | HTTP |
| agent-gateway | 9000 | HTTP |
| agent-auth | 9001 | HTTP |
| agent-llm | 9002 | HTTP |
| agent-workflow | 9003 | HTTP |
| agent-knowledge | 9004 | HTTP |
| agent-agent | 9005 | HTTP |
| agent-conversation | 9006 | HTTP |
| agent-system | 9007 | HTTP |
| Nacos | 8848 / 9848 | HTTP / gRPC |
| MySQL | 3306 | TCP |
| Redis | 6379 | TCP |
| Elasticsearch | 9200 | HTTP |

## 访问入口

| 入口 | URL |
|---|---|
| 前端 | http://localhost:8080 |
| 网关 | http://localhost:9000 |
| Nacos 控制台 | http://localhost:8848/nacos (nacos/nacos) |
| agent-llm API 文档 | http://localhost:9002/doc.html |
| agent-auth API 文档 | http://localhost:9001/doc.html |
| agent-agent API 文档 | http://localhost:9005/doc.html |

## 故障排查

### 启动后 Nacos 没看到服务
- 等 30s 再看,Spring Cloud 应用注册有延迟
- 检查 `application.yml` 中 `nacos.discovery.server-addr` 是不是 `localhost:8848`
- 如果是 Docker 网络,改成 `nacos:8848`

### ES 连接失败
- ES 8 默认开启 security,本项目 `xpack.security.enabled=false` 简化了
- 如果你换成了带认证的 ES,需在 `agent-knowledge/application.yml` 加 username/password

### Ollama 连接失败
- 宿主机跑 Ollama,容器内访问需用 `http://host.docker.internal:11434`
- Mac/Win 自动支持,Linux 需要加 `extra_hosts: ["host.docker.internal:host-gateway"]` 到 compose
