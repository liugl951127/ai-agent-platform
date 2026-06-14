#!/usr/bin/env bash
# =============================================================================
# AI Agent Platform — 防火墙一键配置
# =============================================================================
# 适用: Ubuntu / Debian (ufw + iptables) / CentOS / RHEL / Rocky (firewalld) / 通用 (iptables)
# 默认开放本平台所需全部端口, 默认白名单 SSH
#
# 用法:
#   ./firewall-setup.sh install     # 安装并启用防火墙 + 开放本平台端口
#   ./firewall-setup.sh status      # 看当前状态
#   ./firewall-setup.sh open 8080   # 加一个端口
#   ./firewall-setup.sh open 9000/tcp  # 加一个 tcp 端口
#   ./firewall-setup.sh allow 192.168.1.0/24  # 白名单一段 IP
#   ./firewall-setup.sh deny 1.2.3.4   # 封一个 IP
#   ./firewall-setup.sh reset       # 重置 (全清, 危险)
#   ./firewall-setup.sh uninstall   # 关闭并卸载防火墙
#   -y                               自动 yes
# =============================================================================

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[$(date +%H:%M:%S)]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERR ]${NC} $*" >&2; }

# AI Agent Platform 默认需要开放的端口
DEFAULT_PORTS_TCP=(
  22       # SSH
  80 443   # HTTP/HTTPS
  8848     # Nacos HTTP
  9848     # Nacos gRPC
  3306     # MySQL
  6379     # Redis
  9200     # Elasticsearch HTTP
  9300     # Elasticsearch transport
  8091     # Seata
  8080     # 前端 Vite dev / Nginx
  9000     # agent-gateway
  9001     # agent-auth
  9002     # agent-llm
  9003     # agent-workflow
  9004     # agent-knowledge
  9005     # agent-agent
  9006     # agent-conversation
  9007     # agent-system
  9008     # agent-tools
)

AUTO_YES=0
ACTION="install"
EXTRA_PORTS=()
EXTRA_ALLOW_IPS=()
EXTRA_DENY_IPS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    install|status|reset|uninstall) ACTION=$1 ;;
    open)       EXTRA_PORTS+=("$2"); shift ;;
    allow)      EXTRA_ALLOW_IPS+=("$2"); shift ;;
    deny)       EXTRA_DENY_IPS+=("$2"); shift ;;
    -y)         AUTO_YES=1 ;;
    -h|--help)
      sed -n '3,18p' "$0"; exit 0 ;;
    *) err "未知: $1"; exit 1 ;;
  esac
  shift
done

# --------- 检测系统与防火墙类型 ---------
detect() {
  if [ -f /etc/os-release ]; then
    . /etc/os-release; OS=$ID; VER=$VERSION_ID
  else
    OS=linux
  fi
  log "系统: $OS $VER"

  # 优先级: ufw > firewalld > iptables
  if command -v ufw >/dev/null; then
    FW=ufw
  elif command -v firewall-cmd >/dev/null; then
    FW=firewalld
  elif command -v iptables >/dev/null; then
    FW=iptables
  else
    FW=none
  fi
  log "检测到防火墙: $FW"
}

# --------- 安装防火墙 ---------
install_fw() {
  log "安装防火墙..."
  case $OS in
    ubuntu|debian)
      export DEBIAN_FRONTEND=noninteractive
      apt-get update -qq 2>/dev/null || true
      if [[ " $DEFAULT_PORTS_TCP $EXTRA_PORTS " =~ " 22 " ]] || [ -z "$FW" ]; then
        apt-get install -y -qq ufw 2>&1 | tail -2
        FW=ufw
      fi
      ;;
    centos|rhel|rocky|almalinux|ol)
      if ! command -v firewall-cmd >/dev/null; then
        yum install -y -q firewalld 2>&1 | tail -2
        systemctl enable --now firewalld
      fi
      FW=firewalld
      ;;
  esac
  log "防火墙就绪: $FW"
}

# --------- 启用 + 开放本平台默认端口 ---------
apply_defaults() {
  log "开放 AI Agent Platform 默认端口 (${#DEFAULT_PORTS_TCP[@]} 个 TCP)..."
  for p in "${DEFAULT_PORTS_TCP[@]}"; do
    case $FW in
      ufw)
        ufw allow "$p/tcp" comment "agent-platform" >/dev/null
        ;;
      firewalld)
        firewall-cmd --permanent --add-port="$p/tcp" >/dev/null
        ;;
      iptables)
        iptables -A INPUT -p tcp --dport "$p" -j ACCEPT
        ;;
    esac
  done
  case $FW in
    firewalld) firewall-cmd --reload >/dev/null ;;
    iptables)
      if command -v netfilter-persistent >/dev/null; then netfilter-persistent save; fi
      ;;
  esac
  log "默认端口已开放"
}

