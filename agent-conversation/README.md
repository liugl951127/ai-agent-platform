# agent-conversation

对话会话服务 (chat_session / chat_message 表)。

## 端口
9006

## 启动所需中间件
| 中间件 | 端口 | 必选? |
|---|---|---|
| Nacos Server | 8848 | ✅ 必选 |
| MySQL | 3306 | ✅ 必选 |
| Seata Server | 8091 | ⚠️ 可选 (没起分布式事务不可用,但服务能起) |

## 启动命令
```bash
mvn -pl agent-conversation -am spring-boot:run
```

## 启动前准备
```bash
# 1. 启中间件
docker compose up -d nacos mysql

# 2. 初始化库
mysql -h localhost -uroot -p < sql/init.sql
```

## 验证
```bash
curl http://localhost:9006/actuator/health
```

## 依赖关系图
```
agent-conversation (9006)
   ↓ 依赖
agent-common (R)
   ↓
Nacos + MySQL (+ 可选 Seata)
```
