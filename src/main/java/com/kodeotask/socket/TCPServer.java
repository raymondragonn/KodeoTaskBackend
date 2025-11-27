package com.kodeotask.socket;

import com.kodeotask.service.AuthService;
import com.kodeotask.service.TaskService;
import com.kodeotask.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor TCP puro para manejar peticiones HTTP/REST
 * Cada cliente se maneja en un thread separado
 */
@Component
public class TCPServer {
    
    private static final int TCP_PORT = 8081; // Puerto diferente al de Spring Boot
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UDPServer udpServer;
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running = false;
    
    @PostConstruct
    public void start() {
        threadPool = Executors.newCachedThreadPool();
        new Thread(this::runServer).start();
    }
    
    private void runServer() {
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            running = true;
            System.out.println("Servidor TCP iniciado en puerto " + TCP_PORT);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Cliente TCP conectado: " + clientSocket.getRemoteSocketAddress());
                    
                    // Crear un handler para cada cliente en un thread separado
                    TCPClientHandler handler = new TCPClientHandler(
                        clientSocket, authService, taskService, jwtUtil, udpServer);
                    threadPool.execute(handler);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error al aceptar cliente: " + e.getMessage());
                    }
                }
            }
        } catch (java.net.BindException e) {
            System.err.println("ADVERTENCIA: No se pudo iniciar el servidor TCP en el puerto " + TCP_PORT + 
                " (puerto ya en uso). El servidor REST de Spring Boot seguirá funcionando normalmente.");
            System.err.println("Si necesitas el servidor TCP, detén el proceso que está usando el puerto " + TCP_PORT);
            // No lanzar la excepción, permitir que Spring Boot continúe
        } catch (IOException e) {
            System.err.println("Error al iniciar servidor TCP: " + e.getMessage());
            // No lanzar la excepción, permitir que Spring Boot continúe
        }
    }
    
    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar servidor TCP: " + e.getMessage());
        }
        
        if (threadPool != null) {
            threadPool.shutdown();
        }
        
        System.out.println("Servidor TCP detenido");
    }
}

