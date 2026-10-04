#!/usr/bin/env bash
# 把 config/ 目录下的配置发布到 Nacos 配置中心，重复执行即覆盖更新（幂等）
# 用法：NACOS_ADDR=127.0.0.1:8848 NACOS_NAMESPACE=bizmsg ./scripts/import-config.sh
set -euo pipefail

NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-bizmsg}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
NACOS_CONTEXT="${NACOS_CONTEXT:-/nacos}"
BASE_URL="http://${NACOS_ADDR}${NACOS_CONTEXT}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/../config"

# Nacos 默认只有 public 命名空间，这里按约定补建 bizmsg 命名空间
ensure_namespace() {
  if curl -fsS "${BASE_URL}/v1/console/namespaces" | grep -q "\"namespace\":\"${NACOS_NAMESPACE}\""; then
    echo "命名空间已存在：${NACOS_NAMESPACE}"
    return 0
  fi
  echo "创建命名空间：${NACOS_NAMESPACE}"
  curl -fsS -X POST "${BASE_URL}/v1/console/namespaces" \
    --data-urlencode "customNamespaceId=${NACOS_NAMESPACE}" \
    --data-urlencode "namespaceName=${NACOS_NAMESPACE}" \
    --data-urlencode "namespaceDesc=bizmsg 练习项目" >/dev/null
}

publish_configs() {
  local file data_id result
  for file in "${CONFIG_DIR}"/*.yml; do
    data_id="$(basename "${file}")"
    result="$(curl -fsS -X POST "${BASE_URL}/v1/cs/configs" \
      --data-urlencode "dataId=${data_id}" \
      --data-urlencode "group=${NACOS_GROUP}" \
      --data-urlencode "tenant=${NACOS_NAMESPACE}" \
      --data-urlencode "type=yaml" \
      --data-urlencode "content@${file}")"
    if [ "${result}" = "true" ]; then
      echo "发布成功：${data_id}"
    else
      echo "发布失败：${data_id}（响应：${result}）" >&2
      return 1
    fi
  done
}

ensure_namespace
publish_configs
echo "全部配置已发布。控制台：http://${NACOS_ADDR}${NACOS_CONTEXT} （默认账号 nacos/nacos）"
