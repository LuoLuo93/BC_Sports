#!/bin/bash
# ==========================================================
# 数仓 SQL 部署脚本 (Git Bash + sqlplus)
# 用法: ./deploy.sh <脚本.sql> [脚本2.sql ...]
# 连接: bi_dw@192.168.5.177:1521/orcl (SID 格式)
# 说明: 库字符集为 AL32UTF8 (已确认 NLS_DATABASE_PARAMETERS),
#       部署前自动设置 NLS_LANG, 防止 UTF-8 脚本中文乱码;
#       脚本内应包含 WHENEVER SQLERROR EXIT SQL.SQLCODE, 出错即停
# ==========================================================
set -euo pipefail

DB_USER="${BIDW_DB_USERNAME:-bi_dw}"
DB_PASS="${BIDW_DB_PASSWORD:-123456}"
DB_CONN="${BIDW_DB_CONN:-192.168.5.177:1521/orcl}"

# 库字符集 AL32UTF8 -> 客户端字符集必须一致
export NLS_LANG=AMERICAN_AMERICA.AL32UTF8

SQLPLUS_DIR="/d/WorkSoft/oracle/dbhomeFree/bin"
export PATH="$SQLPLUS_DIR:$PATH"

if [ $# -eq 0 ]; then
  echo "用法: $0 <脚本.sql> [脚本2.sql ...]" >&2
  exit 1
fi

for f in "$@"; do
  if [ ! -f "$f" ]; then
    echo "错误: 文件不存在 $f" >&2
    exit 1
  fi
  enc=$(file -b "$f")
  case "$enc" in
    *UTF-8*) ;;
    *) echo "警告: $f 不是 UTF-8 ($enc), 中文可能乱码" >&2 ;;
  esac
  echo "==> 部署: $f"
  sqlplus -S "$DB_USER/$DB_PASS@$DB_CONN" @"$f"
  echo "==> 完成: $f"
done
