#!/usr/bin/env bash
#
# bizmsg 统一运行脚本
#   一条命令管理 Nacos、RabbitMQ、后端微服务（gateway / business / message）与前端管理系统（web-admin）
#
# 用法：./run.sh <命令> [服务...]
#   start [服务...]     启动服务（默认 all；all 顺序：nacos -> mq -> gateway -> business -> message -> web）
#   stop  [服务...]     停止服务（默认 all，按启动的逆序停止）
#   restart [服务...]   重启服务
#   status              查看所有服务的运行状态与访问地址
#   logs <服务>         持续跟踪某个服务的日志（Ctrl+C 退出，不影响服务运行）
#   build               构建全部后端模块与前端产物
#   import-config       把 nacos/config/*.yml 发布到 Nacos 配置中心
#   help                查看帮助
#
# 服务名：nacos | mq | gateway | business | message | web | all
#   mq = RabbitMQ（5672/15672）、gateway = gateway-service（8080）
#   business = business-service（8082，商品房转移业务）、message = message-service（8081，报文生成 + 落盘回执）
#   web = web-admin 前端（5173）
#
set -euo pipefail

# 基础变量
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="${ROOT_DIR}/.run"
LOG_DIR="${RUN_DIR}/logs"
PID_DIR="${RUN_DIR}/pids"

# Nacos 地址与命名空间，可用环境变量覆盖后执行，例如 NACOS_ADDR=192.168.1.10:8848 ./run.sh start nacos
NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-bizmsg}"
# 单个服务等待端口就绪的最长秒数（首次启动要编译 + 拉依赖，可调大）
START_TIMEOUT="${START_TIMEOUT:-240}"
# 停止服务时等待优雅退出的秒数
STOP_TIMEOUT="${STOP_TIMEOUT:-15}"

MVN_BIN="${MVN_BIN:-mvn}"
NPM_BIN="${NPM_BIN:-npm}"
DOCKER_BIN="${DOCKER_BIN:-docker}"

if [ -t 1 ]; then
  C_RED=$'\033[31m'
  C_GREEN=$'\033[32m'
  C_YELLOW=$'\033[33m'
  C_CYAN=$'\033[36m'
  C_END=$'\033[0m'
else
  C_RED=''
  C_GREEN=''
  C_YELLOW=''
  C_CYAN=''
  C_END=''
fi

# 输出工具
info() { printf '%s[INFO]%s %s\n' "${C_CYAN}" "${C_END}" "$*"; }
ok() { printf '%s[ OK ]%s %s\n' "${C_GREEN}" "${C_END}" "$*"; }
warn() { printf '%s[WARN]%s %s\n' "${C_YELLOW}" "${C_END}" "$*"; }
err() { printf '%s[FAIL]%s %s\n' "${C_RED}" "${C_END}" "$*" >&2; }

require_cmd() {
  local bin="$1" label="$2"
  if ! command -v "${bin}" >/dev/null 2>&1; then
    err "未找到 ${label}（${bin}），请先安装或通过环境变量指定可执行文件路径"
    return 1
  fi
}

ensure_dirs() {
  mkdir -p "${LOG_DIR}" "${PID_DIR}"
}

# 服务元数据
# 输出服务的中文描述，用于日志与控制台提示
service_desc() {
  case "$1" in
    nacos) echo 'Nacos 注册/配置中心' ;;
    mq) echo 'RabbitMQ 消息队列' ;;
    gateway) echo 'gateway-service 网关' ;;
    business) echo 'business-service 业务服务' ;;
    message) echo 'message-service 报文服务' ;;
    web) echo 'web-admin 前端管理系统' ;;
    *) echo "$1" ;;
  esac
}

service_port() {
  case "$1" in
    nacos) echo 8848 ;;
    mq) echo 5672 ;;
    gateway) echo 8080 ;;
    business) echo 8082 ;;
    message) echo 8081 ;;
    web) echo 5173 ;;
    *) return 1 ;;
  esac
}

service_module() {
  case "$1" in
    gateway) echo gateway-service ;;
    business) echo business-service ;;
    message) echo message-service ;;
    *) echo '' ;;
  esac
}

# 把用户输入的服务名/别名统一成脚本内部名称，all 按依赖顺序展开
resolve_targets() {
  local raw="$*" item out=''
  if [ -z "${raw// /}" ]; then
    raw='all'
  fi
  for item in ${raw}; do
    case "${item}" in
      # 依赖顺序：先基础设施（nacos、mq），再网关，最后业务与报文
      all) out="${out} nacos mq gateway business message web" ;;
      nacos) out="${out} nacos" ;;
      mq | rabbitmq | rabbit) out="${out} mq" ;;
      gateway | gateway-service) out="${out} gateway" ;;
      business | business-service) out="${out} business" ;;
      message | message-service) out="${out} message" ;;
      web | web-admin | admin) out="${out} web" ;;
      *)
        err "未知服务：${item}（可选：nacos mq gateway business message web all）"
        return 1
        ;;
    esac
  done
  echo "${out}"
}

