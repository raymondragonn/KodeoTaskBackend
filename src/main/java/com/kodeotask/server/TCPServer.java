package com.kodeotask.server;

import com.kodeotask.config.DatabaseConfig;
import com.kodeotask.service.AuthService;
import com.kodeotask.service.TaskService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * servidor TCP para manejar peticiones HTTP/REST
 */
public class TCPServer {
    
    private static final int DEFAULT_PORT = 8081;
    
    private final int port;
    private final AuthService authService;
    private final TaskService taskService;
    private final UDPServer udpServer;
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    
    public TCPServer(int port, UDPServer udpServer) {
        this.port = port;
        this.authService = new AuthService();
        this.taskService = new TaskService();
        this.udpServer = udpServer;
    }
    
    /**
     * inicia el servidor TCP
     */
    public void start() {
        threadPool = Executors.newCachedThreadPool();
        
        try {
            System.out.println("Inicializando base de datos MySQL...");
            DatabaseConfig.initializeTables();
            
            serverSocket = new ServerSocket(port);
            running = true;
            
            System.out.println("========================================");
            System.out.println("  SERVIDOR TCP - KodeoTask");
            System.out.println("  Puerto: " + port);
            System.out.println("  Estado: ACTIVO");
            System.out.println("========================================");
            System.out.println("Esperando conexiones...\n");
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[TCP] Cliente conectado: " + clientSocket.getRemoteSocketAddress());
                    
                    // Crear handler para el cliente en un thread separado
                    TCPClientHandler handler = new TCPClientHandler(
                        clientSocket, authService, taskService, udpServer
                    );
                    threadPool.execute(handler);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[TCP] Error al aceptar cliente: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[TCP] Error al iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * detiene el servidor TCP
     */
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[TCP] Error al cerrar socket: " + e.getMessage());
        }
        
        if (threadPool != null) {
            threadPool.shutdown();
        }
        
        DatabaseConfig.closeConnection();
        System.out.println("[TCP] Servidor detenido");
    }
    
    /**
     * método principal para ejecutar el servidor
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        int udpPort = UDPServer.DEFAULT_PORT;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--udp-port") && i + 1 < args.length) {
                udpPort = Integer.parseInt(args[++i]);
            }
        }
        
        System.out.println("========================================");
        System.out.println("  INICIANDO SERVIDOR TCP + UDP");
        System.out.println("========================================\n");
        
        UDPServer udpServer = new UDPServer(udpPort);
        Thread udpThread = new Thread(() -> udpServer.start());
        udpThread.setDaemon(true);
        udpThread.start();
        
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        
        TCPServer tcpServer = new TCPServer(port, udpServer);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCerrando servidores...");
            tcpServer.stop();
            udpServer.stop();
        }));
        
        tcpServer.start();
    }
}

