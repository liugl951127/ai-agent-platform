#!/usr/bin/env bash
# =============================================================================
# AI Agent Platform — 中间件一键安装脚本
# =============================================================================
# 安装: Nacos (服务发现+配置) / MySQL / Redis / Elasticsearch / Seata (分布式事务)
# 支持: Ubuntu / Debian / macOS / 任意支持 Docker 的系统
#
# 用法:
#   # 一键装全部 (推荐首次)
#   ./install-middleware.sh
#
#   # 装指定中间件
#   ./install-middleware.sh nacos
#   ./install-middleware.sh mysql
#   ./install-middleware.sh redis
#   ./install-middleware.sh elasticsearch
#   ./install-middleware.sh seata
#   ./install-middleware.sh all        # = 装全部
#
#   # 指定安装模式
#   ./install-middleware.sh --mode=docker all
#   ./install-middleware.sh --mode=native all
#   ./install-middleware.sh --mode=auto all    # 优先 docker, 失败降级 native
#
# 选项:
#   --mode=docker|native|auto    安装方式 (默认 auto)
#   --port=3306                  MySQL 端口 (默认 3306)
#   --es-version=8.11.0          ES 版本
#   --skip-health                装完不健康检查
#   --uninstall                  卸载
#   -y                           所有确认自动 yes
#   -h, --help                   帮助
# =============================================================================

set -e

# --------- 颜色 -------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${GREEN}[$(date +%H:%M:%S)]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERR ]${NC} $*" >&2; }
info() { echo -e "${BLUE}[INFO]${NC} $*"; }

# --------- 默认配置 -------
MYSQL_PORT=3306
MYSQL_ROOT_PWD="root"
REDIS_PORT=6379
REDIS_PWD=""
ES_VERSION="8.11.0"
ES_PORT=9200
NACOS_VERSION="2.3.1"
NACOS_PORT=8848
SEATA_VERSION="2.0.0"
SEATA_PORT=8091
INSTALL_MODE="auto"   # auto | docker | native
SKIP_HEALTH=0
AUTO_YES=0
ACTION="install"
COMPONENTS=()

# --------- 解析参数 ---------
while [[ $# -gt 0 ]]; do
  case $1 in
    --mode=*)         INSTALL_MODE="${1#*=}" ;;
    --port=*)         MYSQL_PORT="${1#*=}" ;;
    --es-version=*)   ES_VERSION="${1#*=}" ;;
    --uninstall)      ACTION="uninstall" ;;
    --skip-health)    SKIP_HEALTH=1 ;;
    -y)               AUTO_YES=1 ;;
    -h|--help)
      sed -n '3,30p' "$0"; exit 0 ;;
    nacos|mysql|redis|elasticsearch|es|seata|all)
      [ "$1" = "es" ] && COMPONENTS+=("elasticsearch") || COMPONENTS+=("$1")
      ;;
    *)
      err "未知参数: $1"; exit 1 ;;
  esac
  shift