reverse_words() {
  local out='' word
  for word in $1; do
    out="${word} ${out}"
  done
  echo "${out}"
}

# 进程 / 端口
# 列出监听指定端口的进程号；lsof 无结果时返回码非 0，这里统一吞掉
port_pids() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN -t 2>/dev/null || true
}

port_pid() {
  port_pids "$1" | head -n 1
}

is_listening() {
  [ -n "$(port_pid "$1")" ]
}

pid_file_of() {
  echo "${PID_DIR}/$1.pid"
}

read_pid() {
  local file
  file="$(pid_file_of "$1")"
  if [ -f "${file}" ]; then
    cat "${file}"
  fi
}

# 轮询等待端口进入监听状态
wait_port() {
  local port="$1" timeout="${2:-60}" waited=0
  while [ "${waited}" -lt "${timeout}" ]; do
    if is_listening "${port}"; then
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  return 1
}

# 轮询等待 HTTP 探活接口返回成功
wait_url() {
  local url="$1" timeout="${2:-60}" waited=0
  while [ "${waited}" -lt "${timeout}" ]; do
    if curl -fsS -o /dev/null --max-time 3 "${url}" 2>/dev/null; then
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  return 1
}

# 先 SIGTERM 优雅退出，超时后再 SIGKILL；同时结束 mvn/npm fork 出来的子进程
terminate_pid() {
  local pid="$1" waited=0
  if [ -z "${pid}" ] || ! kill -0 "${pid}" 2>/dev/null; then
    return 0
  fi
  kill "${pid}" 2>/dev/null || true
  pkill -TERM -P "${pid}" 2>/dev/null || true
  while [ "${waited}" -lt "${STOP_TIMEOUT}" ]; do
    if ! kill -0 "${pid}" 2>/dev/null; then
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  warn "进程 ${pid} 未在 ${STOP_TIMEOUT}s 内退出，强制结束"
  kill -9 "${pid}" 2>/dev/null || true
  pkill -9 -P "${pid}" 2>/dev/null || true
}

# 兜底：按端口找出残留进程并结束
stop_by_port() {
  local port="$1" pid pids found=1
  pids="$(port_pids "${port}")"
  if [ -z "${pids}" ]; then
    return 1
  fi
  for pid in ${pids}; do
    info "结束端口 ${port} 上的进程 ${pid}"
    terminate_pid "${pid}"
    found=0
  done
  return "${found}"
}

# Nacos
# 兼容 docker compose（v2 插件）与独立的 docker-compose 命令
compose() {
  if "${DOCKER_BIN}" compose version >/dev/null 2>&1; then
    "${DOCKER_BIN}" compose -f "${ROOT_DIR}/nacos/docker-compose.yml" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "${ROOT_DIR}/nacos/docker-compose.yml" "$@"
  else
    err '未找到 docker compose / docker-compose，请先安装 Docker Desktop'
    return 1
  fi
}

import_configs() {
  local script="${ROOT_DIR}/nacos/scripts/import-config.sh"
  if [ ! -f "${script}" ]; then
    warn "未找到 ${script}，跳过配置发布"
    return 0
  fi
  info "发布 nacos/config/*.yml 到 Nacos（命名空间 ${NACOS_NAMESPACE}）"
  NACOS_ADDR="${NACOS_ADDR}" NACOS_NAMESPACE="${NACOS_NAMESPACE}" bash "${script}"
}

start_nacos() {
  if is_listening 8848; then
    ok 'Nacos 已在运行'
    import_configs
    return 0
  fi
  require_cmd "${DOCKER_BIN}" 'Docker' || return 1
  if ! "${DOCKER_BIN}" info >/dev/null 2>&1; then
    err 'Docker 守护进程未运行，请先启动 Docker Desktop'
    return 1
  fi
  info '启动 Nacos 容器（nacos/nacos-server:v2.2.3，standalone）'
  # 只起 nacos 这一个服务：compose 文件里还有 rabbitmq，由 ./run.sh start mq 单独管理
  if ! compose up -d nacos; then
    err 'Nacos 容器启动失败，请检查 Docker 与 nacos/docker-compose.yml'
    return 1
  fi
  info "等待 Nacos 就绪（最长 180s）..."
  if ! wait_url "http://${NACOS_ADDR}/nacos/v1/console/health/readiness" 180; then
    err "Nacos 未在预期时间内就绪，请查看容器日志：${DOCKER_BIN} logs bizmsg-nacos"
    return 1
  fi
  ok "Nacos 就绪 → http://${NACOS_ADDR}/nacos （默认账号 nacos/nacos）"
  import_configs || return 1
}

