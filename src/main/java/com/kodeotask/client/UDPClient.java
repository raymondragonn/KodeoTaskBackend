package com.kodeotask.client;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

// cliente udp para recibir y mostrar notificaciones en tiempo real
public class UDPClient {
    
    private static final String host_por_defecto = "localhost";
    private static final int puerto_por_defecto = 8082;
    private static final SimpleDateFormat formato_fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private DatagramSocket socket;
    private InetAddress direccion_servidor;
    private int puerto_servidor;
    private boolean ejecutando = true;
    private Long id_usuario_registrado = null;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: UDPClient <USER_ID> [HOST] [PORT]");
            System.out.println("Ejemplo: UDPClient 1 localhost 8082");
            System.exit(1);
        }
        
        Long id_usuario = Long.parseLong(args[0]);
        String host = host_por_defecto;
        int puerto = puerto_por_defecto;
        
        if (args.length > 1) {
            host = args[1];
        }
        if (args.length > 2) {
            puerto = Integer.parseInt(args[2]);
        }
        
        System.out.println("========================================");
        System.out.println("  CLIENTE UDP - HISTORIAL");
        System.out.println("========================================");
        System.out.println("Host: " + host);
        System.out.println("Puerto: " + puerto);
        System.out.println("Usuario ID: " + id_usuario);
        System.out.println("========================================\n");
        
        UDPClient cliente = new UDPClient();
        cliente.ejecutar(host, puerto, id_usuario);
    }
    
    // modo de solo recepcion de notificaciones
    public void ejecutar(String host, int puerto, Long id_usuario) {
        try {
            socket = new DatagramSocket();
            direccion_servidor = InetAddress.getByName(host);
            puerto_servidor = puerto;
            id_usuario_registrado = id_usuario;
            
            enviar_mensaje("REGISTER:" + id_usuario);
            System.out.println("Registrado para recibir notificaciones");
            System.out.println("Esperando notificaciones...\n");
            System.out.println("========================================");
            System.out.println("  HISTORIAL DE NOTIFICACIONES");
            System.out.println("========================================\n");
            
            final UDPClient cliente = this;
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    if (cliente.id_usuario_registrado != null) {
                        try {
                            cliente.enviar_mensaje("UNREGISTER:" + cliente.id_usuario_registrado);
                        } catch (IOException e) {
                        }
                    }
                    cliente.ejecutando = false;
                    if (cliente.socket != null && !cliente.socket.isClosed()) {
                        cliente.socket.close();
                    }
                }
            }));
            
            recibir_notificaciones();
            
        } catch (Exception e) {
            System.err.println("Error al iniciar cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // envia un mensaje al servidor
    private void enviar_mensaje(String mensaje) throws IOException {
        byte[] datos = mensaje.getBytes();
        DatagramPacket paquete = new DatagramPacket(datos, datos.length, direccion_servidor, puerto_servidor);
        socket.send(paquete);
    }
    
    // recibe notificaciones y las muestra
    private void recibir_notificaciones() {
        byte[] buffer = new byte[2048];
        
        while (ejecutando) {
            try {
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);
                
                String mensaje = new String(paquete.getData(), 0, paquete.getLength());
                
                if (mensaje.startsWith("REGISTERED:") || mensaje.startsWith("UNREGISTERED:") || 
                    mensaje.equals("PONG")) {
                    continue;
                }
                
                mostrar_notificacion(mensaje);
                
            } catch (SocketException e) {
                if (ejecutando) {
                    System.err.println("Socket cerrado: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (ejecutando) {
                    System.err.println("Error al recibir: " + e.getMessage());
                }
            }
        }
    }
    
    // muestra una notificacion formateada
    private void mostrar_notificacion(String json) {
        String fecha_hora = formato_fecha.format(new Date());
        
        String tipo = extraer_campo(json, "type");
        String id_tarea = extraer_campo(json, "taskId");
        String titulo = extraer_campo(json, "taskTitle");
        String creador = extraer_campo(json, "createdByUsername");
        
        if (creador == null) {
            creador = "Sistema";
        }
        
        String tipo_desc = obtener_descripcion_tipo(tipo);
        
        System.out.println("----------------------------------------");
        System.out.println("[" + fecha_hora + "]");
        System.out.println("Tipo: " + tipo_desc);
        System.out.println("De: " + creador);
        System.out.println("Para: Usuario ID: " + id_usuario_registrado);
        
        if (id_tarea != null && id_tarea.length() > 0) {
            System.out.println("Tarea ID: " + id_tarea);
        }
        
        if (titulo != null && titulo.length() > 0) {
            if (titulo.length() > 60) {
                titulo = titulo.substring(0, 57) + "...";
            }
            System.out.println("Titulo: " + titulo);
        }
        
        System.out.println("----------------------------------------\n");
    }
    
    // extrae un campo del json
    private String extraer_campo(String json, String nombre_campo) {
        String patron = "\"" + nombre_campo + "\":";
        int inicio = json.indexOf(patron);
        
        if (inicio == -1) {
            return null;
        }
        
        inicio = inicio + patron.length();
        
        while (inicio < json.length() && Character.isWhitespace(json.charAt(inicio))) {
            inicio++;
        }
        
        if (inicio < json.length() && json.charAt(inicio) == '"') {
            inicio++;
            int fin = json.indexOf('"', inicio);
            if (fin > inicio) {
                return json.substring(inicio, fin);
            }
        } else {
            int fin = inicio;
            while (fin < json.length() && 
                   (Character.isDigit(json.charAt(fin)) || 
                    json.charAt(fin) == '.' || 
                    json.charAt(fin) == '-')) {
                fin++;
            }
            if (fin > inicio) {
                String valor = json.substring(inicio, fin).trim();
                if (valor.equals("null")) {
                    return null;
                }
                return valor;
            }
        }
        
        return null;
    }
    
    // obtiene una descripcion legible del tipo de notificacion
    private String obtener_descripcion_tipo(String tipo) {
        if (tipo == null) {
            return "Notificacion";
        }
        
        if (tipo.equals("task_assigned")) {
            return "Tarea Asignada";
        } else if (tipo.equals("task_created")) {
            return "Tarea Creada";
        } else if (tipo.equals("task_updated")) {
            return "Tarea Actualizada";
        } else if (tipo.equals("task_deleted")) {
            return "Tarea Eliminada";
        } else {
            return tipo;
        }
    }
}
