# AI Agent Platform

> 基于 Spring Cloud + Vue3 + Nacos + Flowable + MyBatis + MySQL + Elasticsearch + Redis 的**分布式大模型智能体平台**。
> 一键部署,开箱即用,支持多模型 / RAG / 工作流 / 工具调用 / 流式对话。

<p align="center">
  <a href="https://github.com/liugl951127/ai-agent-platform/stargazers">
    <img src="https://img.shields.io/github/stars/liugl951127/ai-agent-platform?style=social" alt="Stars"/>
  </a>
  <a href="https://github.com/liugl951127/ai-agent-platform/network/members">
    <img src="https://img.shields.io/github/forks/liugl951127/ai-agent-platform?style=social" alt="Forks"/>
  </a>
  <a href="https://github.com/liugl951127/ai-agent-platform/watchers">
    <img src="https://img.shields.io/github/watchers/liugl951127/ai-agent-platform?style=social" alt="Watchers"/>
  </a>
</p>

<p align="center">
  <img src="https://github.com/liugl951127/ai-agent-platform/actions/workflows/backend-ci.yml/badge.svg" alt="Backend CI"/>
  <img src="https://github.com/liugl951127/ai-agent-platform/actions/workflows/frontend-ci.yml/badge.svg" alt="Frontend CI"/>
  <img src="https://img.shields.io/github/license/liugl951127/ai-agent-platform" alt="License"/>
  <img src="https://img.shields.io/badge/Java-17-blue" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Vue-3.4-success" alt="Vue"/>
</p>

## 🏛️ 架构图

```
                       ┌──────────────────┐
                       │   agent-ui (Vue) │  :8080
                       └─────────┬────────┘
                                 │ /api
                       ┌─────────▼────────┐
                       │  agent-gateway   │  :9000  (JWT 鉴权)
                       └─────────┬────────┘
       ┌────────────┬────────────┼────────────┬────────────┐
       │            │            │            │            │
   agent-auth   agent-system  agent-llm  agent-workflow  agent-knowledge
   :9001         :9007        :9002      :9003          :9004
                                          │
                                  ┌───────┴───────┐
                              agent-agent  agent-conversation
                                :9005         :9006
       ┌────────────────────┴────────────────────┐
       │                                         │
   ┌───▼────┐  ┌──────┐  ┌──────┐  ┌────────┐
   │  MySQL │  │Redis │  │  ES  │  │ Nacos  │
   └────────┘  └──────┘  └──────┘  └────────┘
```

> 详细架构说明:[docs/architecture.md](./docs/architecture.md)

## ✨ 核心能力

| 能力 | 说明 |
|---|---|
| 🧠 **多模型支持** | OpenAI / Ollama / 通义千问 / DeepSeek 统一抽象(`LlmProvider` 接口,可一行扩展) |
| 🤖 **智能体编排** | ReAct 推理 + Function Call 工具反射调用 + 多轮记忆 |
| 🔄 **工作流引擎** | Flowable BPMN 2.0 流程编排,启动时自动部署示例流程 |
| 📚 **知识库 RAG** | Elasticsearch 8 + Spring Data ES,NativeQuery 检索 |
| 🌐 **分布式微服务** | Spring Cloud Gateway + Nacos 注册/配置中心 + OpenFeign 通信 |
| 🔐 **统一鉴权** | JWT (jjwt 0.12) + 网关全局过滤器 + Redis Token 缓存 |
| 💬 **流式对话** | WebFlux SSE 流式输出,前端 `ReadableStream` 增量渲染 |
| 📖 **API 文档** | Knife4j (Swagger 增强 UI),每个服务自带 `/doc.html` |
| 🎨 **现代前端** | Vue3.4 + Vite5 + Element Plus 2.7 + Pinia + Vue Router 4 |
| 🐳 **一键部署** | `docker compose` 一键起中间件 + 全套后端镜像 |

## 📸 效果预览

> 截图待补充 — 贡献指南见 [docs/screenshots/README.md](./docs/screenshots/README.md)

<!--
![Chat](./docs/screenshots/02-chat.png)
![Agents](./docs/screenshots/03-agents.png)
![Knife4j](./docs/screenshots/08-knife4j.png)
-->

## 🚀 快速开始

### 方式 A:本地开发(5 分钟)

```bash
# 1. 起中间件
docker compose up -d

# 2. 编译并启动后端 (各模块单独启,或写个脚本一键起)
mvn -pl agent-gateway      spring-boot:run
mvn -pl agent-auth         spring-boot:run
mvn -pl agent-llm          spring-boot:run
mvn -pl agent-workflow     spring-boot:run
mvn -pl agent-knowledge    spring-boot:run
mvn -pl agent-agent        spring-boot:run
mvn -pl agent-conversation spring-boot:run

# 3. 启前端
cd agent-ui && npm i && npm run dev

# 浏览器打开 http://localhost:8080,admin/123456 登录
```

