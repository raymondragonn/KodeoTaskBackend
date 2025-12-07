-- ============================================
-- Script para agregar tabla task_assignments
-- ============================================

USE kodeotask;

-- Crear tabla de asignaciones de tareas (muchos-a-muchos)
CREATE TABLE IF NOT EXISTS task_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_task_user (task_id, user_id),
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'Tabla task_assignments creada correctamente' AS mensaje;