done
[ ${#COMPONENTS[@]} -eq 0 ] && COMPONENTS=(all)

# --------- 工具函数 ---------
confirm() {
  if [ "$AUTO_YES" = "1" ]; then return 0; fi
  read -p "$1 [y/N] " -n 1 -r; echo
  [[ $REPLY =~ ^[Yy]$ ]]
}
have() { command -v "$1" >/dev/null 2>&1; }
port_used() { ss -tlnp 2>/dev/null | grep -q ":$1 " || netstat -tlnp 2>/dev/null | grep -q ":$1 "; }
ok() { echo -e "  ${GREEN}✓${NC} $1"; }
fail() { echo -e "  ${RED}✗${NC} $1"; }

detect_os() {
  if [ -f /etc/os-release ]; then
    . /etc/os-release; OS=$ID
  elif [ "$(uname)" = "Darwin" ]; then
    OS=darwin
  else
    OS=linux
  fi
  info "检测到系统: $OS"
}

check_docker() {
  if have docker; then
    if docker info >/dev/null 2>&1; then
      DOCKER_OK=1
    else
      warn "docker 已装但 daemon 未运行, 尝试启动..."
      systemctl start docker 2>/dev/null || service docker start 2>/dev/null || true
      sleep 2
      DOCKER_OK=$(docker info >/dev/null 2>&1 && echo 1 || echo 0)
    fi
  else
    DOCKER_OK=0
  fi
  [ "$DOCKER_OK" = "1" ] && ok "Docker 可用" || warn "Docker 不可用"
}

# 等待端口
wait_port() {
  local port=$1; local name=$2; local timeout=${3:-60}
  for i in $(seq 1 $timeout); do
    if port_used $port; then ok "$name 监听 $port"; return 0; fi
    sleep 1
  done
  fail "$name 60s 内未监听 $port"
  return 1
}

# =============================================================================
# MySQL
# =============================================================================
install_mysql() {
  log "[MySQL] 开始安装 (端口 $MYSQL_PORT, root/$MYSQL_ROOT_PWD) ..."
  if port_used $MYSQL_PORT; then ok "MySQL 已在 $MYSQL_PORT"; return; fi

  case $INSTALL_MODE in
    docker)
      docker run -d --name agent-mysql \
        -p $MYSQL_PORT:3306 \
        -e MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PWD \
        -e MYSQL_DATABASE=agent_platform \
        -e TZ=Asia/Shanghai \
        -v agent-mysql-data:/var/lib/mysql \
        --restart unless-stopped \
        mysql:8.0
      wait_port $MYSQL_PORT MySQL
      sleep 5
      docker exec agent-mysql mysql -uroot -p"$MYSQL_ROOT_PWD" -e \
        "CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$MYSQL_ROOT_PWD';
         GRANT ALL ON *.* TO 'root'@'127.0.0.1';
         FLUSH PRIVILEGES;" 2>/dev/null || true
      ;;
    native|auto)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq mariadb-server 2>&1 | tail -2
        mkdir -p /var/run/mysqld && chown -R mysql:mysql /var/run/mysqld 2>/dev/null
        nohup mariadbd --user=mysql --datadir=/var/lib/mysql \
          --socket=/var/run/mysqld/mysqld.sock --port=$MYSQL_PORT --bind-address=0.0.0.0 \
          > /var/log/mariadbd.log 2>&1 &
        wait_port $MYSQL_PORT MySQL
        sleep 3
        mariadb -uroot -e "
          CREATE DATABASE IF NOT EXISTS agent_platform DEFAULT CHARACTER SET utf8mb4;
          ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PWD';
          CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$MYSQL_ROOT_PWD';
          CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '$MYSQL_ROOT_PWD';
          GRANT ALL ON *.* TO 'root'@'127.0.0.1';
          GRANT ALL ON *.* TO 'root'@'%';
          FLUSH PRIVILEGES;" 2>/dev/null
      elif [ "$OS" = "darwin" ]; then
        if have brew; then
          brew install mysql@8 2>&1 | tail -2
          brew services start mysql@8
          wait_port $MYSQL_PORT MySQL
          mysql -uroot -e "
            CREATE DATABASE IF NOT EXISTS agent_platform DEFAULT CHARACTER SET utf8mb4;
            ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PWD';" 2>/dev/null
        else
          err "macOS 需先装 Homebrew"; return 1
        fi
      else
        err "原生安装不支持 $OS, 请用 --mode=docker"; return 1
      fi
      ;;
  esac
  ok "MySQL 启动成功, root 密码: $MYSQL_ROOT_PWD, 库: agent_platform"
}

uninstall_mysql() {
  case $INSTALL_MODE in
    docker) docker rm -f agent-mysql 2>/dev/null; docker volume rm agent-mysql-data 2>/dev/null; ok "MySQL 容器已删" ;;
    native)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        service mariadb stop 2>/dev/null; apt-get purge -y -qq mariadb-server 2>&1 | tail -1; ok "MariaDB 已卸载"
      fi
      ;;
  esac
}

