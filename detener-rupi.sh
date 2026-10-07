#!/usr/bin/env bash
# Detiene la web, la API y el MySQL portable de Rupi. Equivalente a Detener-Rupi.ps1.
cd "$(dirname "$(readlink -f "$0")")"
L="$PWD/.local"
for s in web api; do
  if [ -f "$L/$s.pid" ]; then
    pid=$(cat "$L/$s.pid")
    if [ -r "/proc/$pid/cmdline" ] && tr '\0' ' ' <"/proc/$pid/cmdline" | grep -q "$PWD"; then kill "$pid" 2>/dev/null || true; fi
    rm -f "$L/$s.pid"
  fi
done
MYSQL_HOME=$(ls -d "$L"/mysql-8.4*-linux-* | head -n1)
"$MYSQL_HOME/bin/mysqladmin" --defaults-file="$L/root.cnf" shutdown 2>/dev/null || true
echo "Servicios de Rupi detenidos."
