#!/bin/bash

# =============================================
# Script para ejecutar el servidor TCP + UDP
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
UDP_PORT=${2:-8082}
MYSQL_DRIVER="lib/mysql-connector-j-8.0.33.jar"

echo "╔════════════════════════════════════════╗"
echo "║       SERVIDOR KODEOTASK BACKEND       ║"
echo "╠════════════════════════════════════════╣"
echo "║   Puerto TCP: $TCP_PORT                        ║"
echo "║   Puerto UDP: $UDP_PORT                        ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "Asegúrate de que MySQL esté ejecutándose"
echo "y la base de datos 'kodeotask' exista."
echo ""
echo "Presiona Ctrl+C para detener el servidor"
echo ""

# Ejecutar servidor
java -cp "bin:$MYSQL_DRIVER" com.kodeotask.server.TCPServer --port $TCP_PORT --udp-port $UDP_PORT



