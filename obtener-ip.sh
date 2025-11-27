#!/bin/bash

# Script para obtener la IP local del servidor

echo "=== Obteniendo IP del servidor ==="
echo ""

# macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Sistema: macOS"
    echo ""
    echo "IPs disponibles:"
    ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print "  - " $2}'
    echo ""
    echo "IP principal (en0):"
    ipconfig getifaddr en0 2>/dev/null || echo "  No disponible en en0"
    echo ""
    echo "IP principal (en1):"
    ipconfig getifaddr en1 2>/dev/null || echo "  No disponible en en1"
fi

# Linux
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Sistema: Linux"
    echo ""
    echo "IPs disponibles:"
    ip addr show | grep "inet " | grep -v 127.0.0.1 | awk '{print "  - " $2}' | cut -d'/' -f1
    echo ""
    echo "IP principal:"
    hostname -I | awk '{print $1}'
fi

echo ""
echo "=== Configuración para environment.ts ==="
echo ""
echo "Edita src/environments/environment.ts y cambia:"
echo "  apiUrl: 'http://TU_IP_AQUI:8080'"
echo "  serverHost: 'TU_IP_AQUI'"
echo ""

