package com.kodeotask.service;

import com.kodeotask.model.Task;
import com.kodeotask.socket.UDPServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de notificaciones en tiempo real.
 * 
 * - UDP puro (para clientes custom)
 * - WebSocket/STOMP (para el frontend Angular)
 */
@Service
public class NotificationService {
    
    @Autowired
    private UDPServer udpServer;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Enviar notificación cuando se crea una tarea
    public void notifyTaskCreated(Task task) {
        Long creatorId = task.getCreatedBy();
        if (creatorId != null) {
            sendNotification(creatorId, "task_created", task);
        }
    }
    
    // Enviar notificación cuando se actualiza una tarea
    public void notifyTaskUpdated(Task task) {
        Long creatorId = task.getCreatedBy();
        if (creatorId != null) {
            sendNotification(creatorId, "task_updated", task);
        }
    }
    
    // Enviar notificación cuando se elimina una tarea
    public void notifyTaskDeleted(Long taskId, Long userId) {
        // Para eliminación, enviamos null como task
        sendNotification(userId, "task_deleted", null);
    }
    
    // Enviar notificación cuando se asigna una tarea (para cada usuario asignado)
    public void notifyTaskAssigned(Task task, Long assignedToUserId) {
        sendNotification(assignedToUserId, "task_assigned", task);
    }
    
    /**
     * Envía una notificación tanto por UDP como por WebSocket/STOMP.
     */
    private void sendNotification(Long userId, String type, Task task) {
        // Notificación UDP existente (para compatibilidad)
        udpServer.sendNotification(userId, type, task);
        
        // Notificación WebSocket para el frontend Angular
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("task", task);
            notification.put("timestamp", System.currentTimeMillis());
            
            String destination = "/topic/user/" + userId + "/tasks";
            messagingTemplate.convertAndSend(destination, notification);
        } catch (Exception e) {
            // No romper el flujo si falla WebSocket; ya se envió por UDP
            System.err.println("Error al enviar notificación WebSocket: " + e.getMessage());
        }
    }
}

