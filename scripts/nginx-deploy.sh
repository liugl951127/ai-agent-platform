#!/usr/bin/env bash
# =============================================================================
# AI Agent Platform — Nginx 一键部署
# =============================================================================
# 把 nginx/ 目录的 config 部署到 /etc/nginx/
# =============================================================================

set -e
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[$(date +%H:%M:%S)]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }

SUDO=""
command -v sudo >/dev/null && [ "$EUID" != "0" ] && SUDO="sudo"

SCRIPT_DIR=$(cd $(dirname $0)/.. && pwd)
NGINX_DIR=$SCRIPT_DIR/nginx

if [ ! -d "$NGINX_DIR" ]; then
  echo "找不到 nginx/ 目录: $NGINX_DIR"
  exit 1
fi

# 备份原配置
BACKUP=/etc/nginx/nginx.conf.bak.$(date +%Y%m%d%H%M%S)
if [ -f /etc/nginx/nginx.conf ]; then
  $SUDO cp /etc/nginx/nginx.conf $BACKUP
  log "已备份原配置: $BACKUP"
fi

# 部署
log "部署 nginx.conf..."
$SUDO cp $NGINX_DIR/nginx.conf /etc/nginx/nginx.conf

log "部署站点配置..."
$SUDO mkdir -p /etc/nginx/sites-available /etc/nginx/sites-enabled
for f in $NGINX_DIR/sites-available/*.conf; do
  name=$(basename $f)
  $SUDO cp $f /etc/nginx/sites-available/$name
  if [ ! -e /etc/nginx/sites-enabled/$name ]; then
    $SUDO ln -s /etc/nginx/sites-available/$name /etc/nginx/sites-enabled/$name
  fi
  log "  ✓ $name"
done

# SSL
if [ ! -d /etc/nginx/ssl ] || [ -z "$(ls -A /etc/nginx/ssl/ 2>/dev/null)" ]; then
  warn "/etc/nginx/ssl/ 目录无证书, 先自签生成 (生产用 Let's Encrypt)"
  bash $SCRIPT_DIR/scripts/nginx-ssl-selfsigned.sh ai.your-domain.com
fi

# 测
log "测配置..."
$SUDO nginx -t

# 启用 + reload
log "重载 nginx..."
$SUDO systemctl enable nginx 2>/dev/null || true
$SUDO systemctl reload nginx 2>/dev/null || $SUDO nginx -s reload

log "✅ 部署完成"
echo
log "访问:"
echo "  HTTP  : http://$(hostname -I | awk '{print $1}')/"
echo "  HTTPS : https://$(hostname -I | awk '{print $1}')/"
echo
log "运维命令:"
echo "  nginx -t                # 测配置"
echo "  nginx -s reload         # 热重载"
echo "  tail -f /var/log/nginx/access.log"
