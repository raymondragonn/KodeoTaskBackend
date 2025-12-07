package com.kodeotask.server;

import com.kodeotask.model.Task;
import com.kodeotask.util.JsonUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * servidor UDP para notificaciones en tiempo real
 */
public class UDPServer {
    
    public static final int DEFAULT_PORT = 8082;
    
    private final int port;
    private DatagramSocket socket;
    private volatile boolean running = false;
    
    private final Map<Long, ClientInfo> registeredClients = new ConcurrentHashMap<>();
    
    /**
     * información del cliente registrado
     */
    private static class ClientInfo {
        InetAddress address;
        int port;
        
        ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }
    
    public UDPServer(int port) {
        this.port = port;
    }
    
    /**
     * inicia el servidor UDP
     */
    public void start() {
        try {
            socket = new DatagramSocket(port);
            running = true;
            
            System.out.println("========================================");
            System.out.println("  SERVIDOR UDP - KodeoTask");
            System.out.println("  Puerto: " + port);
            System.out.println("  Estado: ACTIVO");
            System.out.println("========================================");
            System.out.println("Esperando registros de clientes...\n");
            
            byte[] buffer = new byte[1024];
            
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();
                    
                    System.out.println("[UDP] Mensaje recibido de " + 
                        clientAddress.getHostAddress() + ":" + clientPort + " -> " + message);
                    
                    processMessage(message, clientAddress, clientPort);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[UDP] Error al recibir: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[UDP] Error al iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * procesa un mensaje recibido
     */
    private void processMessage(String message, InetAddress address, int port) {
        if (message.startsWith("REGISTER:")) {
            try {
                Long userId = Long.parseLong(message.substring("REGISTER:".length()).trim());
                registeredClients.put(userId, new ClientInfo(address, port));
                
                System.out.println("╔════════════════════════════════════════════════════════════╗");
                System.out.println("║ [NOTIFICACIONES] Cliente UDP registrado                   ║");
                System.out.println("╠════════════════════════════════════════════════════════════╣");
                System.out.println("║ Usuario ID: " + String.format("%-47s", userId) + "║");
                System.out.println("║ Dirección: " + String.format("%-47s", address.getHostAddress() + ":" + port) + "║");
                System.out.println("║ Total clientes registrados: " + String.format("%-30s", registeredClients.size()) + "║");
                System.out.println("╚════════════════════════════════════════════════════════════╝");
                
                sendMessage("REGISTERED:OK", address, port);
                
            } catch (NumberFormatException e) {
                System.err.println("[UDP] Error al parsear userId: " + e.getMessage());
                sendMessage("ERROR:Invalid userId", address, port);
            }
        }
        else if (message.startsWith("UNREGISTER:")) {
            try {
                Long userId = Long.parseLong(message.substring("UNREGISTER:".length()).trim());
                registeredClients.remove(userId);
                
                System.out.println("[NOTIFICACIONES] Cliente UDP desregistrado: userId=" + userId + 
                                 " | Total clientes restantes: " + registeredClients.size());
                sendMessage("UNREGISTERED:OK", address, port);
                
            } catch (NumberFormatException e) {
                System.err.println("[UDP] Error al parsear userId: " + e.getMessage());
            }
        }
        else if (message.equals("PING")) {
            sendMessage("PONG", address, port);
        }
        else {
            System.out.println("[UDP] Mensaje no reconocido: " + message);
        }
    }
    
    /**
     * envía un mensaje a una dirección específica
     */
    private void sendMessage(String message, InetAddress address, int port) {
        if (socket == null || socket.isClosed()) {
            return;
        }
        
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("[UDP] Error al enviar mensaje: " + e.getMessage());
        }
    }
    
    /**
     * envía una notificación a un usuario específico
     */
    public void sendNotification(Long userId, String type, Task task) {
        if (socket == null || !running) {
            System.out.println("[NOTIFICACIONES] Servidor UDP no disponible - type: " + type + ", userId: " + userId);
            return;
        }
        
        ClientInfo client = registeredClients.get(userId);
        if (client == null) {
            System.out.println("[NOTIFICACIONES] Usuario " + userId + " no registrado para notificaciones - type: " + type);
            return;
        }
        
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("timestamp", System.currentTimeMillis());
            
            if (task != null) {
                notification.put("taskId", task.getId());
                notification.put("taskTitle", task.getTitle());
                notification.put("task", task.toJson());
                
                if ("task_assigned".equals(type)) {
                    System.out.println("");
                    System.out.println("╔════════════════════════════════════════════════════════════╗");
                    System.out.println("║ [NOTIFICACIONES] ⭐ NOTIFICACIÓN DE ASIGNACIÓN ⭐          ║");
                    System.out.println("╠════════════════════════════════════════════════════════════╣");
                    System.out.println("║ Usuario destino: " + String.format("%-42s", userId) + "║");
                    System.out.println("║ Tarea ID: " + String.format("%-50s", task.getId()) + "║");
                    System.out.println("║ Tarea título: " + String.format("%-45s", task.getTitle()) + "║");
                    System.out.println("║ Creador: " + String.format("%-51s", task.getCreatedBy()) + "║");
                    if (task.getAssignedUsers() != null && !task.getAssignedUsers().isEmpty()) {
                        System.out.println("║ Usuarios asignados: " + String.format("%-40s", task.getAssignedUsers().toString()) + "║");
                    }
                    if (task.getCreatedByUsername() != null) {
                        System.out.println("║ Creador (username): " + String.format("%-40s", task.getCreatedByUsername()) + "║");
                    }
                    System.out.println("╚════════════════════════════════════════════════════════════╝");
                    System.out.println("");
                }
            }
            
            String jsonNotification = JsonUtil.toJson(notification);
            byte[] data = jsonNotification.getBytes();
            
            DatagramPacket packet = new DatagramPacket(
                data, data.length, client.address, client.port
            );
            socket.send(packet);
            
            System.out.println("[NOTIFICACIONES] ✓ Notificación enviada exitosamente");
            System.out.println("  - Tipo: " + type);
            System.out.println("  - Usuario destino: " + userId);
            System.out.println("  - Dirección: " + client.address.getHostAddress() + ":" + client.port);
            if (task != null) {
                System.out.println("  - Tarea ID: " + task.getId());
            }
            
        } catch (IOException e) {
            System.err.println("[NOTIFICACIONES] ✗ Error al enviar notificación - type: " + type + 
                             ", userId: " + userId + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * envía una notificación a todos los usuarios registrados
     */
    public void broadcast(String type, Task task) {
        for (Map.Entry<Long, ClientInfo> entry : registeredClients.entrySet()) {
            sendNotification(entry.getKey(), type, task);
        }
    }
    
    /**
     * desregistra un cliente
     */
    public void unregisterClient(Long userId) {
        registeredClients.remove(userId);
        System.out.println("[UDP] Cliente desregistrado: userId=" + userId);
    }
    
    /**
     * detiene el servidor UDP
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[UDP] Servidor detenido");
    }
    
    /**
     * obtiene el número de clientes registrados
     */
    public int getRegisteredClientsCount() {
        return registeredClients.size();
    }
    
    /**
     * método principal para ejecutar el servidor UDP independiente
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            }
        }
        
        UDPServer server = new UDPServer(port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCerrando servidor UDP...");
            server.stop();
        }));
        
        server.start();
    }
}

