package com.kodeotask.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * utilidad para generación y validación de tokens
 */
public class TokenUtil {
    
    private static final String SECRET_KEY = "kodeotask-secret-key-2024-muy-segura";
    private static final long TOKEN_DURATION = 24 * 60 * 60 * 1000;
    private static final Map<String, TokenData> tokenCache = new ConcurrentHashMap<>();
    
    private static class TokenData {
        Long userId;
        String username;
        long expiration;
        
        TokenData(Long userId, String username, long expiration) {
            this.userId = userId;
            this.username = username;
            this.expiration = expiration;
        }
    }
    
    /**
     * genera un token para el usuario
     */
    public static String generateToken(Long userId, String username) {
        long expiration = System.currentTimeMillis() + TOKEN_DURATION;
        
        String payload = userId + ":" + username + ":" + expiration;
        String signature = createSignature(payload);
        
        String token = Base64.getEncoder().encodeToString(
            (payload + ":" + signature).getBytes(StandardCharsets.UTF_8)
        );
        
        tokenCache.put(token, new TokenData(userId, username, expiration));
        
        return token;
    }
    
    /**
     * valida un token y devuelve el userId si es válido
     */
    public static Long validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            // Verificar en cache primero
            TokenData cached = tokenCache.get(token);
            if (cached != null) {
                if (System.currentTimeMillis() < cached.expiration) {
                    return cached.userId;
                } else {
                    tokenCache.remove(token);
                    return null;
                }
            }
            
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            
            if (parts.length != 4) {
                return null;
            }
            
            Long userId = Long.parseLong(parts[0]);
            String username = parts[1];
            long expiration = Long.parseLong(parts[2]);
            String signature = parts[3];
            
            if (System.currentTimeMillis() > expiration) {
                return null;
            }
            
            String payload = userId + ":" + username + ":" + expiration;
            String expectedSignature = createSignature(payload);
            
            if (!signature.equals(expectedSignature)) {
                return null;
            }
            
            tokenCache.put(token, new TokenData(userId, username, expiration));
            
            return userId;
            
        } catch (Exception e) {
            System.err.println("Error al validar token: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * extrae el username del token
     */
    public static String extractUsername(String token) {
        try {
            TokenData cached = tokenCache.get(token);
            if (cached != null) {
                return cached.username;
            }
            
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            
            if (parts.length >= 2) {
                return parts[1];
            }
        } catch (Exception e) {
            System.err.println("Error al extraer username: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Crea una firma para el payload
     */
    private static String createSignature(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 32);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear firma: " + e.getMessage());
        }
    }
    
    /**
     * limpia tokens expirados del cache
     */
    public static void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        tokenCache.entrySet().removeIf(entry -> entry.getValue().expiration < now);
    }
}

