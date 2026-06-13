# AI Agent Platform

> 基于 Spring Cloud + Vue3 + Nacos + Flowable + MyBatis + MySQL + Elasticsearch + Redis 的分布式大模型智能体平台。

![Backend CI](https://github.com/liugl951127/ai-agent-platform/actions/workflows/backend-ci.yml/badge.svg)
![Frontend CI](https://github.com/liugl951127/ai-agent-platform/actions/workflows/frontend-ci.yml/badge.svg)
![License](https://img.shields.io/github/license/liugl951127/ai-agent-platform)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.4-success)

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

## ✨ 核心能力

- 🧠 **多模型支持**: OpenAI / Ollama / 通义千问 / DeepSeek (统一抽象,可扩展)
- 🤖 **智能体编排**: ReAct 推理 + Function Call 工具调用 + 多轮记忆
- 🔄 **工作流引擎**: Flowable BPMN 2.0 流程编排,可视化设计
- 📚 **知识库 RAG**: Elasticsearch 8 + IK 分词,语义检索
- 🌐 **分布式微服务**: Spring Cloud Gateway + Nacos 注册/配置中心
- 🔐 **统一鉴权**: JWT + 网关全局过滤器 + Redis Token
- 💬 **流式对话**: SSE / WebFlux 流式输出
- 🎨 **现代前端**: Vue3 + Vite + Element Plus + Pinia

## 🏗️ 项目结构

```
ai-agent-platform/
├── pom.xml                          # 父 POM
├── docker-compose.yml               # 中间件一键起
├── sql/init.sql                     # MySQL 初始化脚本
├── .github/                         # CI / Issue 模板
├── agent-common/                    # 公共模块 (R/BaseEntity/Jwt/Exception)
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

## 🚀 快速开始

### 1. 启动中间件
```bash
docker compose up -d
# - Nacos:   http://localhost:8848/nacos  (nacos/nacos)
# - MySQL:   localhost:3306 (root/root)
# - Redis:   localhost:6379
# - ES:      http://localhost:9200
```

### 2. 启动后端
```bash
# 父工程安装依赖到本地仓
mvn clean install -N

# 各模块单独启动
mvn -pl agent-gateway      spring-boot:run
mvn -pl agent-auth         spring-boot:run
mvn -pl agent-llm          spring-boot:run
mvn -pl agent-workflow     spring-boot:run
mvn -pl agent-knowledge    spring-boot:run
mvn -pl agent-agent        spring-boot:run
mvn -pl agent-conversation spring-boot:run
```

### 3. 启动前端
```bash
cd agent-ui
npm install
npm run dev
# 浏览器打开 http://localhost:8080
# 默认账号: admin / 123456
```

## 🛠️ 技术栈

| 类别 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2.5 / Spring Cloud 2023.0.1 / Spring Cloud Alibaba |
| 服务治理 | Nacos 2.3 (注册+配置) / Spring Cloud Gateway / OpenFeign |
| 持久层 | MyBatis-Plus 3.5.5 / MySQL 8.0 / HikariCP |
| 缓存 | Redis 7.2 (Spring Data Redis) |
| 搜索引擎 | Elasticsearch 8.11 (Spring Data ES + IK 分词) |
| 工作流 | Flowable 7.0.1 (BPMN 2.0) |
| 安全 | Spring Security Crypto / JWT (jjwt 0.12) |
| 工具库 | Hutool 5.8 / Lombok / MapStruct |
| 前端 | Vue 3.4 / Vite 5 / Element Plus 2.7 / Pinia / Vue Router 4 |
| HTTP | Axios + SSE Stream |
| CI/CD | GitHub Actions |

## 📝 接口示例

```bash
# 登录
curl "http://localhost:9000/auth/login?username=admin&password=123456"

# 新增模型
curl -X POST http://localhost:9000/llm/add \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Ollama","provider":"OLLAMA","modelName":"llama3","apiBase":"http://host.docker.internal:11434","temperature":0.7,"maxTokens":2048}'

# 智能体对话
curl -X POST "http://localhost:9000/agent/chat?agentId=1&input=北京今天几度" \
  -H "Authorization: Bearer $TOKEN"
```

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

## 📄 License

[MIT](./LICENSE)
