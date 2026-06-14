# agent-gateway

Spring Cloud Gateway 网关,负责路由 + JWT 鉴权 + 限流。

## 端口
9000

## 启动所需中间件
| 中间件 | 端口 | 必选? | 不起会怎样 |
|---|---|---|---|
| Nacos Server | 8848 | ✅ 必选 | 服务无法注册,路由规则拉不到 |
| Redis | 6379 | ❌ 不需要 | - |
| MySQL | 3306 | ❌ 不需要 | - |
| Elasticsearch | 9200 | ❌ 不需要 | - |
| Sentinel Dashboard | 8080 | ⚠️ 可选 | 限流规则查不到,但不影响启动 |

## 启动命令
```bash
mvn -pl agent-gateway -am spring-boot:run
# 或者
mvn -pl agent-gateway -am package
java -jar agent-gateway/target/agent-gateway-1.0.0.jar
```

## 验证
```bash
# 1. 进程起来了
curl http://localhost:9000/actuator/health
# 期望: {"status":"UP"}

# 2. 路由生效 (登录)
curl -s "http://localhost:9000/auth/login?username=admin&password=123456"
# 期望: {"code":200,"message":"success","data":"eyJ..."}
```

## 常见启动错误

| 错误 | 原因 | 修复 |
|---|---|---|
| `WebMvcConfigurer cannot be cast to ...` | agent-common 的 web starter 没排除 | 看父 pom 里 exclusions |
| `Unable to find Nacos server localhost:8848` | Nacos 没起 | `docker compose up -d nacos` |
| `Port 9000 already in use` | 端口被占 | 改 application.yml server.port |

## 依赖关系图
```
agent-gateway (9000)
   ↓ 依赖
agent-common (JwtUtil / R)
   ↓
[独立,无 DB / Redis]
```