# RabbitMQ
# 与 Nacos 同处 nacos/docker-compose.yml，但作为独立服务管理，启停互不影响
start_mq() {
  if is_listening 5672; then
    ok 'RabbitMQ 已在运行'
    return 0
  fi
  require_cmd "${DOCKER_BIN}" 'Docker' || return 1
  if ! "${DOCKER_BIN}" info >/dev/null 2>&1; then
    err 'Docker 守护进程未运行，请先启动 Docker Desktop'
    return 1
  fi
  info '启动 RabbitMQ 容器（rabbitmq:3.13-management）'
  if ! compose up -d rabbitmq; then
    err 'RabbitMQ 容器启动失败，请检查 Docker 与 nacos/docker-compose.yml'
    return 1
  fi
  info '等待 RabbitMQ 就绪（最长 180s）...'
  # 用 compose 里定义的 healthcheck 结果判定，比等端口更准：5672 通了不代表 broker 已能正常收发
  local waited=0
  while [ "${waited}" -lt 180 ]; do
    if [ "$("${DOCKER_BIN}" inspect -f '{{.State.Health.Status}}' bizmsg-rabbitmq 2>/dev/null)" = 'healthy' ]; then
      ok 'RabbitMQ 就绪 → 管理台 http://127.0.0.1:15672 （bizmsg/bizmsg）'
      return 0
    fi
    sleep 2
    waited=$((waited + 2))
  done
  err "RabbitMQ 未在预期时间内就绪，请查看容器日志：${DOCKER_BIN} logs bizmsg-rabbitmq"
  return 1
}

# 后端服务
start_backend() {
  local name="$1" module port log_file pid_file
  module="$(service_module "${name}")"
  port="$(service_port "${name}")"
  log_file="${LOG_DIR}/${name}.log"
  pid_file="$(pid_file_of "${name}")"

  if is_listening "${port}"; then
    warn "$(service_desc "${name}") 已在运行（端口 ${port}），跳过启动"
    return 0
  fi
  require_cmd "${MVN_BIN}" 'Maven' || return 1
  if ! is_listening 8848; then
    warn "Nacos（8848）未运行，${name} 可能拉不到远程配置与端口，建议先执行 ./run.sh start nacos"
  fi

  info "启动 $(service_desc "${name}")：mvn -pl ${module} spring-boot:run"
  # exec 让子 shell 直接变成 mvn 进程，$! 即 mvn 的 PID；日志落到 .run/logs
  ( cd "${ROOT_DIR}" && exec nohup "${MVN_BIN}" -pl "${module}" spring-boot:run >"${log_file}" 2>&1 ) &
  echo $! >"${pid_file}"

  if wait_port "${port}" "${START_TIMEOUT}"; then
    ok "$(service_desc "${name}") 启动成功 → http://127.0.0.1:${port}"
  else
    err "$(service_desc "${name}") 在 ${START_TIMEOUT}s 内未监听端口 ${port}，最近日志："
    tail -n 30 "${log_file}" || true
    return 1
  fi
}

# 前端
start_web() {
  local dir="${ROOT_DIR}/web-admin" port=5173 log_file pid_file
  log_file="${LOG_DIR}/web.log"
  pid_file="$(pid_file_of web)"

  if is_listening "${port}"; then
    warn "$(service_desc web) 已在运行（端口 ${port}），跳过启动"
    return 0
  fi
  if [ ! -d "${dir}" ]; then
    err "未找到前端工程目录 ${dir}"
    return 1
  fi
  require_cmd "${NPM_BIN}" 'npm' || return 1
  if [ ! -d "${dir}/node_modules" ]; then
    info 'web-admin 缺少依赖，先执行 npm install（首次较慢）'
    ( cd "${dir}" && "${NPM_BIN}" install )
  fi

  info '启动前端管理系统：npm run dev'
  ( cd "${dir}" && exec nohup "${NPM_BIN}" run dev >"${log_file}" 2>&1 ) &
  echo $! >"${pid_file}"

  if wait_port "${port}" 120; then
    ok "$(service_desc web) 启动成功 → http://localhost:${port} （演示账号 admin/123456）"
  else
    err "前端在 120s 内未监听端口 ${port}，最近日志："
    tail -n 30 "${log_file}" || true
    return 1
  fi
}

