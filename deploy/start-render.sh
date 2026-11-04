#!/usr/bin/env bash
set -euo pipefail

export HEIMDALL_FRONTEND_URL="${HEIMDALL_FRONTEND_URL:-${RENDER_EXTERNAL_URL:-}}"
java -jar /app/heimdall.jar &
backend=$!
caddy run --config /etc/caddy/Caddyfile --adapter caddyfile &
proxy=$!

cleanup() {
  kill "$backend" "$proxy" 2>/dev/null || true
  wait "$backend" "$proxy" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 143' TERM INT
wait -n "$backend" "$proxy"
