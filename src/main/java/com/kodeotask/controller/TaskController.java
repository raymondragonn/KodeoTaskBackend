package com.kodeotask.controller;

import com.kodeotask.dto.TaskDTO;
import com.kodeotask.model.Task;
import com.kodeotask.model.User;
import com.kodeotask.repository.UserRepository;
import com.kodeotask.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private UserRepository userRepository;
    
    // GET /api/tasks - Obtener todas las tareas del usuario
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getTasks(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<Task> tasks = taskService.getUserTasks(userId);
        List<TaskDTO> taskDTOs = tasks.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOs);
    }
    
    // GET /api/tasks/{id} - Obtener una tarea específica
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTask(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Task> task = taskService.getTaskById(id, userId);
        return task.map(t -> ResponseEntity.ok(convertToDTO(t)))
                   .orElse(ResponseEntity.notFound().build());
    }
    
    // POST /api/tasks - Crear una nueva tarea
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskDTO taskDTO, Authentication authentication) {
        System.out.println("=== DEBUG CREATE TASK ===");
        System.out.println("Authentication: " + (authentication != null ? "presente" : "null"));
        
        if (authentication == null) {
            System.out.println("ERROR: Authentication es null - posible problema con JWT filter");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"No autenticado\"}");
        }
        
        System.out.println("Principal: " + authentication.getPrincipal());
        System.out.println("Principal class: " + (authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null"));
        System.out.println("TaskDTO recibido: title=" + taskDTO.getTitle() + ", dueDate=" + taskDTO.getDueDate());
        
        Long userId = getUserIdFromAuthentication(authentication);
        System.out.println("userId extraído: " + userId);
        
        if (userId == null) {
            System.out.println("ERROR: No se pudo extraer userId - Principal no es Long");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"Token inválido o expirado\"}");
        }
        
        try {
            Task task = convertToEntity(taskDTO);
            System.out.println("Task convertido: title=" + task.getTitle() + ", dueDate=" + task.getDueDate());
            
            List<Long> assignedUserIds = taskDTO.getAssignedUsers();
            System.out.println("Usuarios asignados: " + (assignedUserIds != null ? assignedUserIds.toString() : "null"));
            
            Task createdTask = taskService.createTask(task, userId, assignedUserIds);
            System.out.println("Tarea creada exitosamente con ID: " + createdTask.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(createdTask));
        } catch (Exception e) {
            System.err.println("ERROR al crear tarea: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"Error al crear tarea: " + e.getMessage() + "\"}");
        }
    }
    
    // PUT /api/tasks/{id} - Actualizar una tarea
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, 
                                           @RequestBody TaskDTO taskDTO, 
                                           Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Task task = convertToEntity(taskDTO);
        List<Long> assignedUserIds = taskDTO.getAssignedUsers();
        Optional<Task> updatedTask = taskService.updateTask(id, task, userId, assignedUserIds);
        return updatedTask.map(t -> ResponseEntity.ok(convertToDTO(t)))
                         .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
    
    // DELETE /api/tasks/{id} - Eliminar una tarea
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        boolean deleted = taskService.deleteTask(id, userId);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    // Helper method para extraer userId del Authentication
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }
    
    @Autowired
    private com.kodeotask.repository.TaskAssignmentRepository taskAssignmentRepository;
    
    // Convertir Task a TaskDTO
    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setCreatedBy(task.getCreatedBy());
        
        // Obtener username del creador
        if (task.getCreatedBy() != null) {
            Optional<User> creator = userRepository.findById(task.getCreatedBy());
            if (creator.isPresent()) {
                dto.setCreatedByUsername(creator.get().getUsername());
            }
        }
        
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
        
        // Obtener usuarios asignados
        List<com.kodeotask.model.TaskAssignment> assignments = 
            taskAssignmentRepository.findByTaskId(task.getId());
        List<Long> assignedUserIds = assignments.stream()
            .map(com.kodeotask.model.TaskAssignment::getUserId)
            .collect(Collectors.toList());
        dto.setAssignedUsers(assignedUserIds);
        
        return dto;
    }
    
    // Convertir TaskDTO a Task
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

