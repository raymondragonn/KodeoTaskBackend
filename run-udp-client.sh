#!/bin/bash

# =============================================
# Script para ejecutar el cliente UDP
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

# Configuración por defecto
MYSQL_DRIVER="lib/mysql-connector-j-8.0.33.jar"
USER_ID=""
HOST="localhost"
PORT="8082"

# Detectar el formato de los argumentos
if [ $# -eq 0 ]; then
    echo "Uso: $0 <USER_ID> [HOST] [PORT]"
    echo "     $0 <USER_ID>"
    echo "     $0 <USER_ID> <HOST>"
    echo "     $0 <USER_ID> <HOST> <PORT>"
    echo ""
    echo "Ejemplos:"
    echo "  $0 1                    # Usuario ID 1, localhost:8082"
    echo "  $0 1 localhost          # Usuario ID 1, localhost:8082"
    echo "  $0 1 localhost 8082     # Usuario ID 1, localhost:8082"
    exit 1
fi

# Verificar si el primer argumento es un número (USER_ID)
if [[ "$1" =~ ^[0-9]+$ ]]; then
    # Formato: USER_ID [HOST] [PORT]
    USER_ID=$1
    HOST=${2:-localhost}
    PORT=${3:-8082}
else
    # Si no es un número, asumir que es el formato antiguo y pedir USER_ID
    echo "⚠ Error: El primer parámetro debe ser el USER_ID (número)"
    echo ""
    echo "Uso correcto: $0 <USER_ID> [HOST] [PORT]"
    echo "Ejemplo: $0 1 localhost 8082"
    exit 1
fi

echo "╔════════════════════════════════════════╗"
echo "║   CLIENTE UDP - HISTORIAL              ║"
echo "╠════════════════════════════════════════╣"
echo "║   Usuario ID: $USER_ID"
echo "║   Servidor: $HOST:$PORT"
echo "╚════════════════════════════════════════╝"
echo ""

# Ejecutar cliente
java -cp "bin:$MYSQL_DRIVER" com.kodeotask.client.UDPClient $USER_ID $HOST $PORT




