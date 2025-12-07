# KodeoTask Backend - Java Vanilla + MySQL

Backend de KodeoTask implementado con **Java puro** (sin Spring Boot) y conexión directa a **MySQL**.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    KodeoTask Backend                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────────┐         ┌─────────────────┐           │
│   │  Servidor TCP   │         │  Servidor UDP   │           │
│   │   Puerto 8081   │         │   Puerto 8082   │           │
│   │                 │         │                 │           │
│   │  - REST API     │◄───────►│  - Notif. RT    │           │
│   │  - Auth         │         │  - Broadcast    │           │
│   │  - CRUD Tasks   │         │  - Registro     │           │
│   └────────┬────────┘         └─────────────────┘           │
│            │                                                 │
│            ▼                                                 │
│   ┌─────────────────┐                                       │
│   │     MySQL       │                                       │
│   │   (JDBC Puro)   │                                       │
│   │                 │                                       │
│   │  - users        │                                       │
│   │  - tasks        │                                       │
│   └─────────────────┘                                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Requisitos

- **Java 17** o superior
- **MySQL 8.0** o superior

## Instalación Rápida

### 1. Configurar MySQL

```bash
# Crear la base de datos
mysql -u root -p < sql/init.sql
```

### 2. Configurar credenciales

Editar `src/main/java/com/kodeotask/config/DatabaseConfig.java`:

```java
private static final String USER = "root";
private static final String PASSWORD = "tu_contraseña";
```

### 3. Compilar

```bash
./compile.sh
```

### 4. Ejecutar

```bash
# Terminal 1: Servidor TCP + UDP
./run-server.sh

# Terminal 2: Cliente TCP
./run-tcp-client.sh

# Terminal 3: Cliente UDP
./run-udp-client.sh
```

## Estructura del Proyecto

```
KodeoTaskBackend/
├── src/main/java/com/kodeotask/
│   ├── config/
│   │   └── DatabaseConfig.java    # Conexión JDBC a MySQL
│   ├── model/
│   │   ├── User.java              # Modelo de usuario
│   │   ├── Task.java              # Modelo de tarea
│   │   └── TaskStatus.java        # Estados de tarea
│   ├── dao/
│   │   ├── UserDAO.java           # Acceso a datos de usuarios
│   │   └── TaskDAO.java           # Acceso a datos de tareas
│   ├── service/
│   │   ├── AuthService.java       # Lógica de autenticación
│   │   └── TaskService.java       # Lógica de tareas
│   ├── util/
│   │   ├── PasswordUtil.java      # Hash de contraseñas (SHA-256)
│   │   ├── TokenUtil.java         # Generación/validación de tokens
│   │   └── JsonUtil.java          # Parser JSON simple
│   ├── server/
│   │   ├── TCPServer.java         # Servidor TCP (peticiones REST)
│   │   ├── TCPClientHandler.java  # Handler de clientes TCP
│   │   └── UDPServer.java         # Servidor UDP (notificaciones)
│   └── client/
│       ├── TCPClient.java         # Cliente TCP de prueba
│       └── UDPClient.java         # Cliente UDP de prueba
├── sql/
│   └── init.sql                   # Script de inicialización MySQL
├── lib/                           # Driver MySQL (se descarga automático)
├── bin/                           # Clases compiladas
├── compile.sh                     # Script de compilación
├── run-server.sh                  # Ejecutar servidor TCP+UDP
├── run-tcp-client.sh              # Ejecutar cliente TCP
├── run-udp-client.sh              # Ejecutar cliente UDP
└── README.md
```

## API REST (Servidor TCP)

### Autenticación

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/register` | Registrar usuario |
| POST | `/api/auth/login` | Iniciar sesión |

### Tareas (requieren token)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/tasks` | Listar tareas del usuario |
| GET | `/api/tasks/{id}` | Obtener tarea específica |
| POST | `/api/tasks` | Crear tarea |
| PUT | `/api/tasks/{id}` | Actualizar tarea |
| DELETE | `/api/tasks/{id}` | Eliminar tarea |

## Protocolo UDP

| Comando | Descripción |
|---------|-------------|
| `REGISTER:userId` | Registrar para notificaciones |
| `UNREGISTER:userId` | Desregistrar |
| `PING` | Verificar conexión |

### Tipos de notificaciones
- `task_created` - Nueva tarea
- `task_updated` - Tarea actualizada
- `task_deleted` - Tarea eliminada

## Flujo de Prueba

1. **Iniciar servidor:** `./run-server.sh`
2. **Iniciar cliente UDP:** `./run-udp-client.sh` → Registrar con ID de usuario
3. **Iniciar cliente TCP:** `./run-tcp-client.sh` → Login y crear tareas
4. **Ver notificaciones** en el cliente UDP cuando se crean/modifican tareas

## Tecnologías

| Componente | Tecnología |
|------------|-----------|
| Framework | **Ninguno** - Java puro |
| Base de datos | **MySQL** con JDBC directo |
| JSON | Parser manual (sin Jackson) |
| Tokens | Implementación propia |
| Hash passwords | SHA-256 + salt |
| Servidor TCP | `ServerSocket` |
| Servidor UDP | `DatagramSocket` |

