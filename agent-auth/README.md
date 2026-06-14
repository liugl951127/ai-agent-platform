# agent-auth

鉴权服务 (登录 / 注册 / JWT / BCrypt / 验证码)。

## 端口
9001

## 启动所需中间件
| 中间件 | 端口 | 必选? |
|---|---|---|
| Nacos Server | 8848 | ✅ 必选 |
| MySQL | 3306 | ✅ 必选 (sys_user 表) |
| Redis | 6379 | ✅ 必选 (JWT token 缓存 / 验证码) |
| Seata Server | 8091 | ⚠️ 可选 (没用分布式事务时可不启) |

## 启动命令
```bash
mvn -pl agent-auth -am spring-boot:run
```

## 启动前准备
```bash
# 1. 启中间件
docker compose up -d nacos mysql redis

# 2. 初始化 MySQL (含默认 admin/123456)
mysql -h localhost -u root -p < sql/init.sql
# 或者: docker exec -i mysql mysql -uroot -proot agent_platform < sql/init.sql
```

## 验证
```bash
# 1. 健康检查
curl http://localhost:9001/actuator/health

# 2. 登录
curl -s "http://localhost:9001/auth/login?username=admin&password=123456"
```

## 常见启动错误

| 错误 | 原因 | 修复 |
|---|---|---|
| `Could not create connection to database server` | MySQL 没起 / init.sql 没跑 | 检查 `docker ps`,跑 `init.sql` |
| `Unable to connect to Redis` | Redis 没起 | `docker compose up -d redis` |
| `Table 'agent_platform.sys_user' doesn't exist` | 没跑 init.sql | 跑 init.sql |

## 依赖关系图
```
agent-auth (9001)
   ↓ 依赖
agent-common (R / JwtUtil / Security)
   ↓
Nacos + MySQL + Redis
```
