package com.kodeotask.server;

import com.kodeotask.config.DatabaseConfig;
import com.kodeotask.service.AuthService;
import com.kodeotask.service.TaskService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// servidor tcp para manejar peticiones http/rest
public class TCPServer {
    
    private static final int puerto_por_defecto = 8081;
    
    private final int puerto;
    private final AuthService servicio_auth;
    private final TaskService servicio_tareas;
    private final UDPServer servidor_udp;
    
    private ServerSocket socket_servidor;
    private ExecutorService pool_threads;
    private boolean ejecutando = false;
    
    public TCPServer(int puerto, UDPServer servidor_udp) {
        this.puerto = puerto;
        this.servicio_auth = new AuthService();
        this.servicio_tareas = new TaskService();
        this.servidor_udp = servidor_udp;
    }
    
    // inicia el servidor tcp
    public void start() {
        pool_threads = Executors.newCachedThreadPool();
        
        try {
            DatabaseConfig.initializeTables();
            
            socket_servidor = new ServerSocket(puerto);
            ejecutando = true;
            
            System.out.println("========================================");
            System.out.println("  SERVIDOR TCP - KodeoTask");
            System.out.println("  Puerto: " + puerto);
            System.out.println("  Estado: ACTIVO");
            System.out.println("========================================");
            System.out.println("Esperando conexiones...\n");
            
            while (ejecutando) {
                try {
                    Socket socket_cliente = socket_servidor.accept();
                    System.out.println("[TCP] Cliente conectado: " + socket_cliente.getRemoteSocketAddress());
                    
                    TCPClientHandler handler = new TCPClientHandler(
                        socket_cliente, servicio_auth, servicio_tareas, servidor_udp
                    );
                    pool_threads.execute(handler);
                    
                } catch (IOException e) {
                    if (ejecutando) {
                        System.err.println("[TCP] Error al aceptar cliente: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[TCP] Error al iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // detiene el servidor tcp
    public void stop() {
        ejecutando = false;
        
        try {
            if (socket_servidor != null && !socket_servidor.isClosed()) {
                socket_servidor.close();
            }
        } catch (IOException e) {
            System.err.println("[TCP] Error al cerrar socket: " + e.getMessage());
        }
        
        if (pool_threads != null) {
            pool_threads.shutdown();
        }
        
        DatabaseConfig.closeConnection();
        System.out.println("[TCP] Servidor detenido");
    }
    
    // metodo principal para ejecutar el servidor
    public static void main(String[] args) {
        int puerto = puerto_por_defecto;
        int puerto_udp = UDPServer.puerto_por_defecto;
        boolean solo_tcp = false;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                puerto = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--udp-port") && i + 1 < args.length) {
                puerto_udp = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--tcp-only") || args[i].equals("--no-udp")) {
                solo_tcp = true;
            }
        }
        
        final UDPServer servidor_udp;
        
        if (!solo_tcp) {
            System.out.println("========================================");
            System.out.println("  INICIANDO SERVIDOR TCP + UDP");
            System.out.println("========================================\n");
            
            servidor_udp = new UDPServer(puerto_udp);
            Thread thread_udp = new Thread(new Runnable() {
                public void run() {
                    servidor_udp.start();
                }
            });
            thread_udp.setDaemon(true);
            thread_udp.start();
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        } else {
            System.out.println("========================================");
            System.out.println("  INICIANDO SERVIDOR TCP SOLO");
            System.out.println("========================================\n");
            servidor_udp = null;
        }
        
        TCPServer servidor_tcp = new TCPServer(puerto, servidor_udp);
        
        final UDPServer servidor_udp_final = servidor_udp;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("\nCerrando servidor...");
                servidor_tcp.stop();
                if (servidor_udp_final != null) {
                    servidor_udp_final.stop();
                }
            }
        }));
        
        servidor_tcp.start();
    }
}
