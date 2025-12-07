package com.kodeotask.dao;

import com.kodeotask.config.DatabaseConfig;
import com.kodeotask.model.Task;
import com.kodeotask.model.TaskStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * data access object para tareas usando JDBC
 */
public class TaskDAO {
    
    /**
     * crea una nueva tarea
     */
    public Task create(Task task) throws SQLException {
        String sql = """
            INSERT INTO tasks (title, description, status, category, created_by, assigned_to, due_date)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus() != null ? task.getStatus().name() : "PENDING");
            stmt.setString(4, task.getCategory());
            stmt.setLong(5, task.getCreatedBy());
            
            if (task.getAssignedTo() != null) {
                stmt.setLong(6, task.getAssignedTo());
            } else {
                stmt.setNull(6, Types.BIGINT);
            }
            
            if (task.getDueDate() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(task.getDueDate()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setId(generatedKeys.getLong(1));
                    }
                }
            }
            
            if (task.getAssignedUsers() != null && !task.getAssignedUsers().isEmpty()) {
                saveAssignedUsers(task.getId(), task.getAssignedUsers());
            }
            
            return task;
        }
    }
    
    /**
     * guarda los usuarios asignados a una tarea
     */
    private void saveAssignedUsers(Long taskId, List<Long> userIds) throws SQLException {
        String deleteSql = "DELETE FROM task_assignments WHERE task_id = ?";
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setLong(1, taskId);
            deleteStmt.executeUpdate();
        }
        
        if (userIds != null && !userIds.isEmpty()) {
            String insertSql = "INSERT INTO task_assignments (task_id, user_id) VALUES (?, ?)";
            try (Connection conn = DatabaseConfig.createNewConnection();
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (Long userId : userIds) {
                    insertStmt.setLong(1, taskId);
                    insertStmt.setLong(2, userId);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }
        }
    }
    
    /**
     * carga los usuarios asignados a una tarea
     */
    private List<Long> loadAssignedUsers(Long taskId) throws SQLException {
        String sql = "SELECT user_id FROM task_assignments WHERE task_id = ?";
        List<Long> userIds = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("user_id"));
                }
            }
        }
        
        return userIds;
    }
    
    /**
     * carga los nombres de usuarios asignados a una tarea
     */
    private List<String> loadAssignedUsersNames(Long taskId) throws SQLException {
        String sql = """
            SELECT u.username, u.first_name, u.last_name 
            FROM task_assignments ta
            JOIN users u ON ta.user_id = u.id
            WHERE ta.task_id = ?
            ORDER BY u.username
            """;
        List<String> userNames = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String username = rs.getString("username");
                    
                    String displayName;
                    if (firstName != null && lastName != null && !firstName.isEmpty() && !lastName.isEmpty()) {
                        displayName = firstName + " " + lastName + " (" + username + ")";
                    } else if (firstName != null && !firstName.isEmpty()) {
                        displayName = firstName + " (" + username + ")";
                    } else {
                        displayName = username;
                    }
                    userNames.add(displayName);
                }
            }
        }
        
        return userNames;
    }
    
    /**
     * Obtiene el username del creador de una tarea
     */
    private String loadCreatorUsername(Long createdBy) throws SQLException {
        String sql = "SELECT username FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, createdBy);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }
        
        return null;
    }
    
    /**
     * busca una tarea por ID
     */
    public Optional<Task> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Task task = mapResultSetToTask(rs);
                    task.setAssignedUsers(loadAssignedUsers(id));
                    task.setAssignedUsersNames(loadAssignedUsersNames(id));
                    task.setCreatedByUsername(loadCreatorUsername(task.getCreatedBy()));
                    return Optional.of(task);
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * obtiene todas las tareas de un usuario
     */
    public List<Task> findByUserId(Long userId) throws SQLException {
        String sql = """
            SELECT DISTINCT t.* FROM tasks t
            LEFT JOIN task_assignments ta ON t.id = ta.task_id
            WHERE t.created_by = ? OR t.assigned_to = ? OR ta.user_id = ?
            ORDER BY t.created_at DESC
            """;
        List<Task> tasks = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapResultSetToTask(rs);
                    task.setAssignedUsers(loadAssignedUsers(task.getId()));
                    task.setAssignedUsersNames(loadAssignedUsersNames(task.getId()));
                    task.setCreatedByUsername(loadCreatorUsername(task.getCreatedBy()));
                    tasks.add(task);
                }
            }
        }
        
        return tasks;
    }
    
    /**
     * obtiene tareas creadas por un usuario
     */
    public List<Task> findByCreatedBy(Long userId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE created_by = ? ORDER BY created_at DESC";
        List<Task> tasks = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapResultSetToTask(rs);
                    task.setAssignedUsers(loadAssignedUsers(task.getId()));
                    task.setAssignedUsersNames(loadAssignedUsersNames(task.getId()));
                    task.setCreatedByUsername(loadCreatorUsername(task.getCreatedBy()));
                    tasks.add(task);
                }
            }
        }
        
        return tasks;
    }
    
    /**
     * obtiene tareas asignadas a un usuario
     */
    public List<Task> findByAssignedTo(Long userId) throws SQLException {
        String sql = """
            SELECT DISTINCT t.* FROM tasks t
            LEFT JOIN task_assignments ta ON t.id = ta.task_id
            WHERE t.assigned_to = ? OR ta.user_id = ?
            ORDER BY t.created_at DESC
            """;
        List<Task> tasks = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapResultSetToTask(rs);
                    task.setAssignedUsers(loadAssignedUsers(task.getId()));
                    task.setAssignedUsersNames(loadAssignedUsersNames(task.getId()));
                    task.setCreatedByUsername(loadCreatorUsername(task.getCreatedBy()));
                    tasks.add(task);
                }
            }
        }
        
        return tasks;
    }
    
    /**
     * obtiene todas las tareas
     */
    public List<Task> findAll() throws SQLException {
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC";
        List<Task> tasks = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Task task = mapResultSetToTask(rs);
                task.setAssignedUsers(loadAssignedUsers(task.getId()));
                task.setAssignedUsersNames(loadAssignedUsersNames(task.getId()));
                task.setCreatedByUsername(loadCreatorUsername(task.getCreatedBy()));
                tasks.add(task);
            }
        }
        
        return tasks;
    }
    
    /**
     * actualiza una tarea
     */
    public Task update(Task task) throws SQLException {
        String sql = """
            UPDATE tasks 
            SET title = ?, description = ?, status = ?, category = ?, 
                assigned_to = ?, due_date = ?, completed_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getStatus() != null ? task.getStatus().name() : "PENDING");
            stmt.setString(4, task.getCategory());
            
