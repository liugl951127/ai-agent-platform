# agent-knowledge

Elasticsearch 知识库 (RAG 检索)。

## 端口
9004

## 启动所需中间件
| 中间件 | 端口 | 必选? |
|---|---|---|
| Nacos Server | 8848 | ✅ 必选 |
| Elasticsearch 8 | 9200 | ✅ 必选 |
| MySQL | 3306 | ❌ 不需要 |
| Redis | 6379 | ❌ 不需要 |

## 启动命令
```bash
mvn -pl agent-knowledge -am spring-boot:run
```

## 启动前准备
```bash
# 1. 启 ES
docker compose up -d elasticsearch

# 2. 等 ES 起来 (大约 30s)
curl http://localhost:9200
# 期望: { "name": ..., "version": { "number": "8.11.0" } }
```

## 验证
```bash
# 1. 健康检查
curl http://localhost:9004/actuator/health

# 2. 写入一条文档
curl -X POST "http://localhost:9004/knowledge/index?kbId=1&content=Spring%20Cloud%20%E6%98%AF%E5%BE%AE%E6%9C%8D%E5%8A%A1%E6%A1%86%E6%9E%B6"

# 3. 搜索
curl -X POST "http://localhost:9004/knowledge/search?kbId=1&q=Spring"
```

## 常见启动错误

| 错误 | 原因 | 修复 |
|---|---|---|
| `Connection refused: localhost:9200` | ES 没起 | 启 ES |
| `cluster health status RED` | ES 单节点初始化没完成 | 等等再试 |
| `ElasticsearchException: no permissions` | ES 启用了 security | 改 application.yml 加账号密码 |

## 依赖关系图
```
agent-knowledge (9004)
   ↓ 依赖
agent-common (R)
   ↓
Nacos + Elasticsearch
```
