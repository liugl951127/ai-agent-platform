# 📘 项目操作手册 (OPERATIONS)

> 完整的使用 / 部署 / 运维指南. 接手人/新成员 30 分钟上手.

---

## 目录

- [0. 项目一览](#0-项目一览)
- [1. 环境要求](#1-环境要求)
- [2. 快速开始 (5 分钟跑起来)](#2-快速开始-5-分钟跑起来)
- [3. 服务清单与端口](#3-服务清单与端口)
- [4. 前端开发](#4-前端开发)
- [5. 后端开发](#5-后端开发)
- [6. 数据库初始化](#6-数据库初始化)
- [7. 启用 Nacos / Seata (生产模式)](#7-启用-nacos--seata-生产模式)
- [8. Docker Compose 一键起](#8-docker-compose-一键起)
- [9. 测试 & 覆盖率](#9-测试--覆盖率)
- [10. 常见问题 (FAQ)](#10-常见问题-faq)
- [11. 故障排查](#11-故障排查)

---

## 0. 项目一览

**AI Agent Platform** — 分布式 AI Agent 协作平台. **10 个微服务 + 1 个前端**.

```
┌─────────────────────────────────────────────────────────────┐
│  前端 (Vue3 + ElementPlus + Pinia)    port: 8080           │
│  ├─ Chat   (流式对话 + 工具调用 + 多智能体)                 │
│  ├─ Agents (智能体 CRUD)                                   │
│  ├─ Models (LLM 模型配置)                                  │
│  ├─ Knowledge (RAG 知识库)                                 │
│  ├─ Tools (工具中心)                                       │
│  └─ Workflow (Flowable BPMN)                              │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTP / SSE
┌─────────────────────────────────────────────────────────────┐
│  agent-gateway    :9000  (Spring Cloud Gateway, 统一入口)  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─ 微服务 ─────────────────────────────────────────────────────┐
│  agent-system      :9007  系统管理 (CRUD)                  │
│  agent-auth       :9001  JWT 鉴权 (login/refresh)         │
│  agent-llm        :9002  LLM 路由 (OpenAI/Qwen/Ollama)    │
│  agent-workflow   :9003  Flowable BPMN                    │
│  agent-knowledge  :9004  RAG (语义切分+ES+重排)            │
│  agent-conversation :9006  会话/历史                       │
│  agent-agent      :9005  多智能体 (Supervisor/GroupChat)   │
│  agent-tools      :9008  工具中心 (5 个工具 + HTTP API)    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─ 中间件 ─────────────────────────────────────────────────────┐
│  Nacos 8848/9848    服务发现 + 配置 (可选)                  │
│  MySQL 3306         业务表                                 │
│  Redis 6379         缓存 (Redisson)                        │
│  Elasticsearch 9200 向量存储 (RAG)                          │
│  Seata 8091         分布式事务 (可选)                       │
└─────────────────────────────────────────────────────────────┘
```

技术栈: **Spring Boot 3.2.5 + Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1 + Vue3 + Vite + ElementPlus + MyBatis-Plus + Flowable + Nacos + Redisson + ES + Docker**

---

## 1. 环境要求

| 软件 | 最低版本 | 推荐版本 | 验证命令 |
|---|---|---|---|
| JDK | 17 | 17 (LTS) | `java -version` |
| Maven | 3.6 | 3.8.7+ | `mvn -version` |
| Node.js | 18 | 22 (LTS) | `node -version` |
| MySQL | 5.7 | 8.0 (或 MariaDB 10.5+) | `mysql --version` |
| (可选) Docker | 20 | 24+ | `docker --version` |

**国内用户**强烈建议配 Maven 阿里云镜像 (`~/.m2/settings.xml`):

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>*,!spring-milestones,!aliyun-spring</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

npm 阿里云:

```bash
npm config set registry https://registry.npmmirror.com
```

---

## 2. 快速开始 (5 分钟跑起来)

### 步骤 1: 克隆 & 编译

```bash
git clone https://github.com/liugl951127/ai-agent-platform.git
cd ai-agent-platform

# 后端
mvn clean install -DskipTests          # ~3 min, 生成 10 个 jar

# 前端
cd agent-ui
npm install                            # ~1 min
npm run build                          # ~20s, 生成 dist/
cd ..
```

### 步骤 2: 准备 MySQL

无 Nacos / Redis 也能跑(默认配置已关掉 Nacos). **MySQL 必装**.

```bash
# Ubuntu / Debian
sudo apt install mariadb-server
sudo systemctl start mariadb

# macOS
brew install mysql@8
brew services start mysql@8

# 登录
mariadb -uroot
```

```sql
CREATE DATABASE agent_platform DEFAULT CHARACTER SET utf8mb4;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
GRANT ALL ON *.* TO 'root'@'localhost';
-- 若 Java 用 TCP 连接, 还需要:
CREATE USER 'root'@'127.0.0.1' IDENTIFIED BY 'root';
GRANT ALL ON *.* TO 'root'@'127.0.0.1';
FLUSH PRIVILEGES;
```

### 步骤 3: 启动后端 (一窗一个)

打开 9 个终端, 依次:

```bash
mvn -pl agent-tools package -DskipTests
java -jar agent-tools/target/agent-tools-1.0.0.jar
# → Started ToolsApplication in 8s
# → http://localhost:9008/tools/list  ← 验证

mvn -pl agent-gateway package -DskipTests
java -jar agent-gateway/target/agent-gateway-1.0.0.jar
# → Started GatewayApplication in 10s
# → http://localhost:9000/actuator/health  ← 验证

# 其他 7 个服务同理 (端口见下表)
```

### 步骤 4: 启动前端

```bash
cd agent-ui
npm run dev
# → Local: http://localhost:8080
```

浏览器打开 `http://localhost:8080`, 默认账号 `admin / 123456`.

### 步骤 5: 试用

1. **登录** → 默认 admin/123456
2. **工具** → 看到 5 个工具, 点 calculator → 输入 `sqrt(144)+2^3` → 点执行, 结果 `20`
3. **智能体** → 没有现成的, 点击 `新建`, 角色选 `researcher`, 工具勾 `http_fetch` + `json`, 保存
4. **智能对话** → 选刚建的智能体 → 输入"用 http_fetch 查 https://api.github.com 看看返回什么" → 看流式输出 + 工具调用面板

---

## 3. 服务清单与端口

| 服务 | 端口 | 启动命令 | 健康检查 |
|---|---|---|---|
| **agent-tools** | 9008 | `java -jar agent-tools/target/agent-tools-1.0.0.jar` | http://localhost:9008/actuator/health |
| **agent-gateway** | 9000 | `java -jar agent-gateway/target/agent-gateway-1.0.0.jar` | http://localhost:9000/actuator/health |
| **agent-system** | 9007 | `java -jar agent-system/target/agent-system-1.0.0.jar` | http://localhost:9007/actuator/health |
| **agent-auth** | 9001 | `java -jar agent-auth/target/agent-auth-1.0.0.jar` | http://localhost:9001/actuator/health |
| **agent-llm** | 9002 | `java -jar agent-llm/target/agent-llm-1.0.0.jar` | http://localhost:9002/actuator/health |
| **agent-workflow** | 9003 | `java -jar agent-workflow/target/agent-workflow-1.0.0.jar` | http://localhost:9003/actuator/health |
| **agent-knowledge** | 9004 | `java -jar agent-knowledge/target/agent-knowledge-1.0.0.jar` | http://localhost:9004/actuator/health |
| **agent-conversation** | 9006 | `java -jar agent-conversation/target/agent-conversation-1.0.0.jar` | http://localhost:9006/actuator/health |
| **agent-agent** | 9005 | `java -jar agent-agent/target/agent-agent-1.0.0.jar` | http://localhost:9005/actuator/health |
| **agent-ui** | 8080 | `cd agent-ui && npm run dev` | http://localhost:8080 |

**端口冲突?** 用命令行覆盖:

```bash
java -jar xxx.jar --server.port=8080
```

---

## 4. 前端开发

### 技术栈

- **Vue 3** (Composition API + `<script setup>`)
- **Vite 5** (构建)
- **Element Plus 2.7** (UI)
- **Pinia 2** (状态)
- **Vue Router 4** (路由)
- **Axios** (HTTP)
- **marked + highlight.js** (Markdown 渲染)
- **@element-plus/icons-vue**

### 目录结构

```
agent-ui/
├── index.html
├── vite.config.js          # Vite 配置 (含代理 /api → gateway)
├── package.json
└── src/
    ├── main.js             # 入口
    ├── App.vue             # 根组件
    ├── api/
    │   └── request.js      # axios 实例 + 拦截器
    ├── store/
    │   ├── user.js         # 用户状态 (token, user)
    │   └── settings.js     # 全局设置 (暗色/流式)
    ├── router/
    │   └── index.js        # 路由 + 守卫
    ├── layout/
    │   └── Layout.vue      # 主框架 (侧边栏 + 头部 + 内容)
    ├── components/
    │   ├── MessageItem.vue # 消息气泡 (Markdown + 工具调用)
    │   └── ToolCallPanel.vue # 工具调用面板
    └── views/
        ├── Login.vue       # 登录
        ├── Chat.vue        # 对话 (流式 + 多智能体)
        ├── Agents.vue      # 智能体
        ├── Models.vue      # 模型
        ├── Knowledge.vue   # 知识库
        ├── Tools.vue       # 工具
        └── Workflow.vue    # 工作流
```

### 常用命令

```bash
npm run dev       # 开发, http://localhost:8080
npm run build     # 生产构建, 输出到 dist/
npm run preview   # 预览构建结果
```

### 加新页面流程

1. `src/views/MyPage.vue` 写组件
2. `src/router/index.js` 加路由
3. `src/layout/Layout.vue` 加菜单项
4. (可选) `src/api/` 加 API 调用

### 跨域 (CORS)

前端 `vite.config.js` 已配代理: `/api/*` → `http://localhost:9000 (gateway)`.
所有后端请求走 `/api`, 由 gateway 路由到具体服务.

生产环境用 Nginx 反代:

```nginx
server {
  listen 80;
  server_name your-domain.com;
  location / {
    root /opt/agent-ui/dist;
    try_files $uri $uri/ /index.html;
  }
  location /api/ {
    proxy_pass http://gateway:9000/;
    proxy_set_header Host $host;
    proxy_buffering off;  # SSE 流式需要
  }
}
```

---

## 5. 后端开发

### 项目结构

```
ai-agent-platform/
├── pom.xml                  # 父 POM (依赖管理 + 模块)
├── agent-common/            # 公共库 (JWT, R, Redisson, Seata, Sentinel, 多租户)
├── agent-system/            # 系统模块
├── agent-auth/              # 鉴权
├── agent-llm/               # LLM 路由
├── agent-workflow/          # Flowable
├── agent-knowledge/         # RAG
├── agent-conversation/      # 会话
├── agent-agent/             # Agent 框架
│   └── src/main/java/com/platform/agent/
│       ├── multi/           # 多智能体
│       │   ├── LlmAgent.java
│       │   ├── Agent.java
│       │   ├── AgentFactory.java   # 4 个角色
│       │   ├── orchestrator/       # Supervisor / GroupChat / Plan-Execute
│       │   ├── bus/                # MessageBus
│       │   ├── memory/             # MemoryStore
│       │   └── reflection/         # ReflectionEngine
│       └── tools/                  # (内嵌, 远程调 agent-tools)
└── agent-tools/             # 工具中心
    └── src/main/java/com/platform/tools/
        ├── Tool.java
        ├── ToolDefinition.java
        ├── registry/ToolRegistry.java
        └── {calculator,datetime,http,json,sql}/  # 5 个工具
```

### 加新工具流程

1. 写 `agent-tools/src/main/java/com/platform/tools/myfeature/MyTool.java`:

```java
@Component
@ToolDefinition(
    name = "my_tool",
    description = "做什么",
    parameters = "{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"string\"}},\"required\":[\"x\"]}",
    category = "general"
)
public class MyTool implements Tool {
    @Override public String name() { return "my_tool"; }
    @Override public Object execute(Map<String, Object> args) {
        String x = String.valueOf(args.get("x"));
        return "处理结果: " + x;
    }
}
```

2. 重启 agent-tools, 自动注册. 在 `GET /tools/list` 看到, LLM 可调用.

### 加新 Agent 角色

编辑 `agent-agent/src/main/java/com/platform/agent/multi/roles/AgentFactory.java`:

```java
public LlmAgent myRole() {
    return new LlmAgent("myRole", "角色描述", "system prompt", 1L, 
        List.of("calculator", "http_fetch"),
        llmFeign, toolInvoker, reflector, memory);
}
```

在 `MultiAgentService.defaultTeam()` 加入即可被 Supervisor 调用.

### 加新 LLM 提供方

编辑 `agent-llm/src/main/java/com/platform/llm/service/impl/`, 新建 `XXXProvider implements LlmProvider`, 加 `@Component` 自动注册.

---

## 6. 数据库初始化

### 自动建表 (MyBatis-Plus + Flowable)

服务**首次启动**会自动建表:

- `agent_info` / `llm_model` / `agent_tool` 等 (MyBatis-Plus)
- `ACT_*` / `FLW_*` (Flowable)
- `sys_*` (Seata)

### 手动初始化 (可选)

```sql
-- 智能体示例数据
INSERT INTO agent_info (name, role, system_prompt, tool_ids, enabled, deleted, create_time, update_time)
VALUES ('Researcher', '研究员',
  '你是一名研究员, 擅长通过网络搜索和文档查证获取事实和数据.',
  'http_fetch,json', 1, 0, NOW(), NOW());
```

---

## 7. 启用 Nacos / Seata (生产模式)

默认配置**关掉**了 Nacos/Seata. 生产环境启用:

### 步骤 1: 启动 Nacos

```bash
# Docker
docker run -d --name nacos -p 8848:8848 -p 9848:9848 \
  -e MODE=standalone nacos/nacos-server:v2.3.1
```

或下载二进制: https://nacos.io/zh-cn/docs/quick-start.html

### 步骤 2: 修改 application.yml

每个服务把 `enabled: false` 改成 `enabled: true`:

```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: localhost:8848
      config:
        enabled: true
        server-addr: localhost:8848
        file-extension: yaml

seata:
  enabled: true
  registry:
    type: nacos
    nacos:
      server-addr: localhost:8848
```

### 步骤 3: 重启服务

服务会自动注册到 Nacos. 在 http://localhost:8848/nacos (默认 nacos/nacos) 看到.

### 步骤 4: 启动 Seata (可选)

```bash
docker run -d --name seata -p 8091:8091 \
  -e SEATA_MODE=file seataio/seata-server:2.0.0
```

---

## 8. Docker Compose 一键起

```bash
# 启动所有中间件 + 服务
docker-compose up -d

# 查看日志
docker-compose logs -f agent-gateway

# 停止
docker-compose down
```

`docker-compose.yml` 含: Nacos, MySQL, Redis, Elasticsearch, Seata, 9 个微服务.
`docker-compose.app.yml` 只含微服务 (假设外部已有中间件).

---

## 9. 测试 & 覆盖率

### 跑测试

```bash
mvn test                           # 全部模块
mvn -pl agent-tools test           # 单模块
mvn -pl agent-tools test -Dtest=ExprEvaluatorTest  # 单个测试类
```

### 统计

| 模块 | 测试数 | 状态 |
|---|---|---|
| common | 20 | ✅ |
| auth | 5 | ✅ |
| llm | 4 | ✅ |
| knowledge | 26 | ✅ |
| agent | 31 | ✅ |
| tools | 41 | ✅ |
| **合计** | **127** | **✅** |

### 覆盖率

```bash
mvn test jacoco:report
# 报告在 target/site/jacoco/index.html
```

---

## 10. 常见问题 (FAQ)

### Q: 服务起不来, 提示 `Connection refused: 127.0.0.1:3306`

A: MySQL 没装/没起. 见 §6.

### Q: 服务起不来, 提示 `9848` 端口连接失败

A: 这是 Nacos 2.x 的 gRPC 端口 (8848+1000). 两种方案:
- 装 Nacos: 见 §7
- 保持默认关 (无需改 yml, 我们的 application.yml 已经 `enabled: false`)

### Q: Chat 页面"流式输出"没反应

A: 后端 `/agent/chat/stream` 接口还没实现. 当前 `/agent/chat` 是同步返回. 流式接口是**预留**的 (前端用 fetch + ReadableStream).

### Q: `agent-knowledge` 启动报 `No qualifying bean of type Retriever`

A: `@ConditionalOnMissingBean` 三方互抑制. 已修复 (commit `12ad7ed`). 重新 `mvn install`.

### Q: agent-agent 启动报 `LlmFeignClient could not be found`

A: 父 POM 的 spring-cloud-starter-openfeign 版本缺失. 已修复 (显式 4.1.1).

### Q: `mvn` 命令找不到

A: `apt install maven` (Ubuntu) 或 `brew install maven` (macOS).

### Q: `npm install` 慢

A: `npm config set registry https://registry.npmmirror.com`

### Q: MySQL 报 `Access denied for user 'root'@'localhost'`

A: 你的 MySQL 密码不是 `root`, 或 root 没权限. 在 application.yml 改:

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/agent_platform?...
    username: your_user
    password: your_password
```

### Q: agent-workflow 启动报 `org.flowable.common.rest.exception.BaseExceptionHandlerAdvice not found`

A: Flowable 7.0.1 与 Spring Boot 3 自动配置冲突. **不要**在 `@ComponentScan` 写 `org.flowable`, 已经修好.

### Q: 我想用真实 LLM (非本地)

A: 在 **Models** 页面 (前端) 新增, 填 API Key / API Base / Model Name. 或 SQL:

```sql
INSERT INTO llm_model (name, provider, model_name, api_base, api_key, temperature, max_tokens, enabled, deleted, create_time, update_time)
VALUES ('GPT-4o', 'OPENAI', 'gpt-4o', 'https://api.openai.com', 'sk-xxx', 0.7, 2000, 1, 0, NOW(), NOW());
```

### Q: 怎么部署到生产?

A: 见 `docs/deployment.md`. 简版:

1. 编译 `mvn clean package -DskipTests`
2. 上传 jar 到服务器
3. 装中间件 (Nacos + MySQL + Redis + ES)
4. 修改 application.yml 里的 IP/密码
5. 启动 `nohup java -jar xxx.jar &` 或用 systemd / Docker

---

## 11. 故障排查

### 诊断工具

```bash
# 看 JVM 健康
jps -lvm
jstack <pid> | head -50

# 看端口占用
ss -tlnp | grep 9007
lsof -i :9007

# 看 MySQL 慢查询
SHOW FULL PROCESSLIST;

# 看 Nacos 服务列表
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=agent-gateway
```

### 常见日志关键词

| 关键词 | 含义 | 解决方案 |
|---|---|---|
| `Connection refused` | 中间件没起 | 启动对应中间件 |
| `ClassNotFoundException` | 依赖没引 | 加 pom 依赖 |
| `BeanCreationException` | Spring 配置错 | 看 cause, 通常是缺 bean 或循环依赖 |
| `Web server failed to start. Port XXX was already in use` | 端口占用 | `pkill -9 java` 或改端口 |
| `MyBatis-Plus null id` | 实体没 `@TableId` | 加 `@TableId(type = IdType.AUTO)` |
| `401 Unauthorized` | JWT 过期/缺失 | 重新登录 |
| `Nacos Discovery Client not initialized` | 关了 Nacos, 服务不注册 | 正常, 不影响本地起服务 |

### 提交 Bug 模板

```markdown
**环境**: JDK 17, Maven 3.8.7, macOS 14
**复现步骤**:
1. mvn clean install
2. java -jar agent-system/...
**期望**: 启动成功
**实际**: 报错 xxx
**日志**:
```
[贴关键堆栈]
```
```

---

## 附录: 命令速查

```bash
# 编译
mvn clean install -DskipTests

# 单服务
mvn -pl agent-tools package -DskipTests

# 跑测试
mvn test

# 启动服务
java -jar agent-xxx/target/agent-xxx-1.0.0.jar

# 启动前端
cd agent-ui && npm run dev

# 看依赖
mvn dependency:tree -pl agent-agent

# 装前端依赖
cd agent-ui && npm install

# 看进程
jps -lvm

# 杀进程
pkill -9 java

# 装中间件 (Ubuntu)
apt install mariadb-server
docker run -d --name nacos -p 8848:8848 -p 9848:9848 -e MODE=standalone nacos/nacos-server:v2.3.1
```

---

最后更新: 2026-06
适用版本: v1.0.0
反馈: 提 Issue 或 PR
