# Configuración para Conexiones en Red Local

Esta guía te ayudará a configurar la aplicación para que otras computadoras en tu red local puedan conectarse y probar las funcionalidades de tiempo real (UDP/TCP).

## Paso 1: Obtener la IP del Servidor

### En macOS/Linux:
```bash
# Opción 1: Usar ifconfig
ifconfig | grep "inet " | grep -v 127.0.0.1

# Opción 2: Usar ip (Linux)
ip addr show | grep "inet " | grep -v 127.0.0.1

# Opción 3: Comando rápido
hostname -I  # Linux
ipconfig getifaddr en0  # macOS (ajusta en0 según tu interfaz)
```

### En Windows:
```cmd
ipconfig
```
Busca la dirección IPv4 de tu adaptador de red (ej: `192.168.1.100`)

## Paso 2: Configurar el Backend

El backend ya está configurado para aceptar conexiones desde cualquier IP en la red local. Solo necesitas:

1. **Iniciar el servidor Spring Boot:**
```bash
cd KodeoTaskBackend
mvn spring-boot:run
```

2. **Verificar que los servidores TCP/UDP estén corriendo:**
   - TCP: puerto 8081
   - UDP: puerto 8082
   - REST API: puerto 8080

Los servidores TCP/UDP escuchan automáticamente en todas las interfaces de red (0.0.0.0).

## Paso 3: Configurar el Frontend (Servidor)

En la computadora que actúa como servidor:

1. **Editar `src/environments/environment.ts`:**
```typescript
export const environment = {
  production: false,
  // Cambiar por la IP de tu servidor
  apiUrl: 'http://TU_IP_AQUI:8080',  // Ejemplo: 'http://192.168.1.100:8080'
  serverHost: 'TU_IP_AQUI',  // Ejemplo: '192.168.1.100'
  tcpPort: 8081,
  udpPort: 8082
};
```

2. **Recompilar el frontend:**
```bash
cd KodeoTask
npm run build
# O si estás en desarrollo:
ng serve --host 0.0.0.0 --port 4200
```

## Paso 4: Configurar el Frontend (Cliente)

En la computadora que se conecta:

1. **Editar `src/environments/environment.ts`:**
```typescript
export const environment = {
  production: false,
  // Usar la IP del servidor
  apiUrl: 'http://IP_DEL_SERVIDOR:8080',  // Ejemplo: 'http://192.168.1.100:8080'
  serverHost: 'IP_DEL_SERVIDOR',  // Ejemplo: '192.168.1.100'
  tcpPort: 8081,
  udpPort: 8082
};
```

2. **Instalar dependencias e iniciar:**
```bash
cd KodeoTask
npm install
ng serve --host 0.0.0.0 --port 4200
```

## Paso 5: Verificar la Conexión

1. **En el servidor:**
   - Abre `http://localhost:4200` o `http://TU_IP:4200`
   - Inicia sesión o crea una cuenta

2. **En el cliente:**
   - Abre `http://IP_DEL_SERVIDOR:4200` en el navegador
   - Inicia sesión con una cuenta diferente

## Paso 6: Probar Funcionalidades

### Probar Asignación de Tareas y Notificaciones UDP:

1. **Usuario A (Servidor):**
   - Crea una nueva tarea
   - Asigna la tarea al Usuario B

2. **Usuario B (Cliente):**
   - Debería recibir una notificación en tiempo real vía UDP/WebSocket
   - La tarea debería aparecer automáticamente en su lista

### Probar Servidor TCP:

El servidor TCP está disponible en el puerto 8081 y puede recibir peticiones HTTP directamente. Puedes probarlo con:

```bash
# Desde otra computadora
curl -X GET http://IP_DEL_SERVIDOR:8081/api/tasks \
  -H "Authorization: Bearer TU_TOKEN"
```

## Solución de Problemas

### Error: "CORS policy blocked"
- Verifica que `SecurityConfig.java` tenga los patrones de origen correctos
- Asegúrate de que la IP del cliente esté en el rango permitido (192.168.x.x, 10.x.x.x, etc.)

### Error: "Connection refused"
- Verifica que el firewall permita conexiones en los puertos 8080, 8081, 8082
- En macOS: System Preferences > Security & Privacy > Firewall
- En Linux: `sudo ufw allow 8080/tcp` (y puertos 8081, 8082)

### Las notificaciones no llegan
- Verifica que el WebSocket esté conectado (revisa la consola del navegador)
- Asegúrate de que ambos usuarios estén autenticados
- Verifica que el servidor UDP esté corriendo (debería aparecer en los logs)

### No puedo acceder desde otra computadora
- Verifica que ambas computadoras estén en la misma red
- Prueba hacer ping: `ping IP_DEL_SERVIDOR`
- Verifica que no haya un proxy o VPN bloqueando la conexión

## Notas de Seguridad

⚠️ **IMPORTANTE:** Esta configuración es solo para desarrollo y pruebas en red local. Para producción:

1. Usa HTTPS en lugar de HTTP
2. Restringe los orígenes CORS a dominios específicos
3. Implementa autenticación más robusta
4. Usa un firewall para proteger los puertos
5. Considera usar variables de entorno para la configuración

## Puertos Utilizados

- **8080**: REST API (Spring Boot)
- **8081**: Servidor TCP puro
- **8082**: Servidor UDP para notificaciones
- **4200**: Frontend Angular (desarrollo)

