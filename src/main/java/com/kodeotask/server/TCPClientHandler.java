package com.kodeotask.server;

import com.kodeotask.model.Task;
import com.kodeotask.service.AuthService;
import com.kodeotask.service.TaskService;
import com.kodeotask.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.*;

// handler para cada cliente tcp conectado, procesa peticiones http
public class TCPClientHandler implements Runnable {
    
    private final Socket socket_cliente;
    private final AuthService servicio_auth;
    private final TaskService servicio_tareas;
    private final UDPServer servidor_udp;
    
    public TCPClientHandler(Socket socket, AuthService servicio_auth, 
                           TaskService servicio_tareas, UDPServer servidor_udp) {
        this.socket_cliente = socket;
        this.servicio_auth = servicio_auth;
        this.servicio_tareas = servicio_tareas;
        this.servidor_udp = servidor_udp;
    }
    
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        
        try {
            in = new BufferedReader(new InputStreamReader(socket_cliente.getInputStream()));
            out = new PrintWriter(socket_cliente.getOutputStream(), true);
            
            PeticionHTTP peticion = parsear_peticion_http(in);
            
            if (peticion != null) {
                System.out.println("[TCP] " + peticion.metodo + " " + peticion.ruta);
                
                String respuesta = procesar_peticion(peticion);
                out.print(respuesta);
                out.flush();
            }
            
        } catch (IOException e) {
            System.err.println("[TCP] Error en handler: " + e.getMessage());
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
            try {
                socket_cliente.close();
            } catch (IOException e) {
                System.err.println("[TCP] Error al cerrar socket: " + e.getMessage());
            }
        }
    }
    
    // parsea una peticion http
    private PeticionHTTP parsear_peticion_http(BufferedReader in) throws IOException {
        PeticionHTTP peticion = new PeticionHTTP();
        
        String linea_peticion = in.readLine();
        if (linea_peticion == null || linea_peticion.length() == 0) {
            return null;
        }
        
        String[] partes = linea_peticion.split(" ");
        if (partes.length >= 2) {
            peticion.metodo = partes[0];
            peticion.ruta = partes[1];
        }
        
        String linea;
        while ((linea = in.readLine()) != null && linea.length() > 0) {
            int indice_dos_puntos = linea.indexOf(':');
            if (indice_dos_puntos > 0) {
                String clave = linea.substring(0, indice_dos_puntos).trim().toLowerCase();
                String valor = linea.substring(indice_dos_puntos + 1).trim();
                peticion.headers.put(clave, valor);
            }
        }
        
        String longitud_contenido_str = peticion.headers.get("content-length");
        if (longitud_contenido_str != null) {
            try {
                int longitud_contenido = Integer.parseInt(longitud_contenido_str);
                if (longitud_contenido > 0) {
                    char[] cuerpo = new char[longitud_contenido];
                    int leidos = in.read(cuerpo, 0, longitud_contenido);
                    if (leidos > 0) {
                        peticion.cuerpo = new String(cuerpo, 0, leidos);
                    }
                }
            } catch (NumberFormatException e) {
            }
        }
        
        return peticion;
    }
    
    // procesa la peticion y genera una respuesta
    private String procesar_peticion(PeticionHTTP peticion) {
        String metodo = peticion.metodo;
        String ruta = peticion.ruta;
        
        try {
            if (metodo.equals("OPTIONS")) {
                return construir_respuesta(200, "OK", obtener_headers_cors(), "");
            }
            
            if (ruta.startsWith("/api/auth/")) {
                return manejar_peticion_auth(metodo, ruta, peticion);
            }
            
            if (ruta.startsWith("/api/users")) {
                return manejar_peticion_usuario(metodo, ruta, peticion);
            }
            
            if (ruta.startsWith("/api/tasks")) {
                return manejar_peticion_tarea(metodo, ruta, peticion);
            }
            
            return construir_respuesta(404, "Not Found", obtener_headers_json(),
                "{\"error\":\"Ruta no encontrada\"}");
                
        } catch (Exception e) {
            e.printStackTrace();
            return construir_respuesta(500, "Internal Server Error", obtener_headers_json(),
                "{\"error\":\"" + JsonUtil.escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    // maneja peticiones de autenticacion
    private String manejar_peticion_auth(String metodo, String ruta, PeticionHTTP peticion) {
        if (!metodo.equals("POST")) {
            return construir_respuesta(405, "Method Not Allowed", obtener_headers_json(),
                "{\"error\":\"Metodo no permitido\"}");
        }
        
        Map<String, String> cuerpo = JsonUtil.parseJson(peticion.cuerpo);
        
        if (ruta.equals("/api/auth/register")) {
            Map<String, Object> resultado = servicio_auth.register(
                cuerpo.get("username"),
                cuerpo.get("email"),
                cuerpo.get("password"),
                cuerpo.get("firstName"),
                cuerpo.get("lastName")
            );
            
            boolean exito = (boolean) resultado.get("success");
            int codigo_estado = exito ? 201 : 400;
            return construir_respuesta(codigo_estado, exito ? "Created" : "Bad Request",
                obtener_headers_json(), JsonUtil.toJson(resultado));
        }
        
        if (ruta.equals("/api/auth/login")) {
            Map<String, Object> resultado = servicio_auth.login(
                cuerpo.get("username"),
                cuerpo.get("password")
            );
            
            boolean exito = (boolean) resultado.get("success");
            int codigo_estado = exito ? 200 : 401;
            return construir_respuesta(codigo_estado, exito ? "OK" : "Unauthorized",
                obtener_headers_json(), JsonUtil.toJson(resultado));
        }
        
        return construir_respuesta(404, "Not Found", obtener_headers_json(),
            "{\"error\":\"Endpoint no encontrado\"}");
    }
    
    // maneja peticiones de usuarios
    private String manejar_peticion_usuario(String metodo, String ruta, PeticionHTTP peticion) {
        String header_auth = peticion.headers.get("authorization");
        Long id_usuario = null;
        
        if (header_auth != null && header_auth.startsWith("Bearer ")) {
            String token = header_auth.substring(7);
            id_usuario = servicio_auth.validateToken(token);
        }
        
        if (id_usuario == null) {
            return construir_respuesta(401, "Unauthorized", obtener_headers_json(),
                "{\"error\":\"Token invalido o no proporcionado\"}");
        }
        
        if (metodo.equals("GET") && ruta.equals("/api/users")) {
            try {
                List<com.kodeotask.model.User> usuarios = servicio_auth.getAllUsers();
                return construir_respuesta(200, "OK", obtener_headers_json(), JsonUtil.usersToJson(usuarios));
            } catch (Exception e) {
                return construir_respuesta(500, "Internal Server Error", obtener_headers_json(),
                    "{\"error\":\"" + JsonUtil.escapeJson(e.getMessage()) + "\"}");
            }
        }
        
        return construir_respuesta(405, "Method Not Allowed", obtener_headers_json(),
            "{\"error\":\"Metodo no permitido\"}");
    }
    
    // maneja peticiones de tareas
    private String manejar_peticion_tarea(String metodo, String ruta, PeticionHTTP peticion) {
        String header_auth = peticion.headers.get("authorization");
        Long id_usuario = null;
        
        if (header_auth != null && header_auth.startsWith("Bearer ")) {
            String token = header_auth.substring(7);
            id_usuario = servicio_auth.validateToken(token);
        }
        
        if (id_usuario == null) {
            return construir_respuesta(401, "Unauthorized", obtener_headers_json(),
                "{\"error\":\"Token invalido o no proporcionado\"}");
        }
        
        if (metodo.equals("GET") && ruta.equals("/api/tasks")) {
            System.out.println("[TASK] GET /api/tasks - Usuario: " + id_usuario);
            List<Task> tareas = servicio_tareas.getUserTasks(id_usuario);
            System.out.println("[TASK] Total tareas: " + tareas.size());
            return construir_respuesta(200, "OK", obtener_headers_json(), JsonUtil.tasksToJson(tareas));
        }
        
        if (metodo.equals("GET") && ruta.matches("/api/tasks/\\d+")) {
            Long id_tarea = Long.parseLong(ruta.substring("/api/tasks/".length()));
            System.out.println("[TASK] GET /api/tasks/" + id_tarea + " - Usuario: " + id_usuario);
            Optional<Task> tarea = servicio_tareas.getTaskById(id_tarea, id_usuario);
            
            if (tarea.isPresent()) {
                Task t = tarea.get();
                System.out.println("[TASK] Tarea encontrada: " + t.getTitle());
                return construir_respuesta(200, "OK", obtener_headers_json(), t.toJson());
            } else {
                System.out.println("[TASK] Tarea no encontrada");
                return construir_respuesta(404, "Not Found", obtener_headers_json(),
                    "{\"error\":\"Tarea no encontrada\"}");
            }
        }
        
        if (metodo.equals("POST") && ruta.equals("/api/tasks")) {
            try {
                System.out.println("[TASK] POST /api/tasks - CREAR - Usuario: " + id_usuario);
                Task tarea = JsonUtil.parseTask(peticion.cuerpo);
                System.out.println("[TASK] Titulo: " + tarea.getTitle());
                
                Task tarea_creada = servicio_tareas.createTask(tarea, id_usuario);
                System.out.println("[TASK] Tarea creada - ID: " + tarea_creada.getId());
                
                if (servidor_udp != null && tarea_creada.getAssignedUsers() != null && 
                    tarea_creada.getAssignedUsers().size() > 0) {
                    System.out.println("[UDP] Enviando notificaciones de asignacion");
                    for (Long id_usuario_asignado : tarea_creada.getAssignedUsers()) {
                        if (!id_usuario_asignado.equals(id_usuario)) {
                            servidor_udp.sendNotification(id_usuario_asignado, "task_assigned", tarea_creada);
                        }
                    }
                    servidor_udp.broadcast("task_created", tarea_creada);
                }
                
                if (servidor_udp != null) {
                    servidor_udp.sendNotification(id_usuario, "task_created", tarea_creada);
                }
                
                return construir_respuesta(201, "Created", obtener_headers_json(), tarea_creada.toJson());
            } catch (Exception e) {
                System.err.println("[TASK] Error al crear tarea: " + e.getMessage());
                e.printStackTrace();
                return construir_respuesta(400, "Bad Request", obtener_headers_json(),
                    "{\"error\":\"" + JsonUtil.escapeJson(e.getMessage()) + "\"}");
            }
        }
        
        if (metodo.equals("PUT") && ruta.matches("/api/tasks/\\d+")) {
            Long id_tarea = Long.parseLong(ruta.substring("/api/tasks/".length()));
            System.out.println("[TASK] PUT /api/tasks/" + id_tarea + " - Usuario: " + id_usuario);
            Task tarea_actualizar = JsonUtil.parseTask(peticion.cuerpo);
            
            Optional<Task> tarea_actualizada = servicio_tareas.updateTask(id_tarea, tarea_actualizar, id_usuario);
            
            if (tarea_actualizada.isPresent()) {
                Task tarea = tarea_actualizada.get();
                System.out.println("[TASK] Tarea actualizada - ID: " + tarea.getId());
                
                if (servidor_udp != null && tarea.getAssignedUsers() != null && 
                    tarea.getAssignedUsers().size() > 0) {
                    System.out.println("[UDP] Enviando notificaciones de asignacion");
                    for (Long id_usuario_asignado : tarea.getAssignedUsers()) {
                        if (!id_usuario_asignado.equals(id_usuario)) {
                            servidor_udp.sendNotification(id_usuario_asignado, "task_assigned", tarea);
                        }
                    }
                    servidor_udp.broadcast("task_updated", tarea);
                }
                
                if (servidor_udp != null) {
                    servidor_udp.sendNotification(id_usuario, "task_updated", tarea);
                }
                
                return construir_respuesta(200, "OK", obtener_headers_json(), tarea.toJson());
            } else {
                System.out.println("[TASK] No autorizado o tarea no encontrada");
                return construir_respuesta(403, "Forbidden", obtener_headers_json(),
                    "{\"error\":\"No autorizado o tarea no encontrada\"}");
            }
        }
        
        if (metodo.equals("DELETE") && ruta.matches("/api/tasks/\\d+")) {
            Long id_tarea = Long.parseLong(ruta.substring("/api/tasks/".length()));
            System.out.println("[TASK] DELETE /api/tasks/" + id_tarea + " - Usuario: " + id_usuario);
            boolean eliminada = servicio_tareas.deleteTask(id_tarea, id_usuario);
            
            if (eliminada) {
                System.out.println("[TASK] Tarea eliminada");
                if (servidor_udp != null) {
                    servidor_udp.sendNotification(id_usuario, "task_deleted", null);
                }
                
                return construir_respuesta(204, "No Content", obtener_headers_cors(), "");
            } else {
                System.out.println("[TASK] No autorizado o tarea no encontrada");
                return construir_respuesta(403, "Forbidden", obtener_headers_json(),
                    "{\"error\":\"No autorizado o tarea no encontrada\"}");
            }
        }
        
        return construir_respuesta(405, "Method Not Allowed", obtener_headers_json(),
            "{\"error\":\"Metodo no permitido\"}");
    }
    
    // construye una respuesta http
    private String construir_respuesta(int codigo_estado, String mensaje_estado, 
                                Map<String, String> headers, String cuerpo) {
        StringBuilder respuesta = new StringBuilder();
        
        respuesta.append("HTTP/1.1 ").append(codigo_estado).append(" ").append(mensaje_estado).append("\r\n");
        
        for (Map.Entry<String, String> header : headers.entrySet()) {
            respuesta.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        
        if (cuerpo != null && cuerpo.length() > 0) {
            respuesta.append("Content-Length: ").append(cuerpo.getBytes().length).append("\r\n");
        } else {
            respuesta.append("Content-Length: 0\r\n");
        }
        
        respuesta.append("\r\n");
        
        if (cuerpo != null) {
            respuesta.append(cuerpo);
        }
        
        return respuesta.toString();
    }
    
    // headers para respuestas json
    private Map<String, String> obtener_headers_json() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
    
    // headers para cors
    private Map<String, String> obtener_headers_cors() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
    
    // clase interna para representar una peticion http
    private static class PeticionHTTP {
        String metodo;
        String ruta;
        Map<String, String> headers = new HashMap<String, String>();
        String cuerpo;
    }
}
