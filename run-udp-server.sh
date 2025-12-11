#!/bin/bash

# =============================================
# Script para ejecutar SOLO el servidor UDP
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
UDP_PORT=${1:-8082}
MYSQL_DRIVER="lib/mysql-connector-j-8.0.33.jar"

echo "╔════════════════════════════════════════╗"
echo "║     SERVIDOR UDP - KodeoTask           ║"
echo "╠════════════════════════════════════════╣"
echo "║   Puerto UDP: $UDP_PORT                        ║"
echo "║   Modo: UDP SOLO (sin TCP)            ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "Este servidor maneja notificaciones en tiempo real."
echo ""
echo "Presiona Ctrl+C para detener el servidor"
echo ""

# Ejecutar servidor UDP solo
java -cp "bin:$MYSQL_DRIVER" com.kodeotask.server.UDPServer --port $UDP_PORT
