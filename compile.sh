#!/bin/bash

# =============================================
# Script de compilación - KodeoTask Java Vanilla
# =============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "╔════════════════════════════════════════╗"
echo "║     COMPILANDO KODEOTASK BACKEND       ║"
echo "╚════════════════════════════════════════╝"
echo ""

# Crear directorio de salida
mkdir -p bin

# Verificar que existe el driver de MySQL
MYSQL_DRIVER="lib/mysql-connector-j-8.0.33.jar"
if [ ! -f "$MYSQL_DRIVER" ]; then
    echo "⚠ Driver MySQL no encontrado en $MYSQL_DRIVER"
    echo "  Descargando driver MySQL..."
    mkdir -p lib
    curl -L -o "$MYSQL_DRIVER" \
        "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"
    
    if [ $? -ne 0 ]; then
        echo "✗ Error al descargar el driver MySQL"
        echo "  Descárgalo manualmente desde:"
        echo "  https://dev.mysql.com/downloads/connector/j/"
        exit 1
    fi
    echo "✓ Driver MySQL descargado"
fi

echo ""
echo "Compilando archivos Java..."

# Compilar todos los archivos Java
find src/main/java -name "*.java" > sources.txt

javac -d bin -cp "$MYSQL_DRIVER" @sources.txt 2>&1

if [ $? -eq 0 ]; then
    echo "✓ Compilación exitosa"
    rm sources.txt
else
    echo "✗ Error de compilación"
    rm sources.txt
    exit 1
fi

echo ""
echo "╔════════════════════════════════════════╗"
echo "║          COMPILACIÓN COMPLETA          ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "Para ejecutar usa los siguientes comandos:"
echo ""
echo "  Servidor TCP+UDP:"
echo "    ./run-server.sh"
echo ""
echo "  Cliente TCP:"
echo "    ./run-tcp-client.sh"
echo ""
echo "  Cliente UDP:"
echo "    ./run-udp-client.sh"
echo ""

