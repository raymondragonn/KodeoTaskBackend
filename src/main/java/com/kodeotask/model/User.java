package com.kodeotask.model;

import java.time.LocalDateTime;

/**
 * modelo de usuario
 */
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public User() {}
    
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
    
    /**
     * convierte el usuario a JSON
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":").append(id != null ? id : 0);
        sb.append(",\"username\":\"").append(username != null ? escapeJson(username) : "").append("\"");
        sb.append(",\"email\":\"").append(email != null ? escapeJson(email) : "").append("\"");
        sb.append(",\"firstName\":").append(firstName != null ? "\"" + escapeJson(firstName) + "\"" : "null");
        sb.append(",\"lastName\":").append(lastName != null ? "\"" + escapeJson(lastName) + "\"" : "null");
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * escapa caracteres especiales para JSON
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

