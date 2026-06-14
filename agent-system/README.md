# agent-system

系统管理服务 (菜单 / 角色 / 字典) — 当前为占位模块。

## 端口
9007

## 启动所需中间件
| 中间件 | 端口 | 必选? |
|---|---|---|
| Nacos Server | 8848 | ✅ 必选 |
| 其他 | - | ❌ 不需要 |

**这是最轻量的服务**,**不依赖 DB / Redis / ES / LLM**,非常适合先用来验证整套 Spring Cloud 链路通不通。

## 启动命令
```bash
mvn -pl agent-system -am spring-boot:run
```

## 验证
```bash
curl http://localhost:9007/actuator/health
# 期望: {"status":"UP"}
```

## 依赖关系图
```
agent-system (9007)
   ↓ 依赖
agent-common (R)
   ↓
Nacos
```
