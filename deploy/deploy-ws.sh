#!/bin/bash
# ============================================================
# WebSocket 站内信生产部署脚本
# 用途：部署后端 jar + 更新 Nginx WebSocket 代理配置
# 使用：在本地运行，需要 SSH 免密登录到生产服务器
# 注意：生产网关端口为 18080（非 8080）
# ============================================================

set -e

PROD_HOST="root@106.53.178.198"
PROD_JAR_DIR="/app/server/mrb/jar"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================"
echo "  WebSocket 站内信生产部署"
echo "========================================"

# ---------- 1. 上传后端 jar ----------
echo ""
echo "[1/3] 上传后端 jar 包 (mrb-user + mrb-gateway)..."
scp "$PROJECT_DIR/backend/mrb-user/target/mrb-user-1.0.0-SNAPSHOT.jar"     "$PROD_HOST:$PROD_JAR_DIR/"
scp "$PROJECT_DIR/backend/mrb-gateway/target/mrb-gateway-1.0.0-SNAPSHOT.jar" "$PROD_HOST:$PROD_JAR_DIR/"
echo "  ✓ jar 上传完成"

# ---------- 2. 重启后端服务 ----------
echo ""
echo "[2/3] 重启后端服务..."
ssh "$PROD_HOST" "cd $PROD_JAR_DIR/.. && bash restart.sh mrb-user && sleep 2 && bash restart.sh mrb-gateway"
echo "  ✓ 后端服务已重启"

# ---------- 3. 更新 Nginx 配置 ----------
echo ""
echo "[3/3] 更新 Nginx 配置（添加 /ws/ WebSocket 代理）..."
scp "$SCRIPT_DIR/nginx-mrb.conf" "$PROD_HOST:/tmp/mrb-nginx.conf"
ssh "$PROD_HOST" bash -s <<'REMOTE'
# 备份现有配置
if [ -f /etc/nginx/sites-available/mrb ]; then
    CONF_PATH=/etc/nginx/sites-available/mrb
    cp "$CONF_PATH" "${CONF_PATH}.bak.$(date +%Y%m%d%H%M%S)"
elif [ -f /etc/nginx/conf.d/mrb.conf ]; then
    CONF_PATH=/etc/nginx/conf.d/mrb.conf
    cp "$CONF_PATH" "${CONF_PATH}.bak.$(date +%Y%m%d%H%M%S)"
else
    echo "  ! 未找到现有 Nginx 配置，将创建 /etc/nginx/conf.d/mrb.conf"
    CONF_PATH=/etc/nginx/conf.d/mrb.conf
fi
cp /tmp/mrb-nginx.conf "$CONF_PATH"
nginx -t && nginx -s reload
REMOTE
echo "  ✓ Nginx 已更新并重载"

# ---------- 验证 ----------
echo ""
echo "========================================"
echo "  部署完成！验证中..."
echo "========================================"
echo ""
echo "WebSocket 端点验证:"
ssh "$PROD_HOST" "curl -s -o /dev/null -w '  HTTP %{http_code} (期望 101 或 400)\\n' -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' 'http://localhost:18080/ws/notification?token=test'"
echo ""
echo "请刷新前端页面验证 WebSocket 连接。"