# =============================================================================
# Redis
# =============================================================================
install_redis() {
  log "[Redis] 开始安装 (端口 $REDIS_PORT) ..."
  if port_used $REDIS_PORT; then ok "Redis 已在 $REDIS_PORT"; return; fi

  case $INSTALL_MODE in
    docker)
      docker run -d --name agent-redis \
        -p $REDIS_PORT:6379 \
        -v agent-redis-data:/data \
        --restart unless-stopped \
        ${REDIS_PWD:+--requirepass $REDIS_PWD} \
        redis:7-alpine
      wait_port $REDIS_PORT Redis
      ;;
    native|auto)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        apt-get install -y -qq redis-server 2>&1 | tail -1
        sed -i 's/^supervised no/supervised systemd/' /etc/redis/redis.conf 2>/dev/null || true
        sed -i "s/^bind 127.0.0.1.*/bind 0.0.0.0/" /etc/redis/redis.conf 2>/dev/null || true
        [ -n "$REDIS_PWD" ] && sed -i "s/^# requirepass .*/requirepass $REDIS_PWD/" /etc/redis/redis.conf
        service redis-server restart 2>/dev/null || redis-server --daemonize yes --port $REDIS_PORT
        wait_port $REDIS_PORT Redis
      elif [ "$OS" = "darwin" ]; then
        brew install redis && brew services start redis
        wait_port $REDIS_PORT Redis
      else
        err "不支持 $OS, 请用 --mode=docker"; return 1
      fi
      ;;
  esac
  ok "Redis 启动成功 @ $REDIS_PORT"
}

uninstall_redis() {
  case $INSTALL_MODE in
    docker) docker rm -f agent-redis 2>/dev/null; docker volume rm agent-redis-data 2>/dev/null; ok "Redis 容器已删" ;;
    native)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then apt-get purge -y -qq redis-server 2>&1 | tail -1; ok "Redis 已卸载"; fi
      ;;
  esac
}

# =============================================================================
# Elasticsearch
# =============================================================================
install_elasticsearch() {
  log "[Elasticsearch] 开始安装 (版本 $ES_VERSION, 端口 $ES_PORT) ..."
  if port_used $ES_PORT; then ok "ES 已在 $ES_PORT"; return; fi

  case $INSTALL_MODE in
    docker)
      docker run -d --name agent-elasticsearch \
        -p $ES_PORT:9200 \
        -e "discovery.type=single-node" \
        -e "xpack.security.enabled=false" \
        -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
        -v agent-es-data:/usr/share/elasticsearch/data \
        --restart unless-stopped \
        docker.elastic.co/elasticsearch/elasticsearch:$ES_VERSION
      wait_port $ES_PORT ES 90
      ;;
    native|auto)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        if ! have curl; then apt-get install -y -qq curl gnupg apt-transport-https; fi
        if [ ! -f /usr/share/keyrings/elasticsearch-keyring.gpg ]; then
          curl -fsSL https://artifacts.elastic.co/GPG-KEY-elasticsearch | gpg --dearmor -o /usr/share/keyrings/elasticsearch-keyring.gpg
          echo "deb [signed-by=/usr/share/keyrings/elasticsearch-keyring.gpg] https://artifacts.elastic.co/packages/8.x/apt stable main" > /etc/apt/sources.list.d/elastic-8.list
          apt-get update -qq
        fi
        apt-get install -y -qq elasticsearch=$ES_VERSION 2>&1 | tail -1
        sed -i 's/^-Xms.*/-Xms512m/' /etc/elasticsearch/jvm.options
        sed -i 's/^-Xmx.*/-Xmx512m/' /etc/elasticsearch/jvm.options
        sed -i 's/^#cluster.name: .*/cluster.name: agent-platform/' /etc/elasticsearch/elasticsearch.yml
        sed -i 's/^#network.host: .*/network.host: 0.0.0.0/' /etc/elasticsearch/elasticsearch.yml
        sed -i 's/^#http.port: .*/http.port: '"$ES_PORT"'/' /etc/elasticsearch/elasticsearch.yml
        sed -i 's/^-Xpack.security.enabled.*/xpack.security.enabled: false/' /etc/elasticsearch/elasticsearch.yml
        service elasticsearch start 2>/dev/null || systemctl start elasticsearch
        wait_port $ES_PORT ES 90
      else
        err "不支持 $OS 原生安装, 请用 --mode=docker"; return 1
      fi
      ;;
  esac
  ok "ES 启动成功 @ http://localhost:$ES_PORT"
}

uninstall_elasticsearch() {
  case $INSTALL_MODE in
    docker) docker rm -f agent-elasticsearch 2>/dev/null; docker volume rm agent-es-data 2>/dev/null; ok "ES 容器已删" ;;
    native)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then apt-get purge -y -qq elasticsearch 2>&1 | tail -1; ok "ES 已卸载"; fi
      ;;
  esac
}

