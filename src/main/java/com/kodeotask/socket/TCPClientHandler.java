package com.kodeotask.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodeotask.dto.*;
import com.kodeotask.service.AuthService;
import com.kodeotask.service.TaskService;
import com.kodeotask.model.Task;
import com.kodeotask.util.JwtUtil;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Handler para cada cliente TCP conectado (ejecutado en un thread separado)
 */
public class TCPClientHandler extends Thread {
    private Socket clientSocket;
    private AuthService authService;
    private TaskService taskService;
    private JwtUtil jwtUtil;
    private ObjectMapper objectMapper;
    private UDPServer udpServer;
    
    public TCPClientHandler(Socket socket, AuthService authService, 
                           TaskService taskService, JwtUtil jwtUtil, UDPServer udpServer) {
        this.clientSocket = socket;
        this.authService = authService;
        this.taskService = taskService;
        this.jwtUtil = jwtUtil;
        this.udpServer = udpServer;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(
                clientSocket.getOutputStream(), true)) {
            
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            
            // Leer la petición HTTP completa
            while ((line = in.readLine()) != null) {
                requestBuilder.append(line).append("\n");
                
                // Si la línea está vacía y hay Content-Length, leer el body
                if (line.isEmpty()) {
                    String requestStr = requestBuilder.toString();
                    HTTPParser.HTTPRequest request = HTTPParser.parse(requestStr);
                    
                    // Procesar la petición
                    String response = processRequest(request);
                    out.print(response);
                    out.flush();
                    
                    // Reiniciar para la siguiente petición (si es keep-alive)
                    requestBuilder = new StringBuilder();
                }
            }
        } catch (IOException e) {
            System.err.println("Error en cliente TCP: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar socket: " + e.getMessage());
            }
        }
    }
    
    private String processRequest(HTTPParser.HTTPRequest request) {
        String method = request.getMethod();
        String path = request.getPath();
        
        try {
            // Rutas de autenticación
            if (path.startsWith("/api/auth/")) {
                return handleAuthRequest(method, path, request);
            }
            
            // Rutas de tareas
            if (path.startsWith("/api/tasks")) {
                return handleTaskRequest(method, path, request);
            }
            
            // Ruta no encontrada
            return HTTPParser.buildResponse(404, "Not Found", 
                Map.of("Content-Type", "application/json"), 
                "{\"error\":\"Ruta no encontrada\"}");
                
        } catch (Exception e) {
            e.printStackTrace();
            return HTTPParser.buildResponse(500, "Internal Server Error",
                Map.of("Content-Type", "application/json"),
                "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    private String handleAuthRequest(String method, String path, HTTPParser.HTTPRequest request) throws Exception {
        if ("POST".equals(method)) {
            if (path.equals("/api/auth/register")) {
                RegisterRequest registerRequest = objectMapper.readValue(
                    request.getBody(), RegisterRequest.class);
                AuthResponse response = authService.register(registerRequest);
                
                int statusCode = response.isSuccess() ? 200 : 400;
                String jsonResponse = objectMapper.writeValueAsString(response);
                
                return HTTPParser.buildResponse(statusCode, 
                    response.isSuccess() ? "OK" : "Bad Request",
                    Map.of("Content-Type", "application/json",
                           "Access-Control-Allow-Origin", "*"),
                    jsonResponse);
            }
            
            if (path.equals("/api/auth/login")) {
                LoginRequest loginRequest = objectMapper.readValue(
                    request.getBody(), LoginRequest.class);
                AuthResponse response = authService.login(loginRequest);
                
                int statusCode = response.isSuccess() ? 200 : 401;
                String jsonResponse = objectMapper.writeValueAsString(response);
                
                return HTTPParser.buildResponse(statusCode,
                    response.isSuccess() ? "OK" : "Unauthorized",
                    Map.of("Content-Type", "application/json",
                           "Access-Control-Allow-Origin", "*"),
                    jsonResponse);
            }
        }
        
        return HTTPParser.buildResponse(405, "Method Not Allowed",
            Map.of("Content-Type", "application/json"),
            "{\"error\":\"Método no permitido\"}");
    }
    
    private String handleTaskRequest(String method, String path, HTTPParser.HTTPRequest request) throws Exception {
        // Extraer token JWT del header Authorization
        String authHeader = request.getHeader("authorization");
        Long userId = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                userId = jwtUtil.extractUserId(token);
                if (!jwtUtil.validateToken(token, jwtUtil.extractUsername(token))) {
                    return HTTPParser.buildResponse(401, "Unauthorized",
                        Map.of("Content-Type", "application/json"),
                        "{\"error\":\"Token inválido\"}");
                }
            } catch (Exception e) {
                return HTTPParser.buildResponse(401, "Unauthorized",
                    Map.of("Content-Type", "application/json"),
                    "{\"error\":\"Token inválido\"}");
            }
        } else {
            return HTTPParser.buildResponse(401, "Unauthorized",
                Map.of("Content-Type", "application/json"),
                "{\"error\":\"Token requerido\"}");
        }
        
        if (userId == null) {
            return HTTPParser.buildResponse(401, "Unauthorized",
                Map.of("Content-Type", "application/json"),
                "{\"error\":\"Usuario no identificado\"}");
        }
        
        // GET /api/tasks
        if ("GET".equals(method) && path.equals("/api/tasks")) {
            List<Task> tasks = taskService.getUserTasks(userId);
            List<TaskDTO> taskDTOs = new ArrayList<>();
            for (Task task : tasks) {
                taskDTOs.add(convertToDTO(task));
            }
            
            String jsonResponse = objectMapper.writeValueAsString(taskDTOs);
            return HTTPParser.buildResponse(200, "OK",
                Map.of("Content-Type", "application/json",
                       "Access-Control-Allow-Origin", "*"),
                jsonResponse);
        }
        
        // GET /api/tasks/{id}
        if ("GET".equals(method) && path.startsWith("/api/tasks/")) {
            String idStr = path.substring("/api/tasks/".length());
            Long taskId = Long.parseLong(idStr);
            
            Optional<Task> task = taskService.getTaskById(taskId, userId);
            if (task.isPresent()) {
                String jsonResponse = objectMapper.writeValueAsString(convertToDTO(task.get()));
                return HTTPParser.buildResponse(200, "OK",
                    Map.of("Content-Type", "application/json",
                           "Access-Control-Allow-Origin", "*"),
                    jsonResponse);
            } else {
                return HTTPParser.buildResponse(404, "Not Found",
                    Map.of("Content-Type", "application/json"),
                    "{\"error\":\"Tarea no encontrada\"}");
            }
        }
        
        // POST /api/tasks
        if ("POST".equals(method) && path.equals("/api/tasks")) {
            TaskDTO taskDTO = objectMapper.readValue(request.getBody(), TaskDTO.class);
            Task task = convertToEntity(taskDTO);
            Task createdTask = taskService.createTask(task, userId);
            
            // Enviar notificación UDP
            udpServer.sendNotification(userId, "task_created", createdTask);
            
            String jsonResponse = objectMapper.writeValueAsString(convertToDTO(createdTask));
            return HTTPParser.buildResponse(201, "Created",
                Map.of("Content-Type", "application/json",
                       "Access-Control-Allow-Origin", "*"),
                jsonResponse);
        }
        
        // PUT /api/tasks/{id}
        if ("PUT".equals(method) && path.startsWith("/api/tasks/")) {
            String idStr = path.substring("/api/tasks/".length());
            Long taskId = Long.parseLong(idStr);
            
            TaskDTO taskDTO = objectMapper.readValue(request.getBody(), TaskDTO.class);
            Task task = convertToEntity(taskDTO);
            Optional<Task> updatedTask = taskService.updateTask(taskId, task, userId);
            
            if (updatedTask.isPresent()) {
                // Enviar notificación UDP
                udpServer.sendNotification(userId, "task_updated", updatedTask.get());
                
                String jsonResponse = objectMapper.writeValueAsString(convertToDTO(updatedTask.get()));
                return HTTPParser.buildResponse(200, "OK",
                    Map.of("Content-Type", "application/json",
                           "Access-Control-Allow-Origin", "*"),
                    jsonResponse);
            } else {
                return HTTPParser.buildResponse(403, "Forbidden",
                    Map.of("Content-Type", "application/json"),
                    "{\"error\":\"No autorizado o tarea no encontrada\"}");
            }
        }
        
        // DELETE /api/tasks/{id}
        if ("DELETE".equals(method) && path.startsWith("/api/tasks/")) {
            String idStr = path.substring("/api/tasks/".length());
            Long taskId = Long.parseLong(idStr);
            
            boolean deleted = taskService.deleteTask(taskId, userId);
            if (deleted) {
                // Enviar notificación UDP
                udpServer.sendNotification(userId, "task_deleted", null);
                
                return HTTPParser.buildResponse(204, "No Content",
                    Map.of("Access-Control-Allow-Origin", "*"),
                    "");
            } else {
                return HTTPParser.buildResponse(403, "Forbidden",
                    Map.of("Content-Type", "application/json"),
                    "{\"error\":\"No autorizado o tarea no encontrada\"}");
            }
        }
        
        // OPTIONS para CORS
        if ("OPTIONS".equals(method)) {
            return HTTPParser.buildResponse(200, "OK",
                Map.of("Access-Control-Allow-Origin", "*",
                       "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
                       "Access-Control-Allow-Headers", "Content-Type, Authorization"),
                "");
        }
        
        return HTTPParser.buildResponse(405, "Method Not Allowed",
            Map.of("Content-Type", "application/json"),
            "{\"error\":\"Método no permitido\"}");
    }
    
    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setAssignedTo(task.getAssignedTo());
        dto.setCategory(task.getCategory());
        // Convertir LocalDateTime a LocalDate
        if (task.getDueDate() != null) {
            dto.setDueDate(task.getDueDate().toLocalDate());
        } else {
            dto.setDueDate(null);
        }
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setCompletedAt(task.getCompletedAt());
        return dto;
    }
    
    private Task convertToEntity(TaskDTO dto) {
        Task task = new Task();
        if (dto.getId() != null) {
            task.setId(dto.getId());
        }
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setCreatedBy(dto.getCreatedBy());
        task.setAssignedTo(dto.getAssignedTo());
        task.setCategory(dto.getCategory());
        // Convertir LocalDate a LocalDateTime (inicio del día)
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate().atStartOfDay());
        } else {
            task.setDueDate(null);
        }
        return task;
    }
}