            if (task.getAssignedTo() != null) {
                stmt.setLong(5, task.getAssignedTo());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            
            if (task.getDueDate() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(task.getDueDate()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            
            if (task.getCompletedAt() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(task.getCompletedAt()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            
            stmt.setLong(8, task.getId());
            
            stmt.executeUpdate();
            
            if (task.getAssignedUsers() != null) {
                saveAssignedUsers(task.getId(), task.getAssignedUsers());
            }
            
            return task;
        }
    }
    
    /**
     * elimina una tarea
     */
    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * elimina una tarea verificando que pertenece al usuario
     */
    public boolean deleteByIdAndUserId(Long id, Long userId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ? AND created_by = ?";
        
        try (Connection conn = DatabaseConfig.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * mapea un ResultSet a un objeto Task
     */
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            task.setStatus(TaskStatus.valueOf(statusStr));
        }
        
        task.setCategory(rs.getString("category"));
        task.setCreatedBy(rs.getLong("created_by"));
        
        long assignedTo = rs.getLong("assigned_to");
        if (!rs.wasNull()) {
            task.setAssignedTo(assignedTo);
        }
        
        Timestamp dueDate = rs.getTimestamp("due_date");
        if (dueDate != null) {
            task.setDueDate(dueDate.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            task.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            task.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            task.setCompletedAt(completedAt.toLocalDateTime());
        }
        
        return task;
    }
}

