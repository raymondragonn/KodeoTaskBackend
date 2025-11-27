package com.kodeotask.config;

import com.kodeotask.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Permitir peticiones públicas sin token
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/auth/") || requestPath.startsWith("/ws/") || 
            requestPath.equals("/api/users") || requestPath.startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authHeader = request.getHeader("Authorization");
        
        System.out.println("=== DEBUG JWT Filter ===");
        System.out.println("Request path: " + requestPath);
        System.out.println("Authorization header: " + (authHeader != null ? "presente" : "ausente"));
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("ERROR: No hay token en la petición");
            // Para rutas protegidas, rechazar explícitamente
            if (requestPath.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token de autenticación requerido\"}");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String token = authHeader.substring(7);
            final String username = jwtUtil.extractUsername(token);
            final Long userId = jwtUtil.extractUserId(token);
            
            System.out.println("Token extraído: " + (token != null ? "presente" : "null"));
            System.out.println("Username extraído: " + username);
            System.out.println("UserId extraído: " + userId);
            
            if (username == null || userId == null) {
                System.out.println("ERROR: No se pudo extraer username o userId del token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token inválido: no se pudo extraer información del usuario\"}");
                return;
            }
            
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtil.validateToken(token, username)) {
                    System.out.println("Token válido, estableciendo autenticación");
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId, // Usamos userId como principal
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("Autenticación establecida con userId=" + userId);
                } else {
                    System.out.println("ERROR: Token inválido o expirado");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token inválido o expirado\"}");
                    return;
                }
            } else {
                System.out.println("Ya hay autenticación en el contexto");
            }
        } catch (Exception e) {
            System.err.println("ERROR en JWT Filter: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Error al procesar el token: " + e.getMessage() + "\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}

