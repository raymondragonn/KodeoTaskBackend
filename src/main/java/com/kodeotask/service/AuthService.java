package com.kodeotask.service;

import com.kodeotask.dao.UserDAO;
import com.kodeotask.model.User;
import com.kodeotask.util.PasswordUtil;
import com.kodeotask.util.TokenUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * servicio de autenticación
 */
public class AuthService {
    
    private final UserDAO userDAO;
    
    public AuthService() {
        this.userDAO = new UserDAO();
    }
    
    /**
     * registra un nuevo usuario
     */
    public Map<String, Object> register(String username, String email, String password, 
                                        String firstName, String lastName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (userDAO.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "El nombre de usuario ya existe");
                return response;
            }
            
            if (userDAO.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "El email ya está registrado");
                return response;
            }
            
            if (password == null || password.length() < 6) {
                response.put("success", false);
                response.put("message", "La contraseña debe tener al menos 6 caracteres");
                return response;
            }
            
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(PasswordUtil.hashPassword(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            
            user = userDAO.create(user);
            
            response.put("success", true);
            response.put("message", "Usuario registrado correctamente");
            response.put("userId", user.getId());
            
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al registrar usuario: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * inicia sesión de un usuario
     */
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOpt = userDAO.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return response;
            }
            
            User user = userOpt.get();
            
            if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
                response.put("success", false);
                response.put("message", "Contraseña incorrecta");
                return response;
            }
            
            String token = TokenUtil.generateToken(user.getId(), user.getUsername());
            
            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error al iniciar sesión: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * valida un token y devuelve el userId
     */
    public Long validateToken(String token) {
        return TokenUtil.validateToken(token);
    }
    
    /**
     * obtiene un usuario por ID
     */
    public Optional<User> getUserById(Long id) {
        try {
            return userDAO.findById(id);
        } catch (SQLException e) {
            System.err.println("Error al obtener usuario: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * obtiene todos los usuarios
     */
    public java.util.List<User> getAllUsers() {
        try {
            return userDAO.findAll();
        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}

