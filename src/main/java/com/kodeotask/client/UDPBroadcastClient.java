package com.kodeotask.client;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// cliente udp que recibe notificaciones para todos los usuarios
public class UDPBroadcastClient {
    
    private static final String host_por_defecto = "localhost";
    private static final int puerto_udp_por_defecto = 8082;
    private static final int puerto_tcp_por_defecto = 8081;
    
    private DatagramSocket socket_udp;
    private InetAddress direccion_servidor;
    private int puerto_udp;
    
    private boolean ejecutando = true;
    
    private Set<Long> usuarios_registrados = new HashSet<Long>();
    
    private SimpleDateFormat formato_fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        String host = host_por_defecto;
        int puerto_udp = puerto_udp_por_defecto;
        int puerto_tcp = puerto_tcp_por_defecto;
        
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            puerto_udp = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            puerto_tcp = Integer.parseInt(args[2]);
        }
        
        System.out.println("========================================");
        System.out.println("  CLIENTE UDP - TODOS LOS USUARIOS");
        System.out.println("========================================");
        System.out.println("Host: " + host);
        System.out.println("Puerto UDP: " + puerto_udp);
        System.out.println("Puerto TCP: " + puerto_tcp);
        System.out.println("========================================\n");
        
        UDPBroadcastClient cliente = new UDPBroadcastClient();
        cliente.iniciar(host, puerto_udp, puerto_tcp);
    }
    
    // inicia el cliente udp
    public void iniciar(String host, int puerto_udp, int puerto_tcp) {
        try {
            socket_udp = new DatagramSocket();
            direccion_servidor = InetAddress.getByName(host);
            this.puerto_udp = puerto_udp;
            
            System.out.println("Cliente UDP iniciado correctamente");
            System.out.println("Obteniendo usuarios de la base de datos...\n");
            
            List<Long> usuarios = obtener_usuarios(host, puerto_tcp);
            
            if (usuarios.size() == 0) {
                System.out.println("No se encontraron usuarios");
                System.out.println("El cliente seguira ejecutandose\n");
            } else {
                System.out.println("Intentando registrar " + usuarios.size() + " usuarios\n");
                
                int registrados = 0;
                for (int i = 0; i < usuarios.size(); i++) {
                    Long id_usuario = usuarios.get(i);
                    if (registrar_usuario(id_usuario)) {
                        registrados++;
                        if (registrados <= 10) {
                            System.out.println("Usuario ID " + id_usuario + " registrado");
                        }
                    }
                    Thread.sleep(50);
                }
                
                System.out.println("\n" + registrados + " usuarios registrados exitosamente\n");
            }
            
            System.out.println("========================================");
            System.out.println("  HISTORIAL DE NOTIFICACIONES");
            System.out.println("========================================\n");
            
            Thread thread_recibir = new Thread(new Runnable() {
                public void run() {
                    recibir_notificaciones();
                }
            });
            thread_recibir.setDaemon(true);
            thread_recibir.start();
            
            System.out.println("Presiona Ctrl+C para detener\n");
            while (ejecutando) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // obtiene la lista de usuarios de la base de datos
    private List<Long> obtener_usuarios(String host, int puerto_tcp) {
        List<Long> usuarios = new ArrayList<Long>();
        
        try {
            String respuesta = enviar_peticion_tcp(host, puerto_tcp, "GET", "/api/users", null, null);
            
            if (respuesta != null && respuesta.length() > 0 && !respuesta.contains("error")) {
                usuarios = parsear_usuarios(respuesta);
                if (usuarios.size() > 0) {
                    return usuarios;
                }
            }
            
            System.out.println("Intentando registrar usuarios del 1 al 100...\n");
            for (long i = 1; i <= 100; i++) {
                usuarios.add(i);
            }
            
        } catch (Exception e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
            for (long i = 1; i <= 100; i++) {
                usuarios.add(i);
            }
        }
        
        return usuarios;
    }
    
    // parsea los ids de usuario del json
    private List<Long> parsear_usuarios(String json) {
        List<Long> usuarios = new ArrayList<Long>();
        
        if (json == null || json.length() == 0) {
            return usuarios;
        }
        
        int indice = 0;
        while (true) {
            int pos_id = json.indexOf("\"id\":", indice);
            if (pos_id == -1) {
                break;
            }
            
            pos_id = pos_id + 5;
            
            while (pos_id < json.length() && Character.isWhitespace(json.charAt(pos_id))) {
                pos_id++;
            }
            
            int fin = pos_id;
            while (fin < json.length() && Character.isDigit(json.charAt(fin))) {
                fin++;
            }
            
            if (fin > pos_id) {
                try {
                    String numero_str = json.substring(pos_id, fin).trim();
                    Long id_usuario = Long.parseLong(numero_str);
                    if (!usuarios.contains(id_usuario)) {
                        usuarios.add(id_usuario);
                    }
                } catch (NumberFormatException e) {
                }
            }
            
            indice = fin;
        }
        
        return usuarios;
    }
    
    // envia una peticion http al servidor tcp
    private String enviar_peticion_tcp(String host, int puerto, String metodo, String ruta, 
                                     String token, String cuerpo) throws IOException {
        
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        
        try {
            socket = new Socket(host, puerto);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            StringBuilder peticion = new StringBuilder();
            peticion.append(metodo).append(" ").append(ruta).append(" HTTP/1.1\r\n");
            peticion.append("Host: ").append(host).append(":").append(puerto).append("\r\n");
            
            if (token != null) {
                peticion.append("Authorization: Bearer ").append(token).append("\r\n");
            }
            
            if (cuerpo != null && cuerpo.length() > 0) {
                peticion.append("Content-Type: application/json\r\n");
                peticion.append("Content-Length: ").append(cuerpo.getBytes().length).append("\r\n");
            }
            
            peticion.append("\r\n");
            
            if (cuerpo != null && cuerpo.length() > 0) {
                peticion.append(cuerpo);
            }
            
            out.print(peticion.toString());
            out.flush();
            
            StringBuilder respuesta = new StringBuilder();
            String linea;
            int longitud_contenido = 0;
            boolean leyendo_headers = true;
            
            while ((linea = in.readLine()) != null) {
                if (leyendo_headers) {
                    if (linea.length() == 0) {
                        leyendo_headers = false;
                        if (longitud_contenido > 0) {
                            char[] buffer = new char[longitud_contenido];
                            int leidos = in.read(buffer, 0, longitud_contenido);
                            if (leidos > 0) {
                                respuesta.append(new String(buffer, 0, leidos));
                            }
                        }
                        break;
                    } else if (linea.toLowerCase().startsWith("content-length:")) {
                        String length_str = linea.substring(15).trim();
                        longitud_contenido = Integer.parseInt(length_str);
                    }
                }
            }
            
            return respuesta.toString();
            
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    // registra un usuario en el servidor udp
    private boolean registrar_usuario(Long id_usuario) {
        if (usuarios_registrados.contains(id_usuario)) {
            return true;
        }
        
        try {
            String mensaje = "REGISTER:" + id_usuario;
            enviar_mensaje_udp(mensaje);
            usuarios_registrados.add(id_usuario);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // envia un mensaje al servidor udp
    private void enviar_mensaje_udp(String mensaje) throws IOException {
        byte[] datos = mensaje.getBytes();
        DatagramPacket paquete = new DatagramPacket(datos, datos.length, direccion_servidor, puerto_udp);
        socket_udp.send(paquete);
    }
    
    // recibe notificaciones del servidor udp
    private void recibir_notificaciones() {
        byte[] buffer = new byte[2048];
        
        while (ejecutando) {
            try {
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket_udp.receive(paquete);
                
                String mensaje = new String(paquete.getData(), 0, paquete.getLength());
                
                if (mensaje.startsWith("REGISTERED:") || 
                    mensaje.startsWith("UNREGISTERED:") || 
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
        System.out.println("Para: Todos los usuarios");
        
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
