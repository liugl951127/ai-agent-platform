# agent-llm

大模型调度服务 (OpenAI / Ollama / Qwen / DeepSeek 统一 Provider)。

## 端口
9002

## 启动所需中间件
| 中间件 | 端口 | 必选? |
|---|---|---|
| Nacos Server | 8848 | ✅ 必选 |
| MySQL | 3306 | ✅ 必选 (llm_model 表) |
| Ollama / OpenAI Key | - | ⚠️ 看 LLM 怎么用 |

## 启动命令
```bash
mvn -pl agent-llm -am spring-boot:run
```

## 启动前准备
```bash
# 1. 启中间件
docker compose up -d nacos mysql

# 2. 初始化 MySQL (含默认 Ollama 模型)
mysql -h localhost -u root -p < sql/init.sql

# 3. (可选) 启本地 Ollama
docker run -d -p 11434:11434 --name ollama ollama/ollama
ollama pull llama3
```

## 验证
```bash
# 1. 健康检查
curl http://localhost:9002/actuator/health

# 2. 列出模型 (需要登录拿 token)
TOKEN=$(curl -s "http://localhost:9000/auth/login?username=admin&password=123456" | jq -r .data)
curl http://localhost:9002/llm/list -H "Authorization: Bearer $TOKEN"

# 3. 调 LLM 对话
curl -X POST "http://localhost:9002/llm/chat?modelId=1" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '[{"role":"user","content":"你好"}]'
```

## 常见启动错误

| 错误 | 原因 | 修复 |
|---|---|---|
| `Connection refused: localhost:11434` | Ollama 没起 | 启 Ollama |
| `Table 'agent_platform.llm_model' doesn't exist` | 没跑 init.sql | 跑 init.sql |
| `WebClient ... 401 Unauthorized` | OpenAI Key 错了 | 改 llm_model.api_key 字段 |

## 依赖关系图
```
agent-llm (9002)
   ↓ 依赖
agent-common (R)
   ↓
Nacos + MySQL + (Ollama/OpenAI)
```
