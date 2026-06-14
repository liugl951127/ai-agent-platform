# 🌐 网络 & 防火墙 & Nginx 手册 (NETWORK-AND-FIREWALL)

> 配套: `scripts/firewall-setup.sh` + `nginx/` 配置 + `scripts/nginx-deploy.sh`

---

## 目录

- [0. 网络架构总览](#0-网络架构总览)
- [1. 端口清单](#1-端口清单)
- [2. 防火墙配置](#2-防火墙配置)
  - [2.1 一键脚本](#21-一键脚本)
  - [2.2 UFW 手动配置 (Ubuntu/Debian)](#22-ufw-手动配置-ubuntudebian)
  - [2.3 firewalld 手动配置 (CentOS/RHEL)](#23-firewalld-手动配置-centosrhel)
  - [2.4 iptables 手动配置 (通用)](#24-iptables-手动配置-通用)
- [3. Nginx 反向代理](#3-nginx-反向代理)
  - [3.1 一键部署](#31-一键部署)
  - [3.2 SSL 证书 (自签 / Let's Encrypt)](#32-ssl-证书-自签--lets-encrypt)
  - [3.3 反代配置详解](#33-反代配置详解)
- [4. 安全清单](#4-安全清单)
- [5. K8s/Docker 网络](#5-k8sdocker-网络)
- [6. 故障排查](#6-故障排查)

---

## 0. 网络架构总览

```
                       ┌─────────────────┐
                       │   公网/用户     │
                       └────────┬────────┘
                                │ HTTPS (443)
                       ┌────────▼────────┐
                       │  Nginx (:80/443)│  ← 反代 + SSL + 限流
                       │  /api/* → gateway│
                       │  /     → 前端 dist│
                       └────────┬────────┘
                                │ HTTP (内网)
        ┌───────────┬───────────┼───────────┬───────────┐
        │           │           │           │           │
   ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌───▼────┐ ┌────▼────┐
   │gateway  │ │  auth   │ │  llm    │ │ tools │ │  ...   │
   │  :9000  │ │  :9001  │ │  :9002  │ │ :9008 │ │       │
   └─────────┘ └─────────┘ └─────────┘ └───────┘ └───────┘
        │
        │  (Nacos 服务发现, 可选)
   ┌────▼────┐
   │  Nacos │
   │ :8848  │
   └─────────┘
```

**安全边界**:
- 互联网 ↔ Nginx (公开 80/443)
- Nginx ↔ 后端服务 (内网, 不直连公网)
- 后端服务 ↔ 中间件 (内网, 数据库/缓存)

---

## 1. 端口清单

| 端口 | 协议 | 服务 | 公网开放? | 备注 |
|---|---|---|---|---|
| 22 | TCP | SSH | ✅ | 服务器运维 |
| 80 | TCP | Nginx HTTP | ✅ | 自动跳 HTTPS |
| 443 | TCP | Nginx HTTPS | ✅ | **主入口** |
| 3306 | TCP | MySQL | ❌ | 仅内网 |
| 6379 | TCP | Redis | ❌ | 仅内网 |
| 8848 | TCP | Nacos | ❌ | 仅内网 (生产可开) |
| 9848 | TCP | Nacos gRPC | ❌ | 仅内网 |
| 8091 | TCP | Seata | ❌ | 仅内网 |
| 9200 | TCP | ES HTTP | ❌ | 仅内网 |
| 9300 | TCP | ES transport | ❌ | 仅内网 |
| 9000 | TCP | agent-gateway | ⚠️ | 可选 (Nginx 在前面) |
| 9001 | TCP | agent-auth | ❌ | |
| 9002 | TCP | agent-llm | ❌ | |
| 9003 | TCP | agent-workflow | ❌ | |
| 9004 | TCP | agent-knowledge | ❌ | |
| 9005 | TCP | agent-agent | ❌ | |
| 9006 | TCP | agent-conversation | ❌ | |
| 9007 | TCP | agent-system | ❌ | |
| 9008 | TCP | agent-tools | ❌ | |

**公网开放原则**: **只开 22 + 80 + 443** (SSH + HTTP + HTTPS), 其他全内网.

---

## 2. 防火墙配置

### 2.1 一键脚本

```bash
cd /path/to/ai-agent-platform
./scripts/firewall-setup.sh                # 装 + 启用 + 开放默认端口
./scripts/firewall-setup.sh -y             # 自动 yes
./scripts/firewall-setup.sh status        # 看状态
./scripts/firewall-setup.sh open 8081     # 加端口
./scripts/firewall-setup.sh open 9000/tcp # 加 TCP
./scripts/firewall-setup.sh allow 10.0.0.0/8   # 白名单网段
./scripts/firewall-setup.sh deny 1.2.3.4        # 封 IP
./scripts/firewall-setup.sh --uninstall -y all  # 卸
```

**自动检测**: 有 ufw 用 ufw, 有 firewalld 用 firewalld, 否则 iptables.

### 2.2 UFW 手动配置 (Ubuntu/Debian)

```bash
# 装
sudo apt install -y ufw

# 默认策略
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH 优先 (避免锁死自己)
sudo ufw allow 22/tcp comment "ssh"

# AI Agent Platform 端口
for p in 80 443 8848 9848 3306 6379 9200 9300 8091 8080 9000 9001 9002 9003 9004 9005 9006 9007 9008; do
  sudo ufw allow $p/tcp comment "agent-platform"
done

# 启用
sudo ufw --force enable

# 看
sudo ufw status verbose
```

**预期输出**:
```
Status: active
Logging: on (low)
Default: deny (incoming), allow (outgoing), disabled (routed)

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW IN    Anywhere       # ssh
80/tcp                     ALLOW IN    Anywhere       # agent-platform
443/tcp                    ALLOW IN    Anywhere       # agent-platform
...
```

**加自定义规则**:
```bash
# 只允许内网访问 3306
sudo ufw delete allow 3306/tcp
sudo ufw allow from 10.0.0.0/8 to any port 3306 proto tcp

# 封一个 IP
sudo ufw deny from 1.2.3.4

# 限速 SSH (防爆破)
sudo ufw limit 22/tcp

# 看编号删
sudo ufw status numbered
sudo ufw delete 5
```

### 2.3 firewalld 手动配置 (CentOS/RHEL)

```bash
sudo yum install -y firewalld
sudo systemctl enable --now firewalld

# 加端口
for p in 80 443 8848 9848 3306 6379 9200 9300 8091 8080 9000 9001 9002 9003 9004 9005 9006 9007 9008; do
  sudo firewall-cmd --permanent --add-port=$p/tcp
done

# 加载
sudo firewall-cmd --reload
sudo firewall-cmd --list-all

# 加白名单
sudo firewall-cmd --permanent --add-source=10.0.0.0/8 --zone=trusted
sudo firewall-cmd --reload

# 封 IP
sudo firewall-cmd --permanent --add-rich-rule='rule family=ipv4 source address=1.2.3.4 reject'
sudo firewall-cmd --reload
```

### 2.4 iptables 手动配置 (通用)

```bash
# 装持久化
sudo apt install -y iptables-persistent

# 默认规则
sudo iptables -F
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
sudo iptables -A INPUT -i lo -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
sudo iptables -A INPUT -p icmp --icmp-type echo-request -j ACCEPT

# 开放平台端口
for p in 80 443 8848 9848 3306 6379 9200 9300 8091 8080 9000 9001 9002 9003 9004 9005 9006 9007 9008; do
  sudo iptables -A INPUT -p tcp --dport $p -j ACCEPT
done

# 默认拒绝入站
sudo iptables -A INPUT -j DROP

# 持久化
sudo netfilter-persistent save
```

---

## 3. Nginx 反向代理

### 3.1 一键部署

```bash
# 装 nginx
sudo apt install -y nginx   # Debian/Ubuntu
# 或
sudo yum install -y nginx   # CentOS/RHEL

# 跑部署脚本 (替换 nginx.conf + 站点配置)
sudo ./scripts/nginx-deploy.sh
```

脚本会:
1. 备份原 `/etc/nginx/nginx.conf` (后缀 .bak.YYYYMMDDHHMMSS)
2. 复制 `nginx/nginx.conf` → `/etc/nginx/nginx.conf`
3. 复制 `nginx/sites-available/*` → `/etc/nginx/sites-available/`
4. symlink 到 `sites-enabled/`
5. 检查 `/etc/nginx/ssl/`, 没有就自签
6. `nginx -t` 测配置
7. `systemctl reload nginx`

### 3.2 SSL 证书

#### 方式 A: Let's Encrypt (生产推荐)

```bash
# 装 certbot
sudo apt install -y certbot python3-certbot-nginx

# 申请证书 (自动改 nginx 配置)
sudo certbot --nginx -d ai.your-domain.com

# 自动续期
sudo certbot renew --dry-run

# 证书位置
# /etc/letsencrypt/live/ai.your-domain.com/fullchain.pem
# /etc/letsencrypt/live/ai.your-domain.com/privkey.pem
```

**改 nginx 配置**:
```nginx
ssl_certificate     /etc/letsencrypt/live/ai.your-domain.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/ai.your-domain.com/privkey.pem;
```

#### 方式 B: 自签 (开发/内网)

```bash
sudo ./scripts/nginx-ssl-selfsigned.sh ai.your-domain.com
# 证书在 /etc/nginx/ssl/
# 浏览器会警告 "不安全", 仅开发用
```

#### 方式 C: 自有证书 (公司签发)

```bash
# 上传 crt + key 到 /etc/nginx/ssl/
sudo cp your-cert.crt /etc/nginx/ssl/ai.your-domain.com.crt
sudo cp your-cert.key /etc/nginx/ssl/ai.your-domain.com.key
sudo chmod 600 /etc/nginx/ssl/*.key
```

### 3.3 反代配置详解

主站配置 `nginx/sites-available/agent-platform.conf`:

```nginx
# 前端 SPA (Vue Router history 模式)
location / {
    try_files $uri $uri/ /index.html;
}

# 静态资源缓存
location ~* \.(js|css|png|jpg|svg|woff)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# 后端 API 走 gateway
location /api/ {
    proxy_pass http://agent_gateway/;
    proxy_http_version 1.1;
    
    # WebSocket / SSE 必须
    proxy_set_header Upgrade    $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    proxy_buffering off;     # 关掉, 否则 SSE 卡死
    proxy_cache off;          # API 不缓存
    proxy_read_timeout 120s; # 长连接超时
}

# LLM 调用限流
location /api/agent/chat {
    limit_req zone=req_per_ip burst=10 nodelay;  # 1 秒 30 个, 突发 10
    limit_conn conn_per_ip 5;                    # 单 IP 最多 5 并发
    proxy_read_timeout 300s;                     # LLM 慢
    proxy_buffering off;
}

# actuator 限内网
location ~ ^/api/.*/actuator/ {
    allow 10.0.0.0/8;
    allow 172.16.0.0/12;
    allow 192.168.0.0/16;
    deny  all;
}
```

**多服务独立域名** (内网): `nginx/sites-available/agent-platform-direct.conf` — 每个服务一个子域, 适合 K8s service mesh.

**upstream 池** 在 `nginx.conf` 的 `http {}` 块里:
```nginx
upstream agent_gateway { server 127.0.0.1:9000; keepalive 32; }
upstream agent_llm     { server 127.0.0.1:9002; keepalive 16; }
# ... 共 9 个
```

**keepalive 32/16**: Spring Boot 默认支持 HTTP/1.1 keep-alive, 这里设连接池避免反复握手.

---

## 4. 安全清单

### 必须做

- [ ] **HTTPS 强制**: HTTP 301 跳 HTTPS
- [ ] **HSTS 头**: `Strict-Transport-Security: max-age=31536000`
- [ ] **CSP 头**: 防 XSS
- [ ] **X-Frame-Options**: 防 clickjacking
- [ ] **SSH 改 2222**: 避开默认 22 (脚本里改下)
- [ ] **SSH 密钥登录**: 禁密码
- [ ] **MySQL root 改强密码**: 别用 `root/root`
- [ ] **Redis 设密码**: `requirepass xxx`
- [ ] **ES 关公网**: 防火墙不开 9200/9300
- [ ] **Nacos 改密码**: 默认 nacos/nacos 危险
- [ ] **后端服务只监听内网**: `server.address=127.0.0.1` 或防火墙
- [ ] **actuator 限内网**: 防信息泄露
- [ ] **JWT secret 改强**: `agent-common/application.yml` 的 `jwt.secret`

### 推荐做

- [ ] **fail2ban**: 防 SSH 爆破
- [ ] **限速 (rate limit)**: Nginx `limit_req` 已在配置里
- [ ] **WAF**: Cloudflare / ModSecurity
- [ ] **日志监控**: Loki / ELK
- [ ] **告警**: UptimeRobot / Prometheus AlertManager
- [ ] **WAF 规则**: SQL 注入 / XSS 黑名单

### 检查命令

```bash
# 1. 开放端口
ss -tlnp

# 2. SSL 评分
curl -sI https://ai.your-domain.com/ | head -5
# 或在线: https://www.ssllabs.com/ssltest/

# 3. 安全头
curl -sI https://ai.your-domain.com/ | grep -iE "x-frame|x-content|strict-transport|content-security"

# 4. 默认密码
grep -r "password.*=.*root\|nacos/nacos\|admin/admin" /opt/agent-* 2>/dev/null

# 5. JWT secret
grep "jwt.secret" agent-common/src/main/resources/application.yml

# 6. 后端监听地址
curl -s http://localhost:9000/actuator/env  # 应 401/403, 不能返回环境变量
```

---

## 5. K8s / Docker 网络

### Docker Compose 端口映射

`docker-compose.yml` 片段:
```yaml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/sites-available:/etc/nginx/sites-available:ro
      - ./nginx/sites-enabled:/etc/nginx/sites-enabled:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on: [agent-gateway]

  agent-gateway:
    build: ./agent-gateway
    expose: ["9000"]   # 不暴露公网, 只给 nginx
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

**原则**: 后端服务用 `expose` (内网), Nginx 用 `ports` (公网).

### K8s Ingress

`agent-platform-ingress.yaml`:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: agent-platform
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: 100m
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
spec:
  ingressClassName: nginx
  tls:
    - hosts: [ai.your-domain.com]
      secretName: ai-tls
  rules:
    - host: ai.your-domain.com
      http:
        paths:
          - path: /api/
            pathType: Prefix
            backend:
              service:
                name: agent-gateway
                port:
                  number: 9000
          - path: /
            pathType: Prefix
            backend:
              service:
                name: agent-ui
                port:
                  number: 80
```

### K8s NetworkPolicy

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: agent-platform
spec:
  podSelector: {}
  policyTypes: [Ingress, Egress]
  ingress:
    - from:
        - namespaceSelector: { matchLabels: { name: ingress-nginx } }
      ports:
        - port: 9000  # gateway
    # 微服务之间互访
    - from:
        - podSelector: { matchLabels: { app: agent-platform } }
```

---

## 6. 故障排查

### Q: `nginx: [emerg] unknown directive "more_clear_headers"`

A: 用了 nginx-extra 模块。Debian apt 装的不带。装上:
```bash
sudo apt install libnginx-mod-http-headers-more-filter
```

或注释掉 `nginx.conf` 那两行(用 `server_tokens off` 也够).

### Q: `nginx: [emerg] host not found in upstream "agent_xxx"`

A: 上游没启动 或 upstream 块没在 `http {}` 里.
```bash
# 检查
sudo nginx -T 2>&1 | grep upstream

# 重启服务
java -jar agent-xxx/target/agent-xxx-1.0.0.jar
```

### Q: HTTPS 502 Bad Gateway

A: Nginx → upstream 连不上。看 nginx 错误日志:
```bash
sudo tail -f /var/log/nginx/error.log
# "connect() failed (111: Connection refused)"  → 服务没起
# "no live upstreams"  → upstream 配置错
# "SSL handshake failed"  → 后端 HTTPS 证书错
```

### Q: 前端刷新 404

A: SPA history 模式, nginx 没配 `try_files`:
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### Q: LLM 流式输出卡住

A: Nginx 缓冲没关:
```nginx
proxy_buffering off;
proxy_cache off;
proxy_read_timeout 300s;
```

### Q: SSE (Server-Sent Events) 没反应

A: 同上, 加 `proxy_buffering off` 和 `Connection: upgrade` 头.

### Q: 浏览器报 `Mixed Content` (HTTPS 页加载 HTTP 资源)

A: 后端 `redirect_uri` 配置 https, 或前端代码用相对路径.

### Q: WebSocket 连接 1006 异常关闭

A: Nginx 没传 `Upgrade` 头:
```nginx
proxy_set_header Upgrade    $http_upgrade;
proxy_set_header Connection "upgrade";
```

### Q: 证书过期提醒

A: Let's Encrypt 90 天, certbot 自动续期. 自签 365 天, 用 cron 续:
```bash
0 0 1 1 * /scripts/nginx-ssl-selfsigned.sh ai.your-domain.com
```

---

## 附录: 完整部署示例

```bash
# ====== 1. 准备 ======
cd /opt
git clone https://github.com/liugl951127/ai-agent-platform.git
cd ai-agent-platform

# ====== 2. 装中间件 ======
./scripts/install-middleware.sh -y

# ====== 3. 编译 ======
mvn clean install -DskipTests
cd agent-ui && npm install && npm run build && cd ..

# ====== 4. 启动服务 ======
mvn -pl agent-tools package -DskipTests
nohup java -jar agent-tools/target/agent-tools-1.0.0.jar > /var/log/agent-tools.log 2>&1 &
# 其他 8 个服务同理

# ====== 5. 防火墙 ======
./scripts/firewall-setup.sh -y

# ====== 6. Nginx ======
./scripts/nginx-deploy.sh

# ====== 7. SSL (生产用 certbot, 开发用自签) ======
# certbot --nginx -d ai.your-domain.com
# 或
./scripts/nginx-ssl-selfsigned.sh ai.your-domain.com
sudo nginx -s reload

# ====== 7. 验证 ======
curl -I https://ai.your-domain.com/        # 200 OK
curl -I https://ai.your-domain.com/api/agent/list
```

完成!
