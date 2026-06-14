# AI Agent 能力总览 (AGENT_FEATURES)

> 截止 2026-06, 平台已实现的 LLM / Agent / 多智能体 / RAG 能力

## 1. 单 Agent 能力 (`agent-agent/multi/LlmAgent`)

| 能力 | 实现位置 | 说明 |
|---|---|---|
| **ReAct 推理** | LlmAgent.handle | Thought → Action → Observation → Final Answer 循环, maxSteps 防爆 |
| **多风格输出兼容** | LlmAgent.parseAction | 同时支持 JSON `{"action":...}` 和 ReAct 文本 `Action: tool(args)` |
| **工具调用** | ToolInvoker (HTTP) + agent-tools 服务 | 远端调工具, agent-tools 挂了不影响 agent-agent 启动 |
| **Reflection 反思** | ReflectionEngine | 可选, 答完后再问 LLM "够好吗?", 替换或保留原答案 |
| **Memory 三层** | MemoryStore (接口) + DefaultMemoryStore (实现) | Working (内存) / 短期 (Redis) / 长期 (MySQL) |
| **Token 用量统计** | AgentResponse.promptTokens/completionTokens | 每次响应都返回, 可落库计费 |

## 2. 多智能体协作 (`agent-agent/multi/orchestrator/`)

| 模式 | 类 | 适用场景 | Demo |
|---|---|---|---|
| **Supervisor** | SupervisorOrchestrator | 主 Agent 拆 plan → 派子 Agent → 汇总 | 写报告: 查资料+写稿+审稿 |
| **GroupChat** | GroupChatOrchestrator | 头脑风暴, 多视角, 轮流发言 | 讨论方案, 多角度分析 |
| **Plan-and-Execute** | PlanAndExecute | 复杂多步任务, 拓扑执行 | 调研 + 写 + 翻译 三步 |

**A2A 消息总线** `bus/MessageBus`:
- `InMemoryBus` — 单 JVM, 用 ConcurrentHashMap
- `RedisBus` (TODO) — 跨服务, Redis Streams
- 协议: AgentMessage (from/to/topic/body/attachments)

**4 个内置角色** `roles/AgentFactory`:
- `researcher` 联网查资料 (http_fetch + json)
- `writer` 把材料组织成报告
- `reviewer` 严格审稿 (1-10 分 + 改进建议)
- `coder` 写代码 / 跑 SQL (calculator + sql + json + datetime)

## 3. 工具能力中心 (`agent-tools`)

5 个工具, 全部 `Tool` 接口 + `@ToolDefinition` 注解, Spring 启动自动注册:

| 工具 | 能力 | 安全 |
|---|---|---|
| `calculator` | 数学表达式求值 (+ - * / ^ () sin/cos/log/sqrt) | 无外部输入风险 |
| `http_fetch` | GET/POST/PUT/DELETE, headers/body/timeout/截断 | **SSRFGuard**: 禁 localhost/私网/loopback |
| `datetime` | now/add/diff/format/parse/zone | 无 |
| `json` | query(点路径) / count / keys / filter(eq/ne/gt/lt/contains) | 无 |
| `sql_query` | 限 SELECT, 强制 LIMIT, 表名白名单 | 多层防护 |

**HTTP 接口**:
- `GET  /tools/list` 列出所有 (LLM function calling 格式)
- `POST /tools/invoke` 调用单个
- `POST /tools/invoke/batch` 批量调用

## 4. RAG 流水线 (`agent-knowledge/rag/`)

完整链路: 文档 → 切分 → Embedding → 向量召回 → 重排 → Prompt 拼装

| 组件 | 实现 | 备注 |
|---|---|---|
| **Chunker** | SemanticChunker | 段落 + 句子 + 滑窗, 目标 500 字符, 最大 1500 |
| **Embedding** | EmbeddingClient (接口) | 默认 HashEmbedding (零依赖); OpenAI 兼容 API (text-embedding-3 等) |
| **Retriever** | Retriever (接口) | 默认 InMemoryVectorStore; 生产用 ES 8.x dense_vector + HNSW |
| **Reranker** | Reranker (接口) | 默认 KeywordBoost (BM25 简化); 生产用 Cross-Encoder |
| **RagPipeline** | 统一入口 | ingest(docId, content) / search(query, topK) / buildRagPrompt |

**支持混合策略**:
- `embedding.openai.enabled=true` 配 apiKey/model 即启用 OpenAI embedding
- 不配 → 降级到 HashEmbedding (单测可用, 准确率低)
- 同理 ES 检索 / Cross-Encoder 重排

## 5. Demo 场景: 写一份 2026 AI 行业研究报告

完整端到端流程 (`MultiAgentServiceIntegrationTest` 验证):

```
用户: "写一份 2026 AI 行业研究报告"
  ↓
Supervisor 模式启动, writer 为主 Agent
  ↓
[step 1] writer 拆 plan
  LLM 返回: {"plan":[
    {"to":"researcher","task":"查 AI 行业 2025 规模"},
    {"to":"researcher","task":"查头部公司"},
    {"to":"writer","task":"整合写报告"}
  ]}
  ↓
[step 2-3] 并行/串行调 researcher (子 Agent)
  → 调 http_fetch 抓数据, 调 json 解析
  → 拿到 2 份研究报告
  ↓
[step 4] writer 子 Agent 把研究材料组织成 2000 字报告
  ↓
[step 5] writer 主 Agent 拿全部结果, 输出最终报告
  ↓
返回: 含 thought (推理过程) / actions (调的工具/子Agent) / artifacts (报告内容)
```

总 127 个测试覆盖所有路径。

## 6. 后续可扩展

- [ ] WebSocket 流式响应 (SSE 也行)
- [ ] Self-Consistency 多采样投票
- [ ] Cross-Encoder 重排 (需 sentence-transformers 或 API)
- [ ] ES dense_vector 真实版 Retriever
- [ ] Hierarchical 三层 Agent (CEO → 部门 → 员工)
- [ ] Agent 评测 (e.g. AgentBench 风格任务集)
- [ ] Token 用量持久化 + 计费
- [ ] 前端 Vue3 聊天 UI (WebSocket 流式)
