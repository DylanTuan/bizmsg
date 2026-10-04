#!/usr/bin/env bash
# 把 Nacos 上的全部配置导回 config/ 目录，便于把控制台改动同步进工程
# 用法：NACOS_ADDR=127.0.0.1:8848 NACOS_NAMESPACE=bizmsg ./scripts/export-config.sh
set -euo pipefail

NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-bizmsg}"
NACOS_CONTEXT="${NACOS_CONTEXT:-/nacos}"
BASE_URL="http://${NACOS_ADDR}${NACOS_CONTEXT}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/../config"
mkdir -p "${CONFIG_DIR}"

curl -fsS "${BASE_URL}/v1/cs/configs?search=blur&dataId=&group=&pageNo=1&pageSize=500&tenant=${NACOS_NAMESPACE}" \
  | python3 -c '
import json
import os
import sys

out_dir = sys.argv[1]
items = json.load(sys.stdin).get("pageItems") or []
if not items:
    print("没有可导出的配置")
for item in items:
    path = os.path.join(out_dir, item["dataId"])
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(item["content"].rstrip("\n") + "\n")
    print("已导出：" + item["dataId"])
' "${CONFIG_DIR}"