# =============================================================================
# Nacos
# =============================================================================
install_nacos() {
  log "[Nacos] 开始安装 (版本 $NACOS_VERSION, 端口 $NACOS_PORT/9848) ..."
  if port_used $NACOS_PORT; then ok "Nacos 已在 $NACOS_PORT"; return; fi

  case $INSTALL_MODE in
    docker)
      docker run -d --name agent-nacos \
        -p $NACOS_PORT:8848 -p 9848:9848 \
        -e MODE=standalone \
        -e JVM_XMS=256m -e JVM_XMX=256m \
        -v agent-nacos-data:/home/nacos/data \
        --restart unless-stopped \
        nacos/nacos-server:v$NACOS_VERSION
      wait_port $NACOS_PORT Nacos 120
      ;;
    native|auto)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        local dir=/opt/nacos
        if [ ! -d $dir ]; then
          local tar=/tmp/nacos-server-$NACOS_VERSION.tar.gz
          if [ ! -f $tar ]; then
            for try in 1 2 3; do
              info "下载 Nacos $NACOS_VERSION (尝试 $try/3)..."
              curl -fSL --retry 3 --retry-delay 5 -o $tar \
                "https://github.com/alibaba/nacos/releases/download/$NACOS_VERSION/nacos-server-$NACOS_VERSION.tar.gz" && break
              sleep 3
            done
            [ ! -f $tar ] && { err "Nacos 下载失败, 请检查网络或改用 --mode=docker"; return 1; }
          fi
          mkdir -p $dir && tar -xzf $tar -C $dir --strip-components=1
        fi
        bash $dir/bin/startup.sh -m standalone
        wait_port $NACOS_PORT Nacos 120
      elif [ "$OS" = "darwin" ]; then
        brew install nacos 2>&1 | tail -1
        brew services start nacos
        wait_port $NACOS_PORT Nacos
      else
        err "不支持 $OS, 请用 --mode=docker"; return 1
      fi
      ;;
  esac
  ok "Nacos 启动成功 @ http://localhost:$NACOS_PORT/nacos (nacos/nacos)"
}

uninstall_nacos() {
  case $INSTALL_MODE in
    docker) docker rm -f agent-nacos 2>/dev/null; docker volume rm agent-nacos-data 2>/dev/null; ok "Nacos 容器已删" ;;
    native) [ -d /opt/nacos ] && bash /opt/nacos/bin/shutdown.sh 2>/dev/null; rm -rf /opt/nacos; ok "Nacos 已删" ;;
  esac
}

# =============================================================================
# Seata
# =============================================================================
install_seata() {
  log "[Seata] 开始安装 (版本 $SEATA_VERSION, 端口 $SEATA_PORT) ..."
  if port_used $SEATA_PORT; then ok "Seata 已在 $SEATA_PORT"; return; fi

  case $INSTALL_MODE in
    docker)
      docker run -d --name agent-seata \
        -p $SEATA_PORT:8091 \
        -e SEATA_MODE=file \
        -v agent-seata-data:/root/seata-data \
        --restart unless-stopped \
        seataio/seata-server:$SEATA_VERSION
      wait_port $SEATA_PORT Seata 120
      ;;
    native|auto)
      if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        local dir=/opt/seata
        if [ ! -d $dir ]; then
          local tar=/tmp/seata-$SEATA_VERSION.tar.gz
          if [ ! -f $tar ]; then
            # 重试 3 次, 网络不稳也能下
            for try in 1 2 3; do
              info "下载 Seata $SEATA_VERSION (尝试 $try/3)..."
              curl -fSL --retry 3 --retry-delay 5 -o $tar \
                "https://github.com/seata/seata/releases/download/v$SEATA_VERSION/seata-server-$SEATA_VERSION.tar.gz" && break
              warn "下载失败, 重试..."
              sleep 3
            done
            [ ! -f $tar ] && { err "Seata 下载失败, 请检查网络或改用 --mode=docker"; return 1; }
          fi
          mkdir -p $dir && tar -xzf $tar -C $dir --strip-components=1
        fi
        # JVM 内存调小 (默认 2G, 沙箱启动慢)
        # 用 setsid 完全脱离当前进程组, 避免 bash 退出后被 sandbox 杀
        setsid bash -c "env JVM_XMS=256m JVM_XMX=512m bash $dir/bin/seata-server.sh > /var/log/seata.log 2>&1" </dev/null >/dev/null 2>&1 &
        disown 2>/dev/null
        wait_port $SEATA_PORT Seata 180
      else
        err "不支持 $OS, 请用 --mode=docker"; return 1
      fi
      ;;
  esac
  ok "Seata 启动成功 @ http://localhost:$SEATA_PORT"
}

