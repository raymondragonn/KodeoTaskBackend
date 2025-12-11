#!/bin/bash

# =============================================
# Script para ejecutar el cliente UDP que recibe
# notificaciones para todos los usuarios automáticamente
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
HOST=${1:-localhost}
UDP_PORT=${2:-8082}
TCP_PORT=${3:-8081}
MYSQL_DRIVER="lib/mysql-connector-j-8.0.33.jar"

echo "╔════════════════════════════════════════╗"
echo "║   CLIENTE UDP - TODOS LOS USUARIOS     ║"
echo "╠════════════════════════════════════════╣"
echo "║   Servidor UDP: $HOST:$UDP_PORT"
echo "║   Servidor TCP: $HOST:$TCP_PORT"
echo "║   Modo: Broadcast (todos los usuarios) ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "Este cliente se conectará al servidor TCP para"
echo "obtener todos los usuarios y registrarlos"
echo "automáticamente en el servidor UDP."
echo ""
echo "Presiona Ctrl+C para detener el cliente"
echo ""

# Ejecutar cliente broadcast
java -cp "bin:$MYSQL_DRIVER" com.kodeotask.client.UDPBroadcastClient $HOST $UDP_PORT $TCP_PORT
