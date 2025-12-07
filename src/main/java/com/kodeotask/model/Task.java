package com.kodeotask.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * modelo de tarea
 */
public class Task {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private String category;
    private Long createdBy;
    private String createdByUsername;
    private Long assignedTo;
    private List<Long> assignedUsers;
    private List<String> assignedUsersNames;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    public Task() {
        this.status = TaskStatus.PENDING;
    }
    
    public Task(String title, String description, Long createdBy) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.status = TaskStatus.PENDING;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    
    public List<Long> getAssignedUsers() { 
        if (assignedUsers == null) {
            assignedUsers = new ArrayList<>();
        }
        return assignedUsers; 
    }
    public void setAssignedUsers(List<Long> assignedUsers) { this.assignedUsers = assignedUsers; }
    
    public List<String> getAssignedUsersNames() {
        if (assignedUsersNames == null) {
            assignedUsersNames = new ArrayList<>();
        }
        return assignedUsersNames;
    }
    public void setAssignedUsersNames(List<String> assignedUsersNames) { this.assignedUsersNames = assignedUsersNames; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    @Override
    public String toString() {
        return "Task{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
    
    /**
     * convierte la tarea a JSON
     */
    public String toJson() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(id != null ? id : 0).append(",");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"status\":\"").append(status != null ? status.name() : "PENDING").append("\",");
        sb.append("\"category\":").append(category != null ? "\"" + escapeJson(category) + "\"" : "null").append(",");
        sb.append("\"createdBy\":").append(createdBy != null ? createdBy : "null").append(",");
        sb.append("\"createdByUsername\":").append(createdByUsername != null ? "\"" + escapeJson(createdByUsername) + "\"" : "null").append(",");
        sb.append("\"assignedTo\":").append(assignedTo != null ? assignedTo : "null").append(",");
        
        // Agregar assignedUsers como array
        sb.append("\"assignedUsers\":[");
        if (assignedUsers != null && !assignedUsers.isEmpty()) {
            for (int i = 0; i < assignedUsers.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(assignedUsers.get(i));
            }
        }
        sb.append("],");
        
        // Agregar assignedUsersNames como array
        sb.append("\"assignedUsersNames\":[");
        if (assignedUsersNames != null && !assignedUsersNames.isEmpty()) {
            for (int i = 0; i < assignedUsersNames.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(assignedUsersNames.get(i))).append("\"");
            }
        }
        sb.append("],");
        
        sb.append("\"dueDate\":").append(dueDate != null ? "\"" + dueDate.format(formatter) + "\"" : "null").append(",");
        sb.append("\"createdAt\":").append(createdAt != null ? "\"" + createdAt.format(formatter) + "\"" : "null").append(",");
        sb.append("\"updatedAt\":").append(updatedAt != null ? "\"" + updatedAt.format(formatter) + "\"" : "null").append(",");
        sb.append("\"completedAt\":").append(completedAt != null ? "\"" + completedAt.format(formatter) + "\"" : "null");
        sb.append("}");
        return sb.toString();
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

