package com.kodeotask.client;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * cliente UDP de prueba para enviar y recibir notificaciones
 */
public class UDPClient {
    
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8082;
    
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private AtomicBoolean running = new AtomicBoolean(true);
    private Long registeredUserId = null;
    
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   CLIENTE UDP DE PRUEBA - KodeoTask    ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║   Host: " + String.format("%-30s", host) + "║");
        System.out.println("║   Puerto: " + String.format("%-28d", port) + "║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        UDPClient client = new UDPClient();
        client.runInteractiveMode(host, port);
    }
    
    /**
     * modo interactivo con menú
     */
    public void runInteractiveMode(String host, int port) {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(host);
            serverPort = port;
            
            Thread receiverThread = new Thread(this::receiveNotifications);
            receiverThread.setDaemon(true);
            receiverThread.start();
            
            Scanner scanner = new Scanner(System.in);
            
            while (running.get()) {
                printMenu();
                System.out.print("Selecciona una opción: ");
                String option = scanner.nextLine().trim();
                
                try {
                    switch (option) {
                        case "1" -> doRegister(scanner);
                        case "2" -> doUnregister();
                        case "3" -> doPing();
                        case "4" -> doSendCustom(scanner);
                        case "5" -> showStatus();
                        case "0", "exit", "quit" -> {
                            running.set(false);
                            if (registeredUserId != null) {
                                doUnregister();
                            }
                            System.out.println("\n¡Hasta luego!");
                            scanner.close();
                            socket.close();
                            return;
                        }
                        default -> System.out.println("Opción no válida");
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
                
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Error al iniciar cliente: " + e.getMessage());
        }
    }
    
    private void printMenu() {
        System.out.println("┌────────────────────────────────────────┐");
        System.out.println("│              MENÚ UDP                  │");
        System.out.println("├────────────────────────────────────────┤");
        System.out.println("│  1. Registrar para notificaciones      │");
        System.out.println("│  2. Desregistrar                       │");
        System.out.println("│  3. Enviar PING                        │");
        System.out.println("│  4. Enviar mensaje personalizado       │");
        System.out.println("│  5. Ver estado                         │");
        System.out.println("│  0. Salir                              │");
        System.out.println("└────────────────────────────────────────┘");
        
        if (registeredUserId != null) {
            System.out.println("✓ Registrado como usuario ID: " + registeredUserId);
        } else {
            System.out.println("✗ No registrado para notificaciones");
        }
        System.out.println();
    }
    
    /**
     * registra el usuario para recibir notificaciones
     */
    private void doRegister(Scanner scanner) throws IOException {
        System.out.println("\n=== REGISTRAR PARA NOTIFICACIONES ===");
        System.out.print("User ID: ");
        String userId = scanner.nextLine().trim();
        
        sendMessage("REGISTER:" + userId);
        registeredUserId = Long.parseLong(userId);
        
        System.out.println("Mensaje enviado. Esperando confirmación...");
    }
    
    /**
     * desregistra el usuario
     */
    private void doUnregister() throws IOException {
        if (registeredUserId == null) {
            System.out.println("✗ No estás registrado");
            return;
        }
        
        System.out.println("\n=== DESREGISTRAR ===");
        sendMessage("UNREGISTER:" + registeredUserId);
        registeredUserId = null;
        
        System.out.println("Desregistrado correctamente");
    }
    
    /**
     * envía un PING al servidor
     */
    private void doPing() throws IOException {
        System.out.println("\n=== PING ===");
        sendMessage("PING");
        System.out.println("PING enviado. Esperando PONG...");
    }
    
    /**
     * envía un mensaje personalizado
     */
    private void doSendCustom(Scanner scanner) throws IOException {
        System.out.println("\n=== MENSAJE PERSONALIZADO ===");
        System.out.print("Mensaje: ");
        String message = scanner.nextLine();
        
        sendMessage(message);
        System.out.println("Mensaje enviado: " + message);
    }
    
    /**
     * Muestra el estado actual
     */
    private void showStatus() {
        System.out.println("\n=== ESTADO ===");
        System.out.println("Servidor: " + serverAddress.getHostAddress() + ":" + serverPort);
        System.out.println("Socket local: " + socket.getLocalPort());
        System.out.println("Registrado: " + (registeredUserId != null ? "Sí (ID: " + registeredUserId + ")" : "No"));
        System.out.println("Ejecutando: " + running.get());
    }
    
    /**
     * envía un mensaje al servidor
     */
    private void sendMessage(String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        socket.send(packet);
    }
    
    /**
     * Thread para recibir notificaciones
     */
    private void receiveNotifications() {
        byte[] buffer = new byte[2048];
        
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                
                System.out.println("\n╔════════════════════════════════════════╗");
                System.out.println("║       NOTIFICACIÓN RECIBIDA            ║");
                System.out.println("╠════════════════════════════════════════╣");
                System.out.println("║ " + formatMessage(message, 38) + " ║");
                System.out.println("╚════════════════════════════════════════╝");
                System.out.print("Selecciona una opción: ");
                
            } catch (SocketException e) {
                if (running.get()) {
                    System.err.println("Socket cerrado: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Error al recibir: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * formatea un mensaje para el ancho especificado
     */
    private String formatMessage(String message, int width) {
        if (message.length() <= width) {
            return String.format("%-" + width + "s", message);
        }
        return message.substring(0, width - 3) + "...";
    }
}

