#!/bin/bash

# =============================================
# Script para ejecutar el cliente TCP
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
PORT=${2:-8081}
MYSQL_DRIVER="lib/mysql-connector-j-8.0.33.jar"

echo "Conectando al servidor TCP $HOST:$PORT..."
echo ""

# Ejecutar cliente
java -cp "bin:$MYSQL_DRIVER" com.kodeotask.client.TCPClient $HOST $PORT




