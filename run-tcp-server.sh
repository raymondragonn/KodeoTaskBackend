#!/bin/bash

# =============================================
# Script para ejecutar SOLO el servidor TCP
# =============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Verificar compilación
if [ ! -d "bin" ] || [ -z "$(ls -A bin 2>/dev/null)" ]; then
    echo "⚠ Proyecto no compilado. Ejecutando compilación..."
    ./compile.sh
    if [ $? -ne 0 ]; then
        exit 1
    fi
fi

# Configuración
TCP_PORT=${1:-8081}
MYSQL_DRIVER="lib/mysql-connector-j-8.0.33.jar"

echo "╔════════════════════════════════════════╗"
echo "║     SERVIDOR TCP - KodeoTask           ║"
echo "╠════════════════════════════════════════╣"
echo "║   Puerto TCP: $TCP_PORT                        ║"
echo "║   Modo: TCP SOLO (sin UDP)            ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "Presiona Ctrl+C para detener el servidor"
echo ""

# Ejecutar servidor TCP solo
java -cp "bin:$MYSQL_DRIVER" com.kodeotask.server.TCPServer --port $TCP_PORT --tcp-only
