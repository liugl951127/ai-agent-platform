# 架构详解

## 1. 分层

```
[Browser]
   │  :8080
   ▼
[Vue 3 SPA] (agent-ui)
   │  /api → vite proxy → :9000
   ▼
[Spring Cloud Gateway] (agent-gateway)
   │  - JWT 鉴权 (AuthFilter)
   │  - 路径路由 (/auth/**, /llm/**, /agent/**, ...)
   │  - 跨域
   ▼
[微服务集群,均注册到 Nacos]
   ├─ agent-auth       :9001   鉴权 + 用户
   ├─ agent-system     :9007   系统管理
   ├─ agent-llm        :9002   大模型路由
   ├─ agent-workflow   :9003   Flowable 流程
   ├─ agent-knowledge  :9004   ES RAG
   ├─ agent-agent      :9005   ReAct + Function Call
   └─ agent-conversation :9006 会话

[共享基础设施]
   ├─ MySQL    :3306   业务数据 + Flowable 表
   ├─ Redis    :6379   JWT Token / 限流 / 缓存
   ├─ ES       :9200   知识库 chunk
   └─ Nacos    :8848   注册中心 + 配置中心
```

## 2. 请求流(以"智能体对话"为例)

```
1. 前端 Chat.vue
   POST /api/agent/chat?agentId=1&input=北京今天几度
   Authorization: Bearer <jwt>

2. Gateway AuthFilter
   ├─ 解析 JWT → uid=1, name=admin
   ├─ 注入 X-User-Id / X-User-Name 到下游请求头
   └─ 路由到 lb://agent-agent/agent/chat

3. agent-agent AgentController
   └─ ReactExecutor.run(1, "北京今天几度")

4. ReactExecutor 执行 ReAct 循环 (最多 5 轮):
   a) RAG 检索
      GET 知识库 chunk (RestTemplate → agent-knowledge)
   b) 工具描述拼装
      SELECT * FROM agent_tool WHERE id IN (...)
   c) 拼 Prompt (system + user)
   d) 调 LLM
      LlmRouter.chatById(modelId, messages)
         → registry[provider].chat(model, messages)
         → OpenAI / Ollama / Qwen ...
   e) 解析 reply:
      - 若包含 "action": 反射调 Spring Bean (WeatherTool.execute)
      - 否则作为最终答案返回

5. 返回 R<String> 给前端,前端 marked 渲染为 Markdown
```

## 3. 模块依赖图

```
        agent-ui ─┐
                  │
   agent-gateway ─┤
                  │
   agent-auth ────┤
   agent-system ──┤
   agent-llm ─────┤
   agent-workflow ┤
   agent-knowledge ┤
   agent-agent ───┤
   agent-conversation ┘
                  │
        agent-common (R, BaseEntity, Jwt, Exception, SwaggerConfig)
```

- **横向依赖**:各微服务互不直接依赖 Java 类,通过 HTTP/Feign 通信
- **纵向依赖**:所有服务 → agent-common
- **api 网关**:agent-gateway 是唯一对外入口,其他服务只在内网互通

## 4. 工作流编排(可选,实验性)

`agent-workflow` 内置一个 BPMN 流程 `agentChat`:

```
[Start] → RagDelegate → LlmDelegate → Gateway
                                       │
                                  (needTool?)
                                   │        │
                                  Yes       No
                                   │        │
                              ToolDelegate  │
                                   │        │
                                   └────[End]
```

可使用 Flowable Modeler (`flowable-ui`) 在线设计更复杂流程,部署 BPMN 即可被 `WorkflowController` 启动。