# --------- 装 + 应用 ---------
do_install() {
  if [ "$FW" = "none" ]; then install_fw; fi

  case $FW in
    ufw)
      ufw --force reset
      ufw default deny incoming
      ufw default allow outgoing
      # SSH 优先, 避免锁死
      ufw allow 22/tcp comment "ssh"
      apply_defaults
      ufw --force enable
      ;;
    firewalld)
      systemctl enable --now firewalld
      firewall-cmd --permanent --zone=public --set-target=ACCEPT 2>/dev/null || true
      firewall-cmd --permanent --zone=public --add-service=ssh
      firewall-cmd --reload
      apply_defaults
      ;;
    iptables)
      # 默认策略: 接受已建立的, 丢弃新的 (除 SSH)
      iptables -F INPUT
      iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
      iptables -A INPUT -i lo -j ACCEPT
      iptables -A INPUT -p tcp --dport 22 -j ACCEPT
      iptables -A INPUT -p icmp --icmp-type echo-request -j ACCEPT
      apply_defaults
      iptables -A INPUT -j DROP
      if command -v netfilter-persistent >/dev/null; then
        netfilter-persistent save
        log "规则已持久化"
      else
        warn "建议安装 iptables-persistent: apt install iptables-persistent"
      fi
      ;;
  esac
  log "✅ 防火墙已就绪"
  do_status
}

# --------- 状态 ---------
do_status() {
  log "======= 防火墙状态 ======="
  case $FW in
    ufw)
      ufw status verbose
      ;;
    firewalld)
      firewall-cmd --state
      firewall-cmd --list-all
      ;;
    iptables)
      iptables -L INPUT -n --line-numbers | head -30
      echo
      log "开放端口: $(ss -tlnp 2>/dev/null | awk 'NR>1 {split($4,a,":"); print a[2]}' | sort -u | tr '\n' ' ')"
      ;;
    none) warn "无防火墙" ;;
  esac
}

# --------- 加端口 ---------
do_open() {
  [ ${#EXTRA_PORTS[@]} -eq 0 ] && { err "用法: $0 open <port>"; exit 1; }
  for p in "${EXTRA_PORTS[@]}"; do
    case $FW in
      ufw) ufw allow "$p" ;;
      firewalld) firewall-cmd --permanent --add-port="$p"; firewall-cmd --reload ;;
      iptables) iptables -A INPUT -p tcp --dport "$p" -j ACCEPT ;;
    esac
    log "已开放 $p"
  done
}

# --------- 白名单 IP ---------
do_allow() {
  [ ${#EXTRA_ALLOW_IPS[@]} -eq 0 ] && { err "用法: $0 allow <cidr>"; exit 1; }
  for ip in "${EXTRA_ALLOW_IPS[@]}"; do
    case $FW in
      ufw) ufw allow from "$ip" ;;
      firewalld) firewall-cmd --permanent --add-source="$ip" --zone=trusted; firewall-cmd --reload ;;
      iptables) iptables -A INPUT -s "$ip" -j ACCEPT ;;
    esac
    log "已白名单 $ip"
  done
}

# --------- 封 IP ---------
do_deny() {
  [ ${#EXTRA_DENY_IPS[@]} -eq 0 ] && { err "用法: $0 deny <ip>"; exit 1; }
  for ip in "${EXTRA_DENY_IPS[@]}"; do
    case $FW in
      ufw) ufw deny from "$ip" ;;
      firewalld) firewall-cmd --permanent --add-rich-rule="rule family=ipv4 source address=$ip reject" ;;
      iptables) iptables -A INPUT -s "$ip" -j DROP ;;
    esac
    log "已封 $ip"
  done
}

# --------- 重置 ---------
do_reset() {
  warn "!!! 重置会清空所有规则 !!!"
  if [ "$AUTO_YES" != "1" ]; then
    read -p "确认? (输入 RESET 继续) " -r
    [ "$REPLY" != "RESET" ] && { log "取消"; exit 0; }
  fi
  case $FW in
    ufw) ufw --force reset ;;
    firewalld) firewall-cmd --permanent --remove-port=1-65535/tcp 2>/dev/null; firewall-cmd --permanent --remove-port=1-65535/udp 2>/dev/null; firewall-cmd --reload ;;
    iptables) iptables -F INPUT; iptables -P INPUT ACCEPT ;;
  esac
  log "已重置"
}

# --------- 卸载 ---------
do_uninstall() {
  case $FW in
    ufw)
      ufw --force disable
      case $OS in ubuntu|debian) apt-get purge -y -qq ufw 2>&1 | tail -1;; esac
      ;;
    firewalld)
      systemctl disable --now firewalld
      case $OS in centos|rhel|rocky|*) yum remove -y -q firewalld 2>&1 | tail -1;; esac
      ;;
    iptables)
      iptables -F INPUT
      iptables -P INPUT ACCEPT
      if command -v netfilter-persistent >/dev/null; then netfilter-persistent save; fi
      ;;
  esac
  log "防火墙已禁用"
}

# --------- main ---------
detect
case $ACTION in
  install)    do_install ;;
  status)     do_status ;;
  open)       do_open ;;
  allow)      do_allow ;;
  deny)       do_deny ;;
  reset)      do_reset ;;
  uninstall)  do_uninstall ;;
esac
