package com.kodeotask.service;

import com.kodeotask.model.Task;
import com.kodeotask.model.TaskAssignment;
import com.kodeotask.model.TaskStatus;
import com.kodeotask.repository.TaskAssignmentRepository;
import com.kodeotask.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    // Obtener todas las tareas de un usuario (creadas o asignadas)
    public List<Task> getUserTasks(Long userId) {
        // Obtener tareas creadas por el usuario
        List<Task> createdTasks = taskRepository.findByCreatedBy(userId);
        
        // Obtener tareas asignadas mediante TaskAssignment
        List<TaskAssignment> assignments = taskAssignmentRepository.findByUserId(userId);
        List<Long> assignedTaskIds = assignments.stream()
                .map(TaskAssignment::getTaskId)
                .distinct()
                .collect(Collectors.toList());
        
        List<Task> assignedTasks = new ArrayList<>();
        if (!assignedTaskIds.isEmpty()) {
            assignedTasks = taskRepository.findAllById(assignedTaskIds);
        }
        
        // Obtener tareas asignadas mediante el campo assignedTo (compatibilidad)
        List<Task> assignedToTasks = taskRepository.findByAssignedTo(userId);
        
        // Combinar todas las tareas y eliminar duplicados
        Set<Long> taskIds = new HashSet<>();
        List<Task> allTasks = new ArrayList<>();
        
        for (Task task : createdTasks) {
            if (taskIds.add(task.getId())) {
                allTasks.add(task);
            }
        }
        
        for (Task task : assignedTasks) {
            if (taskIds.add(task.getId())) {
                allTasks.add(task);
            }
        }
        
        for (Task task : assignedToTasks) {
            if (taskIds.add(task.getId())) {
                allTasks.add(task);
            }
        }
        
        return allTasks;
    }
    
    // Obtener una tarea por ID (solo si pertenece al usuario)
    public Optional<Task> getTaskById(Long taskId, Long userId) {
        Optional<Task> task = taskRepository.findById(taskId);
        if (task.isPresent()) {
            Task t = task.get();
            
            // Es el creador
            if (t.getCreatedBy().equals(userId)) {
                return task;
            }
            
            // Está asignado mediante el campo assignedTo (compatibilidad)
            if (t.getAssignedTo() != null && t.getAssignedTo().equals(userId)) {
                return task;
            }
            
            // Está asignado mediante TaskAssignment (varios usuarios)
            boolean isAssignedViaAssignment = taskAssignmentRepository.findByTaskId(taskId).stream()
                    .anyMatch(assignment -> assignment.getUserId().equals(userId));
            
            if (isAssignedViaAssignment) {
                return task;
            }
        }
        
        return Optional.empty();
    }
    
    // Crear una nueva tarea
    @Transactional
    public Task createTask(Task task, Long userId, List<Long> assignedUserIds) {
        task.setCreatedBy(userId);
        task.setStatus(TaskStatus.PENDING);
        Task savedTask = taskRepository.save(task);
        
        // Crear asignaciones múltiples si se proporcionaron
        if (assignedUserIds != null && !assignedUserIds.isEmpty()) {
            for (Long assignedUserId : assignedUserIds) {
                TaskAssignment assignment = new TaskAssignment();
                assignment.setTaskId(savedTask.getId());
                assignment.setUserId(assignedUserId);
                taskAssignmentRepository.save(assignment);
                
                // Notificar a cada usuario asignado
                if (!assignedUserId.equals(userId)) {
                    notificationService.notifyTaskAssigned(savedTask, assignedUserId);
                }
            }
        }
        
        // Enviar notificación al creador
        notificationService.notifyTaskCreated(savedTask);
        return savedTask;
    }
    
    // Método sobrecargado para compatibilidad
    @Transactional
    public Task createTask(Task task, Long userId) {
        return createTask(task, userId, null);
    }
    
    // Actualizar una tarea
    @Transactional
    public Optional<Task> updateTask(Long taskId, Task updatedTask, Long userId, List<Long> newAssignedUserIds) {
        Optional<Task> existingTask = taskRepository.findById(taskId);
        
        if (existingTask.isPresent()) {
            Task task = existingTask.get();
            
            // Verificar que el usuario tenga permisos (creador o asignado)
            boolean isCreator = task.getCreatedBy().equals(userId);
            boolean isAssigned = taskAssignmentRepository.findByTaskId(taskId).stream()
                    .anyMatch(assignment -> assignment.getUserId().equals(userId));
            
            if (!isCreator && !isAssigned) {
                return Optional.empty();
            }
            
            // Actualizar campos
            if (updatedTask.getTitle() != null) {
                task.setTitle(updatedTask.getTitle());
            }
            if (updatedTask.getDescription() != null) {
                task.setDescription(updatedTask.getDescription());
            }
            if (updatedTask.getStatus() != null) {
                task.setStatus(updatedTask.getStatus());
            }
            if (updatedTask.getCategory() != null) {
                task.setCategory(updatedTask.getCategory());
            }
            if (updatedTask.getDueDate() != null) {
                task.setDueDate(updatedTask.getDueDate());
            }
            
            // Actualizar asignaciones (solo el creador puede cambiar asignaciones)
            if (isCreator && newAssignedUserIds != null) {
                // Eliminar asignaciones existentes
                taskAssignmentRepository.deleteByTaskId(taskId);
                
                // Crear nuevas asignaciones
                for (Long assignedUserId : newAssignedUserIds) {
                    TaskAssignment assignment = new TaskAssignment();
                    assignment.setTaskId(taskId);
                    assignment.setUserId(assignedUserId);
                    taskAssignmentRepository.save(assignment);
                    
                    // Notificar a cada usuario asignado
                    if (!assignedUserId.equals(userId)) {
                        notificationService.notifyTaskAssigned(task, assignedUserId);
                    }
                }
            }
            
            Task savedTask = taskRepository.save(task);
            // Enviar notificación de actualización
            notificationService.notifyTaskUpdated(savedTask);
            return Optional.of(savedTask);
        }
        
        return Optional.empty();
    }
    
    // Método sobrecargado para compatibilidad
    @Transactional
    public Optional<Task> updateTask(Long taskId, Task updatedTask, Long userId) {
        return updateTask(taskId, updatedTask, userId, null);
    }
    
    // Eliminar una tarea (solo el creador puede eliminar)
    @Transactional
    public boolean deleteTask(Long taskId, Long userId) {
        Optional<Task> task = taskRepository.findById(taskId);
        
        if (task.isPresent() && task.get().getCreatedBy().equals(userId)) {
            Long assignedTo = task.get().getAssignedTo();
            taskRepository.deleteById(taskId);
            // Enviar notificación de eliminación
            notificationService.notifyTaskDeleted(taskId, userId);
            if (assignedTo != null && !assignedTo.equals(userId)) {
                notificationService.notifyTaskDeleted(taskId, assignedTo);
            }
            return true;
        }
        
        return false;
    }
    
    // Obtener todas las tareas (para administradores, si es necesario)
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
}

