#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

JAR_PATH="target/extrator.jar"
if [[ ! -f "$JAR_PATH" ]]; then
  echo "ERRO: JAR nao encontrado em $JAR_PATH" >&2
  exit 1
fi

JAVA_BIN="java"
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
fi

SMOKE_DIR="target/ci/security-smoke"
rm -rf "$SMOKE_DIR"
mkdir -p "$SMOKE_DIR"

echo "[smoke] Validando comando de ajuda no JAR empacotado..."
"$JAVA_BIN" -jar "$JAR_PATH" --ajuda >/dev/null

echo "[smoke] Validando inicializacao do modulo de seguranca (SQLite) no JAR empacotado..."
"$JAVA_BIN" -Dextrator.security.db.path="$SMOKE_DIR/users.db" \
  -jar "$JAR_PATH" \
  --auth-info >/dev/null

if [[ ! -f "$SMOKE_DIR/users.db" ]]; then
  echo "ERRO: Banco SQLite de seguranca nao foi criado pelo smoke test." >&2
  exit 1
fi

echo "[smoke] OK: JAR empacotado validado com sucesso."
