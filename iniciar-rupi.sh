#!/usr/bin/env bash
# Arranca Rupi en Linux: MySQL 8.4 portable (3307), API Spring Boot (8081) y web Vite (5173).
# Equivalente a Iniciar-Rupi.ps1. Uso: ./iniciar-rupi.sh [--recompilar]
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")"
ROOT="$PWD"; L="$ROOT/.local"
[ -f "$L/env.sh" ] || { echo "Falta .local/env.sh: prepara MySQL y credenciales (ver LEEME-LOCAL.md)." >&2; exit 1; }
# shellcheck disable=SC1091
. "$L/env.sh"
export PATH="$JAVA_HOME/bin:$PATH"
MYSQL_HOME=$(ls -d "$L"/mysql-8.4*-linux-* | head -n1)
mkdir -p "$L/sockets"

puerto_abierto() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }
esperar() {
  for _ in $(seq 60); do puerto_abierto "$1" && return 0; sleep 1; done
  echo "No inició el puerto $1. Revisa los logs en .local." >&2; exit 1
}

if ! puerto_abierto 3307; then
  nohup "$MYSQL_HOME/bin/mysqld" --defaults-file="$L/my.cnf" >/dev/null 2>&1 &
fi
esperar 3307

JAR="$ROOT/backend/api-spring-boot/target/rupi-api-0.1.0-SNAPSHOT.jar"
if [ "${1:-}" = "--recompilar" ] || [ ! -f "$JAR" ]; then
  mvn -f "$ROOT/backend/api-spring-boot/pom.xml" -B package
fi

if ! puerto_abierto 8081; then
  nohup java "-Djdk.net.unixdomain.tmpdir=$L/sockets" -Djava.net.preferIPv4Stack=true -jar "$JAR" \
    >"$L/api.log" 2>"$L/api-error.log" &
  echo $! >"$L/api.pid"
fi
esperar 8081

if ! puerto_abierto 5173; then
  (cd "$ROOT/frontend/web-react" && nohup node "$ROOT/node_modules/vite/bin/vite.js" --host 127.0.0.1 --port 5173 --strictPort \
    >"$L/web.log" 2>"$L/web-error.log" & echo $! >"$L/web.pid")
fi
esperar 5173
echo "Rupi disponible en http://localhost:5173"
