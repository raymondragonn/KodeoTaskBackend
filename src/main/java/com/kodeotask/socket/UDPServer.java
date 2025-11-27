package com.kodeotask.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodeotask.model.Task;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servidor UDP para enviar notificaciones en tiempo real a los clientes
 */
@Component
public class UDPServer {
    
    private static final int UDP_PORT = 8082;
    private DatagramSocket socket;
    private ObjectMapper objectMapper;
    private boolean running = false;
    
    // Mapa de usuarios registrados para notificaciones: userId -> (host, port)
    private Map<Long, ClientInfo> registeredClients = new ConcurrentHashMap<>();
    
    private static class ClientInfo {
        String host;
        int port;
        
        ClientInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
    
    public UDPServer() {
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void start() {
        new Thread(this::runServer).start();
    }
    
    private void runServer() {
        try {
            socket = new DatagramSocket(UDP_PORT);
            running = true;
            System.out.println("Servidor UDP iniciado en puerto " + UDP_PORT);
            
            byte[] buffer = new byte[1024];
            
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();
                    
                    // Procesar mensaje de registro
                    if (message.startsWith("REGISTER:")) {
                        String userIdStr = message.substring("REGISTER:".length()).trim();
                        try {
                            Long userId = Long.parseLong(userIdStr);
                            registeredClients.put(userId, new ClientInfo(
                                clientAddress.getHostAddress(), clientPort));
                            System.out.println("Cliente UDP registrado: userId=" + userId + 
                                ", " + clientAddress.getHostAddress() + ":" + clientPort);
                            
                            // Enviar confirmación
                            String response = "REGISTERED";
                            byte[] responseData = response.getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                responseData, responseData.length, clientAddress, clientPort);
                            socket.send(responsePacket);
                        } catch (NumberFormatException e) {
                            System.err.println("Error al parsear userId: " + userIdStr);
                        }
                    }
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error en servidor UDP: " + e.getMessage());
                    }
                }
            }
        } catch (java.net.BindException e) {
            System.err.println("ADVERTENCIA: No se pudo iniciar el servidor UDP en el puerto " + UDP_PORT + 
                " (puerto ya en uso). El servidor REST de Spring Boot seguirá funcionando normalmente.");
            System.err.println("Si necesitas el servidor UDP, detén el proceso que está usando el puerto " + UDP_PORT);
            // No lanzar la excepción, permitir que Spring Boot continúe
        } catch (IOException e) {
            System.err.println("Error al iniciar servidor UDP: " + e.getMessage());
            // No lanzar la excepción, permitir que Spring Boot continúe
        }
    }
    
    /**
     * Envía una notificación a un usuario específico
     */
    public void sendNotification(Long userId, String type, Task task) {
        if (socket == null || !running) {
            // Servidor UDP no está disponible
            return;
        }
        
        ClientInfo clientInfo = registeredClients.get(userId);
        if (clientInfo == null) {
            // Cliente no registrado, no se puede enviar notificación
            return;
        }
        
        try {
            Map<String, Object> notification = new java.util.HashMap<>();
            notification.put("type", type);
            notification.put("task", task);
            notification.put("timestamp", System.currentTimeMillis());
            
            String jsonNotification = objectMapper.writeValueAsString(notification);
            byte[] data = jsonNotification.getBytes();
            
            InetAddress address = InetAddress.getByName(clientInfo.host);
            DatagramPacket packet = new DatagramPacket(
                data, data.length, address, clientInfo.port);
            
            socket.send(packet);
            System.out.println("Notificación UDP enviada a userId=" + userId + 
                ": " + type);
        } catch (IOException e) {
            System.err.println("Error al enviar notificación UDP: " + e.getMessage());
        }
    }
    
    /**
     * Elimina el registro de un cliente
     */
    public void unregisterClient(Long userId) {
        registeredClients.remove(userId);
        System.out.println("Cliente UDP desregistrado: userId=" + userId);
    }
    
    @PreDestroy
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("Servidor UDP detenido");
    }
}

