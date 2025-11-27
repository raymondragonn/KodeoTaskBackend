package com.kodeotask.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser simple para peticiones HTTP
 */
public class HTTPParser {
    
    public static class HTTPRequest {
        private String method;
        private String path;
        private String version;
        private Map<String, String> headers;
        private String body;
        
        public HTTPRequest() {
            this.headers = new HashMap<>();
        }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public Map<String, String> getHeaders() { return headers; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        public String getHeader(String key) {
            return headers.get(key.toLowerCase());
        }
    }
    
    /**
     * Parsea una petición HTTP completa
     */
    public static HTTPRequest parse(String rawRequest) throws IOException {
        HTTPRequest request = new HTTPRequest();
        BufferedReader reader = new BufferedReader(new StringReader(rawRequest));
        
        // Parsear primera línea (Request Line)
        String requestLine = reader.readLine();
        if (requestLine == null) {
            throw new IOException("Request line vacía");
        }
        
        String[] parts = requestLine.split(" ", 3);
        if (parts.length >= 3) {
            request.setMethod(parts[0]);
            request.setPath(parts[1]);
            request.setVersion(parts[2]);
        }
        
        // Parsear headers
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim().toLowerCase();
                String value = line.substring(colonIndex + 1).trim();
                request.getHeaders().put(key, value);
            }
        }
        
        // Parsear body si existe
        StringBuilder body = new StringBuilder();
        String contentLengthStr = request.getHeader("content-length");
        if (contentLengthStr != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthStr);
                char[] buffer = new char[contentLength];
                int read = reader.read(buffer, 0, contentLength);
                if (read > 0) {
                    body.append(buffer, 0, read);
                }
            } catch (NumberFormatException e) {
                // Ignorar si no se puede parsear content-length
            }
        }
        
        request.setBody(body.toString());
        return request;
    }
    
    /**
     * Genera una respuesta HTTP
     */
    public static String buildResponse(int statusCode, String statusMessage, 
                                      Map<String, String> headers, String body) {
        StringBuilder response = new StringBuilder();
        
        // Status line
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
        
        // Headers
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }
        }
        
        // Content-Length si hay body
        if (body != null && !body.isEmpty()) {
            response.append("Content-Length: ").append(body.length()).append("\r\n");
        }
        
        // Línea vacía antes del body
        response.append("\r\n");
        
        // Body
        if (body != null) {
            response.append(body);
        }
        
        return response.toString();
    }
}

