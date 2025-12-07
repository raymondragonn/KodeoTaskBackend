-- ============================================
-- KodeoTask - Script de inicialización MySQL
-- ============================================

-- Crear base de datos si no existe
CREATE DATABASE IF NOT EXISTS kodeotask 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE kodeotask;

-- ============================================
-- Tabla de usuarios
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Tabla de tareas
-- ============================================
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'PENDING',
    category VARCHAR(50),
    created_by BIGINT NOT NULL,
    assigned_to BIGINT,
    due_date DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    INDEX idx_created_by (created_by),
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Tabla de asignaciones de tareas (muchos-a-muchos)
-- ============================================
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

-- ============================================
-- Datos de prueba (opcional)
-- ============================================

-- Usuario de prueba (contraseña: test123)
-- La contraseña está hasheada con SHA-256 + salt
-- INSERT INTO users (username, email, password, first_name, last_name)
-- VALUES ('testuser', 'test@example.com', 'HASH_AQUI', 'Test', 'User');

-- ============================================
-- Procedimientos útiles
-- ============================================

-- Procedimiento para limpiar tareas completadas antiguas
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_old_completed_tasks(IN days_old INT)
BEGIN
    DELETE FROM tasks 
    WHERE status = 'COMPLETED' 
    AND completed_at < DATE_SUB(NOW(), INTERVAL days_old DAY);
    
    SELECT ROW_COUNT() AS deleted_tasks;
END //
DELIMITER ;

-- ============================================
-- Vistas útiles
-- ============================================

-- Vista de tareas con información del creador
CREATE OR REPLACE VIEW v_tasks_with_creator AS
SELECT 
    t.*,
    u.username AS creator_username,
    u.first_name AS creator_first_name,
    u.last_name AS creator_last_name
FROM tasks t
JOIN users u ON t.created_by = u.id;

-- Vista de estadísticas de tareas por usuario
CREATE OR REPLACE VIEW v_user_task_stats AS
SELECT 
    u.id AS user_id,
    u.username,
    COUNT(t.id) AS total_tasks,
    SUM(CASE WHEN t.status = 'PENDING' THEN 1 ELSE 0 END) AS pending_tasks,
    SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_tasks,
    SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_tasks
FROM users u
LEFT JOIN tasks t ON u.id = t.created_by
GROUP BY u.id, u.username;

-- ============================================
-- Mensaje de finalización
-- ============================================
SELECT 'Base de datos KodeoTask inicializada correctamente' AS mensaje;

