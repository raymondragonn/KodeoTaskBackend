package com.kodeotask.util;

import com.kodeotask.model.Task;
import com.kodeotask.model.TaskStatus;
import com.kodeotask.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * utilidad para parsear y generar JSON
 */
public class JsonUtil {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * parsea un JSON simple a un Map
     */
    public static Map<String, String> parseJson(String json) {
        Map<String, String> result = new HashMap<>();
        
        if (json == null || json.isEmpty()) {
            return result;
        }
        
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|\\[[^\\]]*\\]|[^,}]+)");
        Matcher matcher = pattern.matcher(json);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            
            if (value.equals("null")) {
                value = null;
            }
            
            result.put(key, value);
        }
        
        return result;
    }
    
    /**
     * convierte un Map a JSON
     */
    public static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * convierte una lista de tareas a JSON
     */
    public static String tasksToJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        
        for (Task task : tasks) {
            if (!first) sb.append(",");
            first = false;
            sb.append(task.toJson());
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * convierte una lista de usuarios a JSON
     */
    public static String usersToJson(List<User> users) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        
        for (User user : users) {
            if (!first) sb.append(",");
            first = false;
            sb.append(user.toJson());
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * parsea un JSON a un objeto Task
     */
    public static Task parseTask(String json) {
        Task task = new Task();
        
        if (json != null && json.contains("\"assignedUsers\"")) {
            Pattern arrayPattern = Pattern.compile("\"assignedUsers\"\\s*:\\s*\\[([^\\]]*)\\]");
            Matcher arrayMatcher = arrayPattern.matcher(json);
            if (arrayMatcher.find()) {
                String arrayContent = arrayMatcher.group(1);
                List<Long> assignedUsers = new ArrayList<>();
                if (arrayContent != null && !arrayContent.trim().isEmpty()) {
                    String[] userIds = arrayContent.split(",");
                    for (String userIdStr : userIds) {
                        try {
                            assignedUsers.add(Long.parseLong(userIdStr.trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                task.setAssignedUsers(assignedUsers);
            } else {
                if (json.contains("\"assignedUsers\":[]")) {
                    task.setAssignedUsers(new ArrayList<>());
                }
            }
        }
        
        Map<String, String> map = parseJson(json);
        
        if (map.containsKey("id") && map.get("id") != null) {
            try {
                task.setId(Long.parseLong(map.get("id")));
            } catch (NumberFormatException ignored) {}
        }
        
        task.setTitle(map.get("title"));
        task.setDescription(map.get("description"));
        task.setCategory(map.get("category"));
        
        String statusStr = map.get("status");
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                task.setStatus(TaskStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                task.setStatus(TaskStatus.PENDING);
            }
        }
        
        if (map.containsKey("createdBy") && map.get("createdBy") != null) {
            try {
                task.setCreatedBy(Long.parseLong(map.get("createdBy")));
            } catch (NumberFormatException ignored) {}
        }
        
        if (map.containsKey("assignedTo") && map.get("assignedTo") != null) {
            try {
                task.setAssignedTo(Long.parseLong(map.get("assignedTo")));
            } catch (NumberFormatException ignored) {}
        }
        
        if (map.containsKey("dueDate") && map.get("dueDate") != null) {
            String dueDateStr = map.get("dueDate").trim();
            if (!dueDateStr.isEmpty()) {
                try {
                    task.setDueDate(LocalDateTime.parse(dueDateStr, DATE_TIME_FORMATTER));
                } catch (DateTimeParseException e1) {
                    try {
                        LocalDate date = LocalDate.parse(dueDateStr, DATE_FORMATTER);
                        task.setDueDate(date.atStartOfDay());
                    } catch (DateTimeParseException e2) {
                        System.err.println("Error parseando fecha dueDate: " + dueDateStr);
                    }
                }
            }
        }
        
        return task;
    }
    
    /**
     * escapa caracteres especiales para JSON
     */
    public static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

