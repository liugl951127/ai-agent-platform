#!/usr/bin/env bash
# =============================================================================
# AI Agent Platform — Nginx SSL 自签证书 (开发/内网用)
# =============================================================================
# 适用: 开发环境 / 内网 / 没正式证书时
# 生产环境请用 Let's Encrypt: certbot --nginx -d your-domain.com
# =============================================================================

set -e
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[$(date +%H:%M:%S)]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }

DOMAIN=${1:-ai.your-domain.com}
WILDCARD=${2:-_.$DOMAIN}   # 通配符证书, 默认 _.your-domain.com
DAYS=365
SSL_DIR=/etc/nginx/ssl

# 检测 sudo
SUDO=""
command -v sudo >/dev/null && [ "$EUID" != "0" ] && SUDO="sudo"

log "生成自签证书:"
log "  域名: $DOMAIN"
log "  通配符: $WILDCARD"
log "  有效期: $DAYS 天"
log "  输出: $SSL_DIR"
warn "⚠️  仅用于开发/内网! 生产请用 Let's Encrypt"

$SUDO mkdir -p $SSL_DIR

# DH params (2048 位, 共享)
if [ ! -f $SSL_DIR/dhparam.pem ]; then
  log "生成 DH params (2048 位, ~30s)..."
  $SUDO openssl dhparam -out $SSL_DIR/dhparam.pem 2048
fi

# 单域名证书
log "生成 $DOMAIN.crt..."
$SUDO openssl req -x509 -nodes -days $DAYS -newkey rsa:2048 \
  -keyout $SSL_DIR/$DOMAIN.key \
  -out $SSL_DIR/$DOMAIN.crt \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=AgentPlatform/CN=$DOMAIN" \
  -addext "subjectAltName=DNS:$DOMAIN,DNS:www.$DOMAIN"

# 通配符证书 (内网多服务用)
if [ -n "$WILDCARD" ]; then
  log "生成 $WILDCARD.crt (通配符)..."
  $SUDO openssl req -x509 -nodes -days $DAYS -newkey rsa:2048 \
    -keyout $SSL_DIR/$WILDCARD.key \
    -out $SSL_DIR/$WILDCARD.crt \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=AgentPlatform/CN=$WILDCARD" \
    -addext "subjectAltName=DNS:$WILDCARD"
fi

$SUDO chmod 600 $SSL_DIR/*.key
$SUDO chmod 644 $SSL_DIR/*.crt $SSL_DIR/dhparam.pem
ls -la $SSL_DIR/

# 测
if command -v nginx >/dev/null; then
  $SUDO nginx -t && log "✅ nginx 配置 OK"
fi

log ""
log "用法 (生产用 Let's Encrypt 替代):"
echo "  # 1. 申请正式证书"
echo "  apt install certbot python3-certbot-nginx"
echo "  certbot --nginx -d $DOMAIN"
echo ""
echo "  # 2. 或继续用自签 (浏览器会警告):"
echo "  nginx -s reload"