uninstall_seata() {
  case $INSTALL_MODE in
    docker) docker rm -f agent-seata 2>/dev/null; docker volume rm agent-seata-data 2>/dev/null; ok "Seata 容器已删" ;;
    native) pkill -f seata-server 2>/dev/null; rm -rf /opt/seata; ok "Seata 已删" ;;
  esac
}

# =============================================================================
# 健康检查
# =============================================================================
health_check() {
  log "[健康检查] 等待 10s 让服务完全启动 ..."
  sleep 10
  echo
  log "======= 健康检查 ======="

  [ ${#COMPONENTS[@]} -gt 0 ] && [[ " ${COMPONENTS[*]} " =~ " mysql " || " ${COMPONENTS[*]} " =~ " all " ]] && \
    { command -v mysql >/dev/null && mysql -h127.0.0.1 -P$MYSQL_PORT -uroot -p"$MYSQL_ROOT_PWD" -e "SELECT VERSION();" 2>/dev/null | head -2 && ok "MySQL OK" || fail "MySQL 检查失败"; }

  [[ " ${COMPONENTS[*]} " =~ " redis " || " ${COMPONENTS[*]} " =~ " all " ]] && \
    { command -v redis-cli >/dev/null && redis-cli -p $REDIS_PORT ${REDIS_PWD:+-a $REDIS_PWD} PING 2>/dev/null | head -1 && ok "Redis OK" || fail "Redis 检查失败"; }

  [[ " ${COMPONENTS[*]} " =~ " elasticsearch " || " ${COMPONENTS[*]} " =~ " all " ]] && \
    { curl -sf http://localhost:$ES_PORT | head -c 100 2>/dev/null && ok "ES OK" || fail "ES 检查失败"; }

  [[ " ${COMPONENTS[*]} " =~ " nacos " || " ${COMPONENTS[*]} " =~ " all " ]] && \
    { curl -sf http://localhost:$NACOS_PORT/nacos/ | head -c 50 2>/dev/null && ok "Nacos OK" || fail "Nacos 检查失败"; }

  [[ " ${COMPONENTS[*]} " =~ " seata " || " ${COMPONENTS[*]} " =~ " all " ]] && \
    { curl -sf http://localhost:$SEATA_PORT/ | head -c 50 2>/dev/null && ok "Seata OK" || fail "Seata 检查失败"; }

  echo
  log "✅ 安装完成!"
  echo
  info "下一步:"
  echo "  1. 启用服务注册: 编辑各 agent-*/application.yml 把 enabled: false 改 true"
  echo "  2. 启动后端: java -jar agent-xxx/target/agent-xxx-1.0.0.jar"
  echo "  3. 启动前端: cd agent-ui && npm run dev"
  echo "  4. 打开浏览器: http://localhost:8080 (admin/123456)"
}

# =============================================================================
# Main
# =============================================================================
main() {
  echo
  log "🚀 AI Agent Platform 中间件安装器"
  log "==============================================="
  log "模式: $INSTALL_MODE | 组件: ${COMPONENTS[*]}"
  echo
  detect_os
  check_docker

  # 模式决定
  if [ "$INSTALL_MODE" = "auto" ]; then
    if [ "$DOCKER_OK" = "1" ]; then
      INSTALL_MODE="docker"; info "使用 Docker 模式"
    else
      INSTALL_MODE="native"; info "Docker 不可用, 降级到 native (apt/brew)"
    fi
  fi

  if [ "$INSTALL_MODE" = "docker" ] && [ "$DOCKER_OK" != "1" ]; then
    err "Docker 不可用, 请先启动 Docker 或改用 --mode=native"; exit 1
  fi

  # 解析 all
  if [[ " ${COMPONENTS[*]} " =~ " all " ]]; then
    COMPONENTS=(mysql redis elasticsearch nacos seata)
  fi

  for c in "${COMPONENTS[@]}"; do
    if [ "$ACTION" = "install" ]; then
      "install_$c" || warn "$c 安装失败, 继续其他组件"
    else
      "uninstall_$c" || warn "$c 卸载失败, 继续"
    fi
  done

  [ "$ACTION" = "install" ] && [ "$SKIP_HEALTH" != "1" ] && health_check
}

main "$@"
