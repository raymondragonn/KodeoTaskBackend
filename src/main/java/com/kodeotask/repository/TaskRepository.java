package com.kodeotask.repository;

import com.kodeotask.model.Task;
import com.kodeotask.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Obtener todas las tareas de un usuario (creadas por él o asignadas a él)
    List<Task> findByCreatedByOrAssignedTo(Long createdBy, Long assignedTo);
    
    // Obtener tareas creadas por un usuario
    List<Task> findByCreatedBy(Long createdBy);
    
    // Obtener tareas asignadas a un usuario
    List<Task> findByAssignedTo(Long assignedTo);
    
    // Obtener tareas por estado
    List<Task> findByStatus(TaskStatus status);
    
    // Obtener tareas por categoría
    List<Task> findByCategory(String category);
    
    // Obtener una tarea específica por ID y usuario (para verificar permisos)
    Optional<Task> findByIdAndCreatedByOrIdAndAssignedTo(Long id, Long createdBy, Long id2, Long assignedTo);
}

