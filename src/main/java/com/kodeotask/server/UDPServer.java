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

// servidor udp para notificaciones en tiempo real
public class UDPServer {
    
    public static final int puerto_por_defecto = 8082;
    
    private final int puerto;
    private DatagramSocket socket;
    private boolean ejecutando = false;
    
    private final Map<Long, InfoCliente> clientes_registrados = new ConcurrentHashMap<Long, InfoCliente>();
    
    // informacion del cliente registrado
    private static class InfoCliente {
        InetAddress direccion;
        int puerto;
        
        InfoCliente(InetAddress direccion, int puerto) {
            this.direccion = direccion;
            this.puerto = puerto;
        }
    }
    
    public UDPServer(int puerto) {
        this.puerto = puerto;
    }
    
    // inicia el servidor udp
    public void start() {
        try {
            socket = new DatagramSocket(puerto);
            ejecutando = true;
            
            System.out.println("========================================");
            System.out.println("  SERVIDOR UDP - KodeoTask");
            System.out.println("  Puerto: " + puerto);
            System.out.println("  Estado: ACTIVO");
            System.out.println("========================================");
            System.out.println("Esperando registros de clientes...\n");
            
            byte[] buffer = new byte[1024];
            
            while (ejecutando) {
                try {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paquete);
                    
                    String mensaje = new String(paquete.getData(), 0, paquete.getLength()).trim();
                    InetAddress direccion_cliente = paquete.getAddress();
                    int puerto_cliente = paquete.getPort();
                    
                    System.out.println("[UDP] Mensaje recibido de " + 
                        direccion_cliente.getHostAddress() + ":" + puerto_cliente + " -> " + mensaje);
                    
                    procesar_mensaje(mensaje, direccion_cliente, puerto_cliente);
                    
                } catch (IOException e) {
                    if (ejecutando) {
                        System.err.println("[UDP] Error al recibir: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[UDP] Error al iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // procesa un mensaje recibido
    private void procesar_mensaje(String mensaje, InetAddress direccion, int puerto) {
        if (mensaje.startsWith("REGISTER:")) {
            try {
                Long id_usuario = Long.parseLong(mensaje.substring("REGISTER:".length()).trim());
                clientes_registrados.put(id_usuario, new InfoCliente(direccion, puerto));
                
                System.out.println("[UDP] Cliente UDP registrado");
                System.out.println("  Usuario ID: " + id_usuario);
                System.out.println("  Direccion: " + direccion.getHostAddress() + ":" + puerto);
                System.out.println("  Total clientes: " + clientes_registrados.size());
                
                enviar_mensaje("REGISTERED:OK", direccion, puerto);
                
            } catch (NumberFormatException e) {
                System.err.println("[UDP] Error al parsear userId: " + e.getMessage());
                enviar_mensaje("ERROR:Invalid userId", direccion, puerto);
            }
        } else if (mensaje.startsWith("UNREGISTER:")) {
            try {
                Long id_usuario = Long.parseLong(mensaje.substring("UNREGISTER:".length()).trim());
                clientes_registrados.remove(id_usuario);
                
                System.out.println("[UDP] Cliente UDP desregistrado: userId=" + id_usuario + 
                                 " | Total restantes: " + clientes_registrados.size());
                enviar_mensaje("UNREGISTERED:OK", direccion, puerto);
                
            } catch (NumberFormatException e) {
                System.err.println("[UDP] Error al parsear userId: " + e.getMessage());
            }
        } else if (mensaje.equals("PING")) {
            enviar_mensaje("PONG", direccion, puerto);
        } else {
            System.out.println("[UDP] Mensaje no reconocido: " + mensaje);
        }
    }
    
    // envia un mensaje a una direccion especifica
    private void enviar_mensaje(String mensaje, InetAddress direccion, int puerto) {
        if (socket == null || socket.isClosed()) {
            return;
        }
        
        try {
            byte[] datos = mensaje.getBytes();
            DatagramPacket paquete = new DatagramPacket(datos, datos.length, direccion, puerto);
            socket.send(paquete);
        } catch (IOException e) {
            System.err.println("[UDP] Error al enviar mensaje: " + e.getMessage());
        }
    }
    
    // envia una notificacion a un usuario especifico
    public void sendNotification(Long id_usuario, String tipo, Task tarea) {
        if (socket == null || !ejecutando) {
            System.out.println("[UDP] Servidor UDP no disponible - tipo: " + tipo + ", userId: " + id_usuario);
            return;
        }
        
        InfoCliente cliente = clientes_registrados.get(id_usuario);
        if (cliente == null) {
            System.out.println("[UDP] Usuario " + id_usuario + " no registrado - tipo: " + tipo);
            return;
        }
        
        try {
            Map<String, Object> notificacion = new HashMap<String, Object>();
            notificacion.put("type", tipo);
            notificacion.put("timestamp", System.currentTimeMillis());
            
            if (tarea != null) {
                notificacion.put("taskId", tarea.getId());
                notificacion.put("taskTitle", tarea.getTitle());
                notificacion.put("task", tarea.toJson());
            }
            
            String json_notificacion = JsonUtil.toJson(notificacion);
            byte[] datos = json_notificacion.getBytes();
            
            DatagramPacket paquete = new DatagramPacket(
                datos, datos.length, cliente.direccion, cliente.puerto
            );
            socket.send(paquete);
            
            System.out.println("[UDP] Notificacion enviada - tipo: " + tipo + 
                             ", usuario: " + id_usuario);
            
        } catch (IOException e) {
            System.err.println("[UDP] Error al enviar notificacion: " + e.getMessage());
        }
    }
    
    // envia una notificacion a todos los usuarios registrados
    public void broadcast(String tipo, Task tarea) {
        for (Map.Entry<Long, InfoCliente> entrada : clientes_registrados.entrySet()) {
            sendNotification(entrada.getKey(), tipo, tarea);
        }
    }
    
    // desregistra un cliente
    public void unregisterClient(Long id_usuario) {
        clientes_registrados.remove(id_usuario);
        System.out.println("[UDP] Cliente desregistrado: userId=" + id_usuario);
    }
    
    // detiene el servidor udp
    public void stop() {
        ejecutando = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[UDP] Servidor detenido");
    }
    
    // obtiene el numero de clientes registrados
    public int getRegisteredClientsCount() {
        return clientes_registrados.size();
    }
    
    // metodo principal para ejecutar el servidor udp independiente
    public static void main(String[] args) {
        int puerto = puerto_por_defecto;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                puerto = Integer.parseInt(args[++i]);
            }
        }
        
        UDPServer servidor = new UDPServer(puerto);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("\nCerrando servidor UDP...");
                servidor.stop();
            }
        }));
        
        servidor.start();
    }
}