# 启停命令
start_one() {
  case "$1" in
    nacos) start_nacos ;;
    mq) start_mq ;;
    gateway | business | message) start_backend "$1" ;;
    web) start_web ;;
    *) err "未知服务：$1"; return 1 ;;
  esac
}

stop_one() {
  local name="$1" port pid pid_file stopped=1
  port="$(service_port "${name}")"

  # 容器类服务（Nacos / RabbitMQ）：交给 docker compose 停，容器与数据都保留
  case "${name}" in
    nacos | mq)
      local compose_service
      if [ "${name}" = 'nacos' ]; then compose_service='nacos'; else compose_service='rabbitmq'; fi
      if is_listening "${port}"; then
        info "停止 $(service_desc "${name}") 容器"
        compose stop "${compose_service}"
        ok "$(service_desc "${name}") 已停止（容器保留，可用 ./run.sh start ${name} 快速拉起）"
      else
        warn "$(service_desc "${name}") 未在运行"
      fi
      return 0
      ;;
  esac

  pid_file="$(pid_file_of "${name}")"
  pid="$(read_pid "${name}")"
  if [ -n "${pid}" ] && kill -0 "${pid}" 2>/dev/null; then
    info "停止 $(service_desc "${name}")（PID ${pid}）"
    terminate_pid "${pid}"
    stopped=0
  fi
  if stop_by_port "${port}"; then
    stopped=0
  fi
  rm -f "${pid_file}"

  if [ "${stopped}" -eq 0 ]; then
    ok "$(service_desc "${name}") 已停止"
  else
    warn "$(service_desc "${name}") 未在运行（端口 ${port} 无监听）"
  fi
}

print_summary() {
  echo ''
  ok '访问地址：'
  printf '  %s前端管理系统%s  http://localhost:5173  （演示账号 admin/123456）\n' "${C_CYAN}" "${C_END}"
  printf '  %s网关入口%s      http://127.0.0.1:8080\n' "${C_CYAN}" "${C_END}"
  printf '  %sNacos 控制台%s  http://%s/nacos  （nacos/nacos）\n' "${C_CYAN}" "${C_END}" "${NACOS_ADDR}"
  printf '  %sRabbitMQ 管理台%s http://127.0.0.1:15672  （bizmsg/bizmsg）\n' "${C_CYAN}" "${C_END}"
  printf '  %s查看日志%s      ./run.sh logs <nacos|mq|gateway|business|message|web>\n' "${C_CYAN}" "${C_END}"
}

# 子命令
cmd_start() {
  local targets failed=0
  targets="$(resolve_targets "$@")" || exit 2
  local item
  for item in ${targets}; do
    start_one "${item}" || failed=1
  done
  if [ "${failed}" -ne 0 ]; then
    err '部分服务启动失败，请查看上方日志或 .run/logs/ 下的日志文件'
    return 1
  fi
  print_summary
}

cmd_stop() {
  local targets
  targets="$(resolve_targets "$@")" || exit 2
  # 按启动的逆序停止：web -> message -> business -> gateway -> mq -> nacos
  targets="$(reverse_words "${targets}")"
  local failed=0 item
  for item in ${targets}; do
    stop_one "${item}" || failed=1
  done
  if [ "${failed}" -ne 0 ]; then
    return 1
  fi
}

cmd_restart() {
  cmd_stop "$@"
  cmd_start "$@"
}

cmd_status() {
  local name port pid state
  echo ''
  echo 'bizmsg 服务状态'
  echo '----------------------------------------------------------------'
  for name in nacos mq gateway business message web; do
    port="$(service_port "${name}")"
    pid="$(port_pid "${port}")"
    if [ -n "${pid}" ]; then
      state="${C_GREEN}运行中${C_END}"
      printf '  [%b] %-28s 端口 %-5s PID %s\n' "${state}" "$(service_desc "${name}")" "${port}" "${pid}"
    else
      state="${C_YELLOW}未运行${C_END}"
      printf '  [%b] %-28s 端口 %-5s\n' "${state}" "$(service_desc "${name}")" "${port}"
    fi
  done
  echo '----------------------------------------------------------------'
  print_summary
}

