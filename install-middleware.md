# 🛠 中间件安装手册 (INSTALL-MIDDLEWARE)

> 一键安装: **Nacos / MySQL / Redis / Elasticsearch / Seata**
>
> 配套脚本: `scripts/install-middleware.sh` (17KB, 支持 docker + native 两种模式)

---

## 目录

- [0. 中间件一览](#0-中间件一览)
- [1. 一键安装 (推荐)](#1-一键安装-推荐)
- [2. 单独安装某个](#2-单独安装某个)
- [3. Docker 模式 vs 原生模式](#3-docker-模式-vs-原生模式)
- [4. 各中间件详细说明](#4-各中间件详细说明)
- [5. 启动顺序 & 健康检查](#5-启动顺序--健康检查)
- [6. 启用中间件 (后端配置)](#6-启用中间件-后端配置)
- [7. 卸载](#7-卸载)
- [8. 常见问题](#8-常见问题)

---

## 0. 中间件一览

| 中间件 | 端口 | 作用 | 必需? | 备选方案 |
|---|---|---|---|---|
| **MySQL** | 3306 | 业务数据 / Flowable 表 | ✅ 必需 | 不可替代 |
| **Redis** | 6379 | Redisson 分布式锁/限流/缓存 | ⚠️ 推荐 | 可去掉(redisson 标了 optional) |
| **Elasticsearch** | 9200 | RAG 向量存储 | ⚠️ 推荐 | 启动时用 InMemoryVectorStore |
| **Nacos** | 8848 / 9848 | 服务发现 + 配置中心 | ⚠️ 可选 | 默认关,本机启动不需要 |
| **Seata** | 8091 | 分布式事务 | ⚠️ 可选 | 单服务事务用 @Transactional |

**默认配置**: 我们的 application.yml **默认关掉** Nacos/Seata, 启动只需 MySQL.

---

## 1. 一键安装 (推荐)

### 方式 A: 用本仓库脚本 (推荐 ✅)

```bash
# 1. 进入仓库
cd ai-agent-platform

# 2. 装全部
./scripts/install-middleware.sh

# 提示选项 [y/N], 输入 y 继续; 多个组件每个都问一次
# 或加 -y 全自动 yes
./scripts/install-middleware.sh -y
```

脚本会**按顺序**装: MySQL → Redis → ES → Nacos → Seata, 装完做健康检查.

### 方式 B: 直接下载单文件 (无需克隆整个仓库)

```bash
# 方式 B1: 用 curl
curl -fsSL https://raw.githubusercontent.com/liugl951127/ai-agent-platform/main/scripts/install-middleware.sh -o install-middleware.sh
chmod +x install-middleware.sh
./install-middleware.sh -y

# 方式 B2: 用 wget
wget -q https://raw.githubusercontent.com/liugl951127/ai-agent-platform/main/scripts/install-middleware.sh
chmod +x install-middleware.sh
./install-middleware.sh -y
```

> 📌 **两种方式功能完全一致**, 选你方便的.

### 方式 C: 纯 Docker (最快, 推荐生产)

```bash
# 一行命令起所有中间件
docker run -d --name agent-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=agent_platform \
  -v agent-mysql-data:/var/lib/mysql --restart unless-stopped mysql:8.0

docker run -d --name agent-redis -p 6379:6379 \
  -v agent-redis-data:/data --restart unless-stopped redis:7-alpine

docker run -d --name agent-elasticsearch -p 9200:9200 \
  -e "discovery.type=single-node" -e "xpack.security.enabled=false" \
  -v agent-es-data:/usr/share/elasticsearch/data --restart unless-stopped \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

docker run -d --name agent-nacos -p 8848:8848 -p 9848:9848 \
  -e MODE=standalone -v agent-nacos-data:/home/nacos/data --restart unless-stopped \
  nacos/nacos-server:v2.3.1

docker run -d --name agent-seata -p 8091:8091 \
  -e SEATA_MODE=file -v agent-seata-data:/root/seata-data --restart unless-stopped \
  seataio/seata-server:2.0.0
```

### 方式 D: docker-compose (本仓库有)

```bash
docker-compose up -d    # 起所有中间件 + 服务
docker-compose down    # 停
```

---

## 2. 单独安装某个

```bash
# 装指定
./scripts/install-middleware.sh mysql
./scripts/install-middleware.sh redis
./scripts/install-middleware.sh elasticsearch
./scripts/install-middleware.sh nacos
./scripts/install-middleware.sh seata
./scripts/install-middleware.sh all      # 装全部 = 默认行为

# 多选
./scripts/install-middleware.sh mysql redis nacos

# 指定安装方式
./scripts/install-middleware.sh --mode=docker mysql    # 强制 Docker
./scripts/install-middleware.sh --mode=native redis    # 强制原生 (apt/brew)
./scripts/install-middleware.sh --mode=auto seata      # 自动 (有 docker 用 docker, 否则原生)

# 自定义参数
./scripts/install-middleware.sh --port=3307 mysql      # MySQL 用 3307
./scripts/install-middleware.sh --es-version=8.12.0 elasticsearch

# 跳过健康检查
./scripts/install-middleware.sh --skip-health redis

# 自动化 (跳过确认)
./scripts/install-middleware.sh -y all

# 帮助
./scripts/install-middleware.sh --help
```

---

## 3. Docker 模式 vs 原生模式

| 维度 | Docker 模式 | 原生模式 (apt/brew) |
|---|---|---|
| 启动速度 | 快 (镜像已下) | 中 (apt install) |
| 资源占用 | 多一层 Docker | 更少 |
| 隔离性 | 完全隔离 | 共用主机 |
| 升级/卸载 | 简单 (rm container) | apt purge |
| 数据持久化 | volume | /var/lib/... |
| **生产推荐** | ✅ | ⚠️ |
| **开发推荐** | ✅ | ✅ |
| **离线环境** | ❌ (需先拉镜像) | ✅ (本地包) |

**默认行为**: 脚本有 docker 就用 docker, 否则降级原生.

**强制模式**:
- `--mode=docker` — 强制 Docker (没装就报错)
- `--mode=native` — 强制原生
- `--mode=auto` — 自动 (默认)

---

## 4. 各中间件详细说明

### 4.1 MySQL

**端口**: 3306
**默认账号**: root / root
**默认库**: agent_platform
**数据卷**: `/var/lib/mysql` (native) / `agent-mysql-data` (docker)

**手动验证**:
```bash
mariadb -uroot -proot
mysql> SHOW DATABASES;
+--------------------+
| agent_platform     |  ← 出现这个就 OK
| information_schema |
| mysql              |
| performance_schema |
+--------------------+
```

**重置密码** (忘记 root 密码时):
```bash
# 1. 停
sudo systemctl stop mariadb

# 2. 跳过授权启动
sudo mysqld_safe --skip-grant-tables &

# 3. 改密码
mariadb -uroot
> FLUSH PRIVILEGES;
> ALTER USER 'root'@'localhost' IDENTIFIED BY '新密码';
> EXIT;

# 4. 正常启动
sudo systemctl start mariadb
```

**JDBC URL 模板** (后端 application.yml 用):
```
jdbc:mysql://127.0.0.1:3306/agent_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
```

---

### 4.2 Redis

**端口**: 6379
**密码**: 默认无密码 (设了改 application.yml)
**数据卷**: `/data`

**手动验证**:
```bash
redis-cli PING
# PONG  ← OK

redis-cli SET foo bar
redis-cli GET foo    # bar

# 看键
redis-cli KEYS '*'
```

**Redisson 配置** (后端 application.yml):
```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password:        # 无密码就空着
```

---

### 4.3 Elasticsearch

**端口**: 9200 (HTTP), 9300 (transport, Java 客户端用)
**版本**: 8.11.0
**JVM**: 默认 512m (脚本已调小, 沙箱友好)
**安全**: 默认禁用 xpack (方便开发)
**数据卷**: `/usr/share/elasticsearch/data`

**手动验证**:
```bash
curl http://localhost:9200
{
  "name" : "agent-elasticsearch",
  "cluster_name" : "agent-platform",
  "version" : {
    "number" : "8.11.0",
    ...
  }
}
```

**常用命令**:
```bash
# 索引列表
curl http://localhost:9200/_cat/indices

# 创建索引
curl -X PUT http://localhost:9200/my_index -H 'Content-Type: application/json' -d '{
  "mappings": {
    "properties": {
      "content": { "type": "text" },
      "vector": { "type": "dense_vector", "dims": 384 }
    }
  }
}'

# 删索引
curl -X DELETE http://localhost:9200/my_index
```

**RAG 集成** (agent-knowledge/application.yml):
```yaml
spring:
  elasticsearch:
    uris: http://127.0.0.1:9200
```

---

### 4.4 Nacos

**端口**: 8848 (HTTP) + 9848 (gRPC, 2.x 必需)
**账号**: nacos / nacos
**Web 控制台**: http://localhost:8848/nacos

**手动验证**:
```bash
curl http://localhost:8848/nacos/
# 返回 HTML 页面 = OK

# 注册服务测试
curl -X POST 'http://localhost:8848/nacos/v1/ns/instance?serviceName=test&ip=127.0.0.1&port=8080'

# 查服务列表
curl 'http://localhost:8848/nacos/v1/ns/service/list'
```

**Web 控制台**:
- 用户名: nacos
- 密码: nacos
- 看到服务列表 / 配置管理 / 命名空间

**为什么有 9848 端口?**
Nacos 2.x 客户端用 gRPC 推送配置变更 + 服务心跳, 端口 = 8848 + 1000 = 9848. 防火墙要**同时开放两个**.

**启用方法** (在每个服务的 application.yml):
```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: 127.0.0.1:8848
      config:
        enabled: true
        server-addr: 127.0.0.1:8848
        file-extension: yaml
```

---

### 4.5 Seata

**端口**: 8091 (默认 file 模式, 无 TC server)
**存储**: file (默认, 重启丢数据) / db (推荐) / redis

**手动验证**:
```bash
curl http://localhost:8091/
# 返回 "cannot find service" 或空 = OK (server 起来, 没注册服务)

# 看进程
ps -ef | grep seata-server
```

**启用方法** (在用到 @GlobalTransactional 的服务):
```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: agent-platform-tx-group
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      dataId: seataServer.properties
```

**生产用 db 模式** (持久化):
```bash
# 1. 初始化 Seata 数据库
mysql -uroot -proot < /opt/seata/script/server/db/mysql.sql

# 2. 改 store.mode
# /opt/seata/conf/application.yml: store.mode: db
# 配 DB URL/USER/PASSWORD

# 3. 重启
bash /opt/seata/bin/seata-server.sh restart
```

---

## 5. 启动顺序 & 健康检查

### 推荐启动顺序

```
1. MySQL         (无依赖, 第一个)
2. Redis         (无依赖)
3. Elasticsearch (无依赖)
4. Nacos         (无依赖)
5. Seata         (无依赖, 可选)
6. 后端服务       (依赖上面, 默认关 Nacos/Seata)
7. 前端           (最后)
```

### 启动后端

后端服务**默认只依赖 MySQL**:
```bash
mvn -pl agent-tools package -DskipTests
java -jar agent-tools/target/agent-tools-1.0.0.jar
# 启动成功, 不依赖任何其他中间件
```

启用 Redis / ES / Nacos / Seata 的服务**才需要对应中间件**.

### 一键健康检查

```bash
# 用我们的脚本
./scripts/install-middleware.sh --skip-health seata   # 装不健康
# 已装好组件想健康检查: --skip-health 反而跳过, 直接看下面

# 手动
curl -sf http://localhost:3306 >/dev/null && echo "MySQL UP" || echo "MySQL DOWN"
redis-cli -p 6379 PING 2>/dev/null
curl -sf http://localhost:9200 >/dev/null && echo "ES UP" || echo "ES DOWN"
curl -sf http://localhost:8848/nacos/ >/dev/null && echo "Nacos UP" || echo "Nacos DOWN"
curl -sf http://localhost:8091/ >/dev/null && echo "Seata UP" || echo "Seata DOWN"
```

---

## 6. 启用中间件 (后端配置)

后端 application.yml **默认全部关掉** Nacos / Seata. 启用步骤:

### 6.1 启用 MySQL (默认开)

不用改, application.yml 已配:
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/agent_platform
    username: root
    password: root
```

### 6.2 启用 Redis (redisson)

`agent-common` 已经在, 但需要 `agent-common/src/main/resources/application.yml` 配 Redis 地址:
```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
```

启动时观察日志: 看到 `RedissonClient connected` 就 OK.

### 6.3 启用 ES (RAG)

`agent-knowledge` 的 application.yml:
```yaml
spring:
  elasticsearch:
    uris: http://127.0.0.1:9200
```

### 6.4 启用 Nacos

每个 `agent-*/application.yml`:
```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: 127.0.0.1:8848
      config:
        enabled: true
        server-addr: 127.0.0.1:8848
        file-extension: yaml
```

### 6.5 启用 Seata

`agent-agent/agent-conversation` (用 @GlobalTransactional) 的 application.yml:
```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: agent-platform-tx-group
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      username: nacos
      password: nacos
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      dataId: seataServer.properties
  service:
    vgroup-mapping:
      agent-platform-tx-group: default
    disable-global-transaction: false
```

---

## 7. 卸载

```bash
# 卸全部
./scripts/install-middleware.sh --uninstall all

# 卸指定
./scripts/install-middleware.sh --uninstall mysql
./scripts/install-middleware.sh --uninstall redis
./scripts/install-middleware.sh --uninstall elasticsearch
./scripts/install-middleware.sh --uninstall nacos
./scripts/install-middleware.sh --uninstall seata

# 完全清掉 (含数据卷)
./scripts/install-middleware.sh --uninstall -y mysql
docker volume rm agent-mysql-data
```

⚠️ **卸载会删数据**, 先备份再卸!

---

## 8. 常见问题

### Q: 装完 MySQL, Java 应用连不上, 报 `Connection refused`

A: 多半 `localhost` 解析为 IPv6 `::1`, MariaDB/MySQL 只监听 127.0.0.1 (IPv4).
**改用 127.0.0.1 不要用 localhost** (我们 application.yml 已用 127.0.0.1).

### Q: Nacos 报 `9848` 连接失败

A: Nacos 2.x gRPC 端口 (8848+1000). 防火墙要开放 9848, **不是 8848**.

```bash
# 验证端口都通
nc -zv localhost 8848
nc -zv localhost 9848
```

### Q: ES 启动报错 `max virtual memory areas vm.max_map_count [65530] is too low`

A: ES 要求 `vm.max_map_count >= 262144`:
```bash
sudo sysctl -w vm.max_map_count=262144
# 永久:
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

### Q: Seata 启动慢 (1-2 分钟)

A: 正常, Spring Boot 启动. 脚本默认 `JVM_XMS=256m JVM_XMX=512m`. 内存大可调高:
```bash
JVM_XMS=1g JVM_XMX=2g bash /opt/seata/bin/seata-server.sh
```

### Q: Redis 数据丢失

A: 默认配置**不持久化**. 改 `/etc/redis/redis.conf`:
```
appendonly yes
```
重启 `service redis-server restart`.

### Q: MySQL root 密码不对

A: 改 application.yml 的 `spring.datasource.password`, 或重置 root 密码.

### Q: Seata 报 `no available server to connect`

A: Seata 注册到 Nacos, 但 Nacos 找 TC 失败. 检查:
```bash
# 1. Nacos 是否启动
curl http://localhost:8848/nacos/

# 2. Seata 是否注册
# Web 控制台 → 服务管理 → 服务列表 → 看有没有 seata-server

# 3. Seata 日志
tail /opt/seata/logs/seata-server.log
```

### Q: 安装失败怎么排查

```bash
# 1. 详细输出
bash -x ./scripts/install-middleware.sh mysql

# 2. 看日志
tail -50 /var/log/seata.log
tail -50 /var/log/mariadbd.log

# 3. docker 日志
docker logs agent-mysql
docker logs agent-redis

# 4. 端口检查
ss -tlnp | grep 3306
ss -tlnp | grep 6379
```

### Q: 沙箱里 Seata 起不来

A: 沙箱会杀 background java 进程, 在你**自己电脑**上没这问题. 用 docker 模式 (`--mode=docker`) 可绕过.

---

## 附录: 命令速查

```bash
# 装
./scripts/install-middleware.sh -y all
./scripts/install-middleware.sh -y redis
./scripts/install-middleware.sh --mode=docker all

# 卸
./scripts/install-middleware.sh --uninstall -y all

# 验证
mysql -uroot -proot -e "SELECT 1"
redis-cli PING
curl -s http://localhost:9200 | head -5
curl -s http://localhost:8848/nacos/
curl -s http://localhost:8091/

# 启停 (native)
service mariadb status | start | stop | restart
service redis-server status | start | stop | restart
service elasticsearch status | start | stop | restart
bash /opt/nacos/bin/startup.sh -m standalone
bash /opt/nacos/bin/shutdown.sh
bash /opt/seata/bin/seata-server.sh start | stop

# 启停 (docker)
docker ps | grep agent-
docker logs -f agent-mysql
docker restart agent-mysql
```

---

最后更新: 2026-06
适用版本: v1.0.0
