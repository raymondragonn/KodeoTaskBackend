package com.kodeotask.server;

import com.kodeotask.model.Task;
import com.kodeotask.service.AuthService;
import com.kodeotask.service.TaskService;
import com.kodeotask.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * handler para cada cliente TCP conectado, procesa peticiones HTTP
 */
public class TCPClientHandler implements Runnable {
    
    private final Socket clientSocket;
    private final AuthService authService;
    private final TaskService taskService;
    private final UDPServer udpServer;
    
    public TCPClientHandler(Socket socket, AuthService authService, 
                           TaskService taskService, UDPServer udpServer) {
        this.clientSocket = socket;
        this.authService = authService;
        this.taskService = taskService;
        this.udpServer = udpServer;
    }
    
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            HTTPRequest request = parseHTTPRequest(in);
            
            if (request != null) {
                System.out.println("[TCP] " + request.method + " " + request.path);
                
                String response = processRequest(request);
                out.print(response);
                out.flush();
            }
            
        } catch (IOException e) {
            System.err.println("[TCP] Error en handler: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[TCP] Error al cerrar socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * parsea una petición HTTP
     */
    private HTTPRequest parseHTTPRequest(BufferedReader in) throws IOException {
        HTTPRequest request = new HTTPRequest();
        
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }
        
        String[] parts = requestLine.split(" ");
        if (parts.length >= 2) {
            request.method = parts[0];
            request.path = parts[1];
        }
        
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim().toLowerCase();
                String value = line.substring(colonIndex + 1).trim();
                request.headers.put(key, value);
            }
        }
        
        String contentLengthStr = request.headers.get("content-length");
        if (contentLengthStr != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthStr);
                if (contentLength > 0) {
                    char[] body = new char[contentLength];
                    int read = in.read(body, 0, contentLength);
                    if (read > 0) {
                        request.body = new String(body, 0, read);
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        
        return request;
    }
    
    /**
     * procesa la petición y genera una respuesta
     */
    private String processRequest(HTTPRequest request) {
        String method = request.method;
        String path = request.path;
        
        try {
            if ("OPTIONS".equals(method)) {
                return buildResponse(200, "OK", getCorsHeaders(), "");
            }
            
            if (path.startsWith("/api/auth/")) {
                return handleAuthRequest(method, path, request);
            }
            
            if (path.startsWith("/api/users")) {
                return handleUserRequest(method, path, request);
            }
            
            if (path.startsWith("/api/tasks")) {
                return handleTaskRequest(method, path, request);
            }
            
            return buildResponse(404, "Not Found", getJsonHeaders(),
                "{\"error\":\"Ruta no encontrada\"}");
                
        } catch (Exception e) {
            e.printStackTrace();
            return buildResponse(500, "Internal Server Error", getJsonHeaders(),
                "{\"error\":\"" + JsonUtil.escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    /**
     * maneja peticiones de autenticación
     */
    private String handleAuthRequest(String method, String path, HTTPRequest request) {
        if (!"POST".equals(method)) {
            return buildResponse(405, "Method Not Allowed", getJsonHeaders(),
                "{\"error\":\"Método no permitido\"}");
        }
        
        Map<String, String> body = JsonUtil.parseJson(request.body);
        
        if (path.equals("/api/auth/register")) {
            Map<String, Object> result = authService.register(
                body.get("username"),
                body.get("email"),
                body.get("password"),
                body.get("firstName"),
                body.get("lastName")
            );
            
            boolean success = (boolean) result.get("success");
            int statusCode = success ? 201 : 400;
            return buildResponse(statusCode, success ? "Created" : "Bad Request",
                getJsonHeaders(), JsonUtil.toJson(result));
        }
        
        if (path.equals("/api/auth/login")) {
            Map<String, Object> result = authService.login(
                body.get("username"),
                body.get("password")
            );
            
            boolean success = (boolean) result.get("success");
            int statusCode = success ? 200 : 401;
            return buildResponse(statusCode, success ? "OK" : "Unauthorized",
                getJsonHeaders(), JsonUtil.toJson(result));
        }
        
        return buildResponse(404, "Not Found", getJsonHeaders(),
            "{\"error\":\"Endpoint no encontrado\"}");
    }
    
    /**
     * maneja peticiones de usuarios
     */
    private String handleUserRequest(String method, String path, HTTPRequest request) {
        String authHeader = request.headers.get("authorization");
        Long userId = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userId = authService.validateToken(token);
        }
        
        if (userId == null) {
            return buildResponse(401, "Unauthorized", getJsonHeaders(),
                "{\"error\":\"Token inválido o no proporcionado\"}");
        }
        
        if ("GET".equals(method) && path.equals("/api/users")) {
            try {
                List<com.kodeotask.model.User> users = authService.getAllUsers();
                return buildResponse(200, "OK", getJsonHeaders(), JsonUtil.usersToJson(users));
            } catch (Exception e) {
                return buildResponse(500, "Internal Server Error", getJsonHeaders(),
                    "{\"error\":\"" + JsonUtil.escapeJson(e.getMessage()) + "\"}");
            }
        }
        
        return buildResponse(405, "Method Not Allowed", getJsonHeaders(),
            "{\"error\":\"Método no permitido\"}");
    }
    
    /**
     * maneja peticiones de tareas
     */
    private String handleTaskRequest(String method, String path, HTTPRequest request) {
        String authHeader = request.headers.get("authorization");
        Long userId = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userId = authService.validateToken(token);
        }
        
        if (userId == null) {
            return buildResponse(401, "Unauthorized", getJsonHeaders(),
                "{\"error\":\"Token inválido o no proporcionado\"}");
        }
        
        if ("GET".equals(method) && path.equals("/api/tasks")) {
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║ [TASK CRUD] GET /api/tasks                               ║");
            System.out.println("║ Usuario ID: " + String.format("%-47s", userId) + "║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            List<Task> tasks = taskService.getUserTasks(userId);
            System.out.println("[TASK CRUD] ✓ Total tareas encontradas: " + tasks.size());
            if (tasks.size() > 0) {
                System.out.println("[TASK CRUD] Tareas:");
                for (Task task : tasks) {
                    System.out.println("  - ID: " + task.getId() + " | Título: " + task.getTitle() + 
                                     " | Estado: " + task.getStatus() + 
                                     (task.getAssignedUsers() != null && !task.getAssignedUsers().isEmpty() ? 
                                      " | Asignados: " + task.getAssignedUsers() : ""));
                }
            }
            return buildResponse(200, "OK", getJsonHeaders(), JsonUtil.tasksToJson(tasks));
        }
        
        if ("GET".equals(method) && path.matches("/api/tasks/\\d+")) {
            Long taskId = Long.parseLong(path.substring("/api/tasks/".length()));
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║ [TASK CRUD] GET /api/tasks/" + String.format("%-35s", taskId) + "║");
            System.out.println("║ Usuario ID: " + String.format("%-47s", userId) + "║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            Optional<Task> task = taskService.getTaskById(taskId, userId);
            
            if (task.isPresent()) {
                Task t = task.get();
                System.out.println("[TASK CRUD] ✓ Tarea encontrada:");
                System.out.println("  - ID: " + t.getId());
                System.out.println("  - Título: " + t.getTitle());
                System.out.println("  - Estado: " + t.getStatus());
                System.out.println("  - Creador: " + t.getCreatedBy());
                if (t.getAssignedUsers() != null && !t.getAssignedUsers().isEmpty()) {
                    System.out.println("  - Usuarios asignados: " + t.getAssignedUsers());
                }
                return buildResponse(200, "OK", getJsonHeaders(), t.toJson());
            } else {
                System.out.println("[TASK CRUD] ✗ Tarea no encontrada o sin acceso");
                return buildResponse(404, "Not Found", getJsonHeaders(),
                    "{\"error\":\"Tarea no encontrada\"}");
            }
        }
        
        if ("POST".equals(method) && path.equals("/api/tasks")) {
            try {
                System.out.println("╔════════════════════════════════════════════════════════════╗");
                System.out.println("║ [TASK CRUD] POST /api/tasks - CREAR TAREA                 ║");
                System.out.println("║ Usuario ID: " + String.format("%-47s", userId) + "║");
                System.out.println("╚════════════════════════════════════════════════════════════╝");
                Task task = JsonUtil.parseTask(request.body);
                System.out.println("[TASK CRUD] Datos de la tarea:");
                System.out.println("  - Título: " + task.getTitle());
                System.out.println("  - Descripción: " + (task.getDescription() != null ? task.getDescription() : "Sin descripción"));
                System.out.println("  - Categoría: " + (task.getCategory() != null ? task.getCategory() : "Sin categoría"));
                System.out.println("  - Estado: " + (task.getStatus() != null ? task.getStatus() : "PENDING"));
                if (task.getAssignedUsers() != null && !task.getAssignedUsers().isEmpty()) {
                    System.out.println("  - Usuarios asignados: " + task.getAssignedUsers());
                }
                
                Task created = taskService.createTask(task, userId);
                System.out.println("[TASK CRUD] ✓ Tarea creada exitosamente:");
                System.out.println("  - ID: " + created.getId());
                System.out.println("  - Título: " + created.getTitle());
                
                if (udpServer != null && created.getAssignedUsers() != null && !created.getAssignedUsers().isEmpty()) {
                    System.out.println("");
                    System.out.println("╔════════════════════════════════════════════════════════════╗");
                    System.out.println("║ [NOTIFICACIONES] Enviando notificaciones de asignación   ║");
                    System.out.println("║ Tarea ID: " + String.format("%-47s", created.getId()) + "║");
                    System.out.println("╚════════════════════════════════════════════════════════════╝");
                    for (Long assignedUserId : created.getAssignedUsers()) {
                        if (!assignedUserId.equals(userId)) {
                            System.out.println("[NOTIFICACIONES] → Enviando notificación task_assigned a usuario ID: " + assignedUserId);
                            udpServer.sendNotification(assignedUserId, "task_assigned", created);
                        } else {
                            System.out.println("[NOTIFICACIONES] ⊗ Omitiendo notificación al creador (usuario " + userId + ")");
                        }
                    }
                    System.out.println("[NOTIFICACIONES] ✓ Notificaciones de asignación completadas");
                }
                
                if (udpServer != null) {
                    System.out.println("[NOTIFICACIONES] → Enviando notificación task_created al creador (usuario " + userId + ")");
                    udpServer.sendNotification(userId, "task_created", created);
                }
                
                return buildResponse(201, "Created", getJsonHeaders(), created.toJson());
            } catch (Exception e) {
                System.err.println("[TASK CRUD] ✗ ERROR al crear tarea:");
                System.err.println("  - Mensaje: " + e.getMessage());
                e.printStackTrace();
                return buildResponse(400, "Bad Request", getJsonHeaders(),
                    "{\"error\":\"" + JsonUtil.escapeJson(e.getMessage()) + "\"}");
            }
        }
        
        if ("PUT".equals(method) && path.matches("/api/tasks/\\d+")) {
            Long taskId = Long.parseLong(path.substring("/api/tasks/".length()));
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║ [TASK CRUD] PUT /api/tasks/" + String.format("%-35s", taskId) + "║");
            System.out.println("║ Usuario ID: " + String.format("%-47s", userId) + "║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            Task taskUpdate = JsonUtil.parseTask(request.body);
            System.out.println("[TASK CRUD] Datos a actualizar:");
            System.out.println("  - Título: " + (taskUpdate.getTitle() != null ? taskUpdate.getTitle() : "Sin cambios"));
            System.out.println("  - Estado: " + (taskUpdate.getStatus() != null ? taskUpdate.getStatus() : "Sin cambios"));
            if (taskUpdate.getAssignedUsers() != null && !taskUpdate.getAssignedUsers().isEmpty()) {
                System.out.println("  - Usuarios asignados: " + taskUpdate.getAssignedUsers());
            }
            
            Optional<Task> updated = taskService.updateTask(taskId, taskUpdate, userId);
            
            if (updated.isPresent()) {
                Task updatedTask = updated.get();
                System.out.println("[TASK CRUD] ✓ Tarea actualizada exitosamente:");
                System.out.println("  - ID: " + updatedTask.getId());
                System.out.println("  - Título: " + updatedTask.getTitle());
                System.out.println("  - Estado: " + updatedTask.getStatus());
                if (updatedTask.getAssignedUsers() != null && !updatedTask.getAssignedUsers().isEmpty()) {
                    System.out.println("  - Usuarios asignados: " + updatedTask.getAssignedUsers());
                }
                
                if (udpServer != null && updatedTask.getAssignedUsers() != null && !updatedTask.getAssignedUsers().isEmpty()) {
                    System.out.println("");
                    System.out.println("╔════════════════════════════════════════════════════════════╗");
                    System.out.println("║ [NOTIFICACIONES] Enviando notificaciones de asignación   ║");
                    System.out.println("║ Tarea ID: " + String.format("%-47s", taskId) + "║");
                    System.out.println("╚════════════════════════════════════════════════════════════╝");
                    for (Long assignedUserId : updatedTask.getAssignedUsers()) {
                        if (!assignedUserId.equals(userId)) {
                            System.out.println("[NOTIFICACIONES] → Enviando notificación task_assigned a usuario ID: " + assignedUserId);
                            udpServer.sendNotification(assignedUserId, "task_assigned", updatedTask);
                        } else {
                            System.out.println("[NOTIFICACIONES] ⊗ Omitiendo notificación al creador (usuario " + userId + ")");
                        }
                    }
                    System.out.println("[NOTIFICACIONES] ✓ Notificaciones de asignación completadas");
                }
                
                if (udpServer != null) {
                    System.out.println("[NOTIFICACIONES] → Enviando notificación task_updated al creador (usuario " + userId + ")");
                    udpServer.sendNotification(userId, "task_updated", updatedTask);
                }
                
                return buildResponse(200, "OK", getJsonHeaders(), updatedTask.toJson());
            } else {
                System.out.println("[TASK CRUD] ✗ No autorizado o tarea no encontrada");
                return buildResponse(403, "Forbidden", getJsonHeaders(),
                    "{\"error\":\"No autorizado o tarea no encontrada\"}");
            }
        }
        
        if ("DELETE".equals(method) && path.matches("/api/tasks/\\d+")) {
            Long taskId = Long.parseLong(path.substring("/api/tasks/".length()));
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║ [TASK CRUD] DELETE /api/tasks/" + String.format("%-33s", taskId) + "║");
            System.out.println("║ Usuario ID: " + String.format("%-47s", userId) + "║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            boolean deleted = taskService.deleteTask(taskId, userId);
            
            if (deleted) {
                System.out.println("[TASK CRUD] ✓ Tarea eliminada exitosamente");
                if (udpServer != null) {
                    System.out.println("[NOTIFICACIONES] → Enviando notificación task_deleted al usuario " + userId);
                    udpServer.sendNotification(userId, "task_deleted", null);
                }
                
                return buildResponse(204, "No Content", getCorsHeaders(), "");
            } else {
                System.out.println("[TASK CRUD] ✗ No autorizado o tarea no encontrada");
                return buildResponse(403, "Forbidden", getJsonHeaders(),
                    "{\"error\":\"No autorizado o tarea no encontrada\"}");
            }
        }
        
        return buildResponse(405, "Method Not Allowed", getJsonHeaders(),
            "{\"error\":\"Método no permitido\"}");
    }
    
    /**
     * construye una respuesta HTTP
     */
    private String buildResponse(int statusCode, String statusMessage, 
                                Map<String, String> headers, String body) {
        StringBuilder response = new StringBuilder();
        
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
        
        for (Map.Entry<String, String> header : headers.entrySet()) {
            response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        
        if (body != null && !body.isEmpty()) {
            response.append("Content-Length: ").append(body.getBytes().length).append("\r\n");
        } else {
            response.append("Content-Length: 0\r\n");
        }
        
        response.append("\r\n");
        
        if (body != null) {
            response.append(body);
        }
        
        return response.toString();
    }
    
    /**
     * headers para respuestas JSON
     */
    private Map<String, String> getJsonHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
    
    /**
     * headers para CORS
     */
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
    
    /**
     * clase interna para representar una petición HTTP
     */
    private static class HTTPRequest {
        String method;
        String path;
        Map<String, String> headers = new HashMap<>();
        String body;
    }
}