cmd_logs() {
  local name="${1:-}"
  if [ -z "${name}" ]; then
    err '用法：./run.sh logs <nacos|mq|gateway|business|message|web>'
    return 2
  fi
  case "${name}" in
    gateway | gateway-service) name=gateway ;;
    business | business-service) name=business ;;
    message | message-service) name=message ;;
    web | web-admin | admin) name=web ;;
    mq | rabbitmq | rabbit) name=mq ;;
    nacos) ;;
    *)
      err "未知服务：${name}"
      return 2
      ;;
  esac

  # 容器类服务的日志直接从 docker 取
  if [ "${name}" = 'nacos' ]; then
    compose logs -f --tail=100 nacos
    return 0
  fi
  if [ "${name}" = 'mq' ]; then
    compose logs -f --tail=100 rabbitmq
    return 0
  fi

  local log_file="${LOG_DIR}/${name}.log"
  if [ ! -f "${log_file}" ]; then
    err "日志不存在：${log_file}（服务未通过本脚本启动过？）"
    return 1
  fi
  info "跟踪日志 ${log_file}（Ctrl+C 退出）"
  tail -n 100 -f "${log_file}"
}

cmd_build() {
  require_cmd "${MVN_BIN}" 'Maven' || return 1
  info '构建后端：mvn clean package -DskipTests'
  ( cd "${ROOT_DIR}" && "${MVN_BIN}" clean package -DskipTests )

  require_cmd "${NPM_BIN}" 'npm' || return 1
  local dir="${ROOT_DIR}/web-admin"
  if [ ! -d "${dir}/node_modules" ]; then
    info 'web-admin 缺少依赖，先执行 npm install'
    ( cd "${dir}" && "${NPM_BIN}" install )
  fi
  info '构建前端：npm run build'
  ( cd "${dir}" && "${NPM_BIN}" run build )
  ok '构建完成：后端 target/*.jar，前端 web-admin/dist/'
}

cmd_help() {
  cat <<'USAGE'
bizmsg 统一运行脚本

用法：
  ./run.sh <命令> [服务...]

命令：
  start [服务...]     启动服务，默认 all
                      all 顺序：nacos -> mq -> gateway -> business -> message -> web
  stop  [服务...]     停止服务，默认 all（按启动逆序停止）
  restart [服务...]   重启服务
  status              查看所有服务的运行状态与访问地址
  logs <服务>         持续跟踪日志
  build               构建全部后端模块与前端产物
  import-config       把 nacos/config/*.yml 发布到 Nacos 配置中心
  help                显示本帮助

服务名与端口：
  nacos    8848   Nacos 注册/配置中心（Docker 容器 bizmsg-nacos）
  mq       5672   RabbitMQ 消息队列（容器 bizmsg-rabbitmq，管理台 15672）
  gateway  8080   gateway-service 网关，前端 /api 统一入口
  business 8082   business-service 业务服务（商品房转移：受理 → 办结 → 发 MQ）
  message  8081   message-service 报文服务（消费办结事件，生成 XML + 落盘回执）
  web      5173   web-admin 前端管理系统（Vite dev server）
  all             nacos + mq + gateway + business + message + web

常用示例：
  ./run.sh start                  一键启动全部服务
  ./run.sh start nacos mq         只启动 Nacos 与 RabbitMQ
  ./run.sh restart gateway        重启网关
  ./run.sh status                 查看状态
  ./run.sh logs message           查看报文服务日志
  ./run.sh logs mq                跟踪 RabbitMQ 容器日志
  ./run.sh stop                   停止全部服务

说明：
  - 运行期日志与 PID 统一放在 .run/ 下，已加入 .gitignore，可随时删除。
  - 后端通过 mvn -pl <module> spring-boot:run 启动，首次运行需联网下载依赖，耗时会明显变长。
  - 端口/路由等共享配置来自 Nacos，start nacos 会自动执行 nacos/scripts/import-config.sh 发布配置。
  - Nacos 与 RabbitMQ 共用 nacos/docker-compose.yml，但作为两个独立服务启停，互不影响。
  - 可用环境变量覆盖：NACOS_ADDR、NACOS_NAMESPACE、START_TIMEOUT、MVN_BIN、NPM_BIN、DOCKER_BIN。
USAGE
}

main() {
  local cmd="${1:-help}"
  if [ "$#" -gt 0 ]; then
    shift
  fi
  case "${cmd}" in
    start) ensure_dirs; cmd_start "$@" ;;
    stop) ensure_dirs; cmd_stop "$@" ;;
    restart) ensure_dirs; cmd_restart "$@" ;;
    status | ps) cmd_status ;;
    logs | log) cmd_logs "$@" ;;
    build) cmd_build ;;
    import-config | config) ensure_dirs; import_configs ;;
    help | -h | --help) cmd_help ;;
    *)
      err "未知命令：${cmd}"
      echo ''
      cmd_help
      exit 2
      ;;
  esac
}

main "$@"