### 方式 B:全 Docker 编排

```bash
# 一次性构建 8 个微服务镜像
for m in agent-gateway agent-auth agent-llm agent-workflow \
         agent-knowledge agent-agent agent-conversation agent-system; do
  docker build -t $m:latest -f $m/Dockerfile .
done

# 启动全套(中间件 + 8 个微服务)
docker compose -f docker-compose.app.yml up -d
```

详细部署文档:[docs/deployment.md](./docs/deployment.md)

## 🏗️ 项目结构

```
ai-agent-platform/
├── pom.xml                          # 父 POM
├── docker-compose.yml               # 中间件
├── docker-compose.app.yml           # 中间件 + 8 个微服务全套编排
├── Dockerfile                       # 通用多阶段构建模板
├── sql/init.sql                     # MySQL 初始化脚本
├── docs/                            # 设计/部署/架构文档
├── .github/                         # CI / Issue 模板
├── agent-common/                    # 公共模块 (R/BaseEntity/Jwt/Swagger)
├── agent-gateway/                   # Spring Cloud Gateway
├── agent-auth/                      # 鉴权服务
├── agent-system/                    # 系统管理
├── agent-llm/                       # 大模型调度 (OpenAI/Ollama/Qwen)
├── agent-workflow/                  # Flowable 工作流引擎
├── agent-knowledge/                 # ES 知识库
├── agent-conversation/              # 对话/会话
├── agent-agent/                     # 智能体编排 (ReAct)
└── agent-ui/                        # Vue3 前端
```

## 🛠️ 技术栈

| 类别 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2.5 / Spring Cloud 2023.0.1 / Spring Cloud Alibaba 2023.0.1 |
| 服务治理 | Nacos 2.3 / Spring Cloud Gateway / OpenFeign / Sentinel (待接入) |
| 持久层 | MyBatis-Plus 3.5.5 / MySQL 8.0 / HikariCP |
| 缓存 | Redis 7.2 (Spring Data Redis) |
| 搜索引擎 | Elasticsearch 8.11 (Spring Data ES + NativeQuery) |
| 工作流 | Flowable 7.0.1 (BPMN 2.0) |
| 安全 | Spring Security Crypto / JWT (jjwt 0.12) |
| API 文档 | Knife4j 4.5 (Swagger 增强 UI) |
| 工具库 | Hutool 5.8 / Lombok |
| 前端 | Vue 3.4 / Vite 5 / Element Plus 2.7 / Pinia / Vue Router 4 |
| HTTP | Axios + ReadableStream 流式 |
| CI/CD | GitHub Actions (Maven + Node 20) |
| 容器化 | Docker + Docker Compose (Alpine + Temurin 17) |

## 📝 接口示例

```bash
# 登录拿 token
TOKEN=$(curl -s "http://localhost:9000/auth/login?username=admin&password=123456" | jq -r .data)

# 新增大模型 (Ollama 本地)
curl -X POST http://localhost:9000/llm/add \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Ollama","provider":"OLLAMA","modelName":"llama3","apiBase":"http://host.docker.internal:11434","temperature":0.7,"maxTokens":2048}'

# 智能体对话 (RAG + Function Call)
curl -X POST "http://localhost:9000/agent/chat?agentId=1&input=北京今天几度" \
  -H "Authorization: Bearer $TOKEN"

# 工作流启动
curl -X POST "http://localhost:9000/workflow/start/agentChat" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"modelId":1,"input":"北京今天几度"}'

# 知识库检索
curl -X POST "http://localhost:9000/knowledge/search?kbId=1&q=RAG" \
  -H "Authorization: Bearer $TOKEN"
```

## 📖 API 文档

启动后端后,访问任意服务的 `/doc.html`:

- http://localhost:9001/doc.html (agent-auth)
- http://localhost:9002/doc.html (agent-llm)
- http://localhost:9005/doc.html (agent-agent)

Knife4j 提供接口调试、参数示例、响应格式、实体类列表等。

## 🤝 贡献指南

1. Fork 本仓库
2. 创建分支 (`git checkout -b feat/amazing-feature`)
3. 提交代码 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feat/amazing-feature`)
5. 提交 Pull Request

提交前请确保:
- 遵循 `.editorconfig` 风格
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)
- 在对应模块新增/更新单元测试
- 更新对应文档 (`docs/` 或 `README.md`)

## ⭐ Star History

如果这个项目对你有帮助,**点个 ⭐ Star** 是对我最大的支持!

<a href="https://star-history.com/#liugl951127/ai-agent-platform&Date">
  <picture>
    <img media="(prefers-color-scheme: dark)" src="https://api.star-history.com/svg?repos=liugl951127/ai-agent-platform&type=Date&theme=dark" />
    <img media="(prefers-color-scheme: light)" src="https://api.star-history.com/svg?repos=liugl951127/ai-agent-platform&type=Date" />
  </picture>
</a>

## 📄 License

[MIT](./LICENSE)
