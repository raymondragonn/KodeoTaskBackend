package com.kodeotask.service;

import com.kodeotask.dto.AuthResponse;
import com.kodeotask.dto.LoginRequest;
import com.kodeotask.dto.RegisterRequest;
import com.kodeotask.model.User;
import com.kodeotask.repository.UserRepository;
import com.kodeotask.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public AuthResponse register(RegisterRequest request) {
        // Validar que el username no exista
        if (userRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse(false, "El nombre de usuario ya existe");
        }
        
        // Validar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "El email ya está registrado");
        }
        
        // Validaciones básicas
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return new AuthResponse(false, "La contraseña debe tener al menos 6 caracteres");
        }
        
        // Crear nuevo usuario
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        try {
            user = userRepository.save(user);
            return new AuthResponse(true, "Usuario registrado correctamente");
        } catch (Exception e) {
            return new AuthResponse(false, "Error al registrar usuario: " + e.getMessage());
        }
    }
    
    public AuthResponse login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());
        
        if (userOptional.isEmpty()) {
            AuthResponse response = new AuthResponse(false, null, null, null);
            response.setError("Usuario o contraseña incorrectos");
            return response;
        }
        
        User user = userOptional.get();
        
        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            AuthResponse response = new AuthResponse(false, null, null, null);
            response.setError("Usuario o contraseña incorrectos");
            return response;
        }
        
        // Generar token JWT
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        AuthResponse response = new AuthResponse(true, token, user.getId(), user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }
}

