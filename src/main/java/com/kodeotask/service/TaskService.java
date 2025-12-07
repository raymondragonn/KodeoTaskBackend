package com.kodeotask.service;

import com.kodeotask.dao.TaskDAO;
import com.kodeotask.model.Task;
import com.kodeotask.model.TaskStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * servicio de gestión de tareas
 */
public class TaskService {
    
    private final TaskDAO taskDAO;
    
    public TaskService() {
        this.taskDAO = new TaskDAO();
    }
    
    /**
     * crea una nueva tarea
     */
    public Task createTask(Task task, Long userId) throws SQLException {
        System.out.println("[TASK SERVICE] Creando nueva tarea - Usuario: " + userId + ", Título: " + task.getTitle());
        task.setCreatedBy(userId);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING);
        }
        task.setCreatedAt(LocalDateTime.now());
        
        if (task.getAssignedUsers() != null && !task.getAssignedUsers().isEmpty()) {
            System.out.println("[TASK SERVICE] Tarea será asignada a usuarios: " + task.getAssignedUsers());
        }
        
        Task created = taskDAO.create(task);
        System.out.println("[TASK SERVICE] ✓ Tarea creada exitosamente - ID: " + created.getId());
        return created;
    }
    
    /**
     * obtiene todas las tareas de un usuario
     */
    public List<Task> getUserTasks(Long userId) {
        try {
            return taskDAO.findByUserId(userId);
        } catch (SQLException e) {
            System.err.println("Error al obtener tareas: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * obtiene una tarea por ID
     */
    public Optional<Task> getTaskById(Long taskId, Long userId) {
        try {
            Optional<Task> taskOpt = taskDAO.findById(taskId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                if (task.getCreatedBy().equals(userId) || 
                    (task.getAssignedTo() != null && task.getAssignedTo().equals(userId))) {
                    return taskOpt;
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            System.err.println("Error al obtener tarea: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * actualiza una tarea
     */
    public Optional<Task> updateTask(Long taskId, Task updatedTask, Long userId) {
        try {
            System.out.println("[TASK SERVICE] Actualizando tarea ID: " + taskId + " - Usuario: " + userId);
            Optional<Task> existingTaskOpt = taskDAO.findById(taskId);
            if (existingTaskOpt.isEmpty()) {
                System.out.println("[TASK SERVICE] ✗ Tarea no encontrada - ID: " + taskId);
                return Optional.empty();
            }
            
            Task existingTask = existingTaskOpt.get();
            
            if (!existingTask.getCreatedBy().equals(userId)) {
                System.out.println("[TASK SERVICE] ✗ Usuario no autorizado - Usuario: " + userId + ", Creador: " + existingTask.getCreatedBy());
                return Optional.empty();
            }
            
            boolean assignmentChanged = false;
            if (updatedTask.getAssignedUsers() != null) {
                List<Long> oldAssignments = existingTask.getAssignedUsers() != null ? 
                    new ArrayList<>(existingTask.getAssignedUsers()) : new ArrayList<>();
                List<Long> newAssignments = new ArrayList<>(updatedTask.getAssignedUsers());
                
                if (!oldAssignments.equals(newAssignments)) {
                    assignmentChanged = true;
                    System.out.println("[TASK SERVICE] Cambio en asignaciones detectado:");
                    System.out.println("[TASK SERVICE]   Asignaciones anteriores: " + oldAssignments);
                    System.out.println("[TASK SERVICE]   Asignaciones nuevas: " + newAssignments);
                }
            }
            
            if (updatedTask.getTitle() != null) {
                existingTask.setTitle(updatedTask.getTitle());
                System.out.println("[TASK SERVICE] Título actualizado: " + updatedTask.getTitle());
            }
            if (updatedTask.getDescription() != null) {
                existingTask.setDescription(updatedTask.getDescription());
            }
            if (updatedTask.getCategory() != null) {
                existingTask.setCategory(updatedTask.getCategory());
            }
            if (updatedTask.getAssignedTo() != null) {
                existingTask.setAssignedTo(updatedTask.getAssignedTo());
            }
            if (updatedTask.getAssignedUsers() != null) {
                existingTask.setAssignedUsers(updatedTask.getAssignedUsers());
            }
            if (updatedTask.getDueDate() != null) {
                existingTask.setDueDate(updatedTask.getDueDate());
            }
            
            if (updatedTask.getStatus() != null) {
                TaskStatus oldStatus = existingTask.getStatus();
                TaskStatus newStatus = updatedTask.getStatus();
                existingTask.setStatus(newStatus);
                System.out.println("[TASK SERVICE] Estado cambiado: " + oldStatus + " -> " + newStatus);
                
                if (newStatus == TaskStatus.COMPLETED && oldStatus != TaskStatus.COMPLETED) {
                    existingTask.setCompletedAt(LocalDateTime.now());
                } else if (newStatus != TaskStatus.COMPLETED) {
                    existingTask.setCompletedAt(null);
                }
            }
            
            existingTask.setUpdatedAt(LocalDateTime.now());
            Task updated = taskDAO.update(existingTask);
            System.out.println("[TASK SERVICE] ✓ Tarea actualizada exitosamente - ID: " + taskId);
            
            if (assignmentChanged) {
                System.out.println("[TASK SERVICE] ⚠ Se requiere enviar notificaciones de asignación");
            }
            
            return Optional.of(updated);
            
        } catch (SQLException e) {
            System.err.println("[TASK SERVICE] ✗ Error al actualizar tarea: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    /**
     * elimina una tarea
     */
    public boolean deleteTask(Long taskId, Long userId) {
        try {
            return taskDAO.deleteByIdAndUserId(taskId, userId);
        } catch (SQLException e) {
            System.err.println("Error al eliminar tarea: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * obtiene todas las tareas
     */
    public List<Task> getAllTasks() {
        try {
            return taskDAO.findAll();
        } catch (SQLException e) {
            System.err.println("Error al obtener todas las tareas: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}

