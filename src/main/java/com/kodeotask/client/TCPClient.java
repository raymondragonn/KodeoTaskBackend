package com.kodeotask.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * cliente TCP de prueba para enviar peticiones HTTP al servidor
 */
public class TCPClient {
    
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8081;
    
    private String currentToken = null;
    private Long currentUserId = null;
    
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   CLIENTE TCP DE PRUEBA - KodeoTask    ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║   Host: " + String.format("%-30s", host) + "║");
        System.out.println("║   Puerto: " + String.format("%-28d", port) + "║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        TCPClient client = new TCPClient();
        client.runInteractiveMode(host, port);
    }
    
    /**
     * Modo interactivo con menú
     */
    public void runInteractiveMode(String host, int port) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            printMenu();
            System.out.print("Selecciona una opción: ");
            String option = scanner.nextLine().trim();
        
            try {
                switch (option) {
                    case "1" -> doRegister(host, port, scanner);
                    case "2" -> doLogin(host, port, scanner);
                    case "3" -> doGetTasks(host, port);
                    case "4" -> doCreateTask(host, port, scanner);
                    case "5" -> doUpdateTask(host, port, scanner);
                    case "6" -> doDeleteTask(host, port, scanner);
                    case "7" -> doCustomRequest(host, port, scanner);
                    case "8" -> showCurrentToken();
                    case "0", "exit", "quit" -> {
                        System.out.println("\n¡Hasta luego!");
                        scanner.close();
                        return;
                    }
                    default -> System.out.println("Opción no válida");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            
            System.out.println();
        }
    }
    
    private void printMenu() {
        System.out.println("┌────────────────────────────────────────┐");
        System.out.println("│              MENÚ PRINCIPAL            │");
        System.out.println("├────────────────────────────────────────┤");
        System.out.println("│  1. Registrar usuario                  │");
        System.out.println("│  2. Login                              │");
        System.out.println("│  3. Ver mis tareas                     │");
        System.out.println("│  4. Crear tarea                        │");
        System.out.println("│  5. Actualizar tarea                   │");
        System.out.println("│  6. Eliminar tarea                     │");
        System.out.println("│  7. Petición HTTP personalizada        │");
        System.out.println("│  8. Ver token actual                   │");
        System.out.println("│  0. Salir                              │");
        System.out.println("└────────────────────────────────────────┘");
        
        if (currentToken != null) {
            System.out.println("✓ Sesión activa - Usuario ID: " + currentUserId);
        } else {
            System.out.println("✗ No has iniciado sesión");
        }
        System.out.println();
    }
    
    private void doRegister(String host, int port, Scanner scanner) throws IOException {
        System.out.println("\n=== REGISTRO DE USUARIO ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Nombre: ");
        String firstName = scanner.nextLine();
        System.out.print("Apellido: ");
        String lastName = scanner.nextLine();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine();
        
        String json = String.format(
            "{\"username\":\"%s\",\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"password\":\"%s\"}",
            username, email, firstName, lastName, password
        );
        
        String response = sendRequest(host, port, "POST", "/api/auth/register", null, json);
        System.out.println("\n--- Respuesta ---");
        System.out.println(response);
    }
    
    private void doLogin(String host, int port, Scanner scanner) throws IOException {
        System.out.println("\n=== INICIO DE SESIÓN ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine();
        
        String json = String.format(
            "{\"username\":\"%s\",\"password\":\"%s\"}",
            username, password
        );
        
        String response = sendRequest(host, port, "POST", "/api/auth/login", null, json);
        System.out.println("\n--- Respuesta ---");
        System.out.println(response);
        
        if (response.contains("\"token\":\"")) {
            int start = response.indexOf("\"token\":\"") + 9;
            int end = response.indexOf("\"", start);
            currentToken = response.substring(start, end);
            
            // Extraer userId
            if (response.contains("\"userId\":")) {
                int userIdStart = response.indexOf("\"userId\":") + 9;
                int userIdEnd = response.indexOf(",", userIdStart);
                if (userIdEnd == -1) userIdEnd = response.indexOf("}", userIdStart);
                currentUserId = Long.parseLong(response.substring(userIdStart, userIdEnd).trim());
            }
            
            System.out.println("\n✓ Login exitoso - Token guardado");
        }
    }
    
    private void doGetTasks(String host, int port) throws IOException {
        if (currentToken == null) {
            System.out.println("✗ Debes iniciar sesión primero");
            return;
        }
        
        System.out.println("\n=== MIS TAREAS ===");
        String response = sendRequest(host, port, "GET", "/api/tasks", currentToken, null);
        System.out.println("\n--- Respuesta ---");
        prettyPrintJson(response);
    }
    
    private void doCreateTask(String host, int port, Scanner scanner) throws IOException {
        if (currentToken == null) {
            System.out.println("✗ Debes iniciar sesión primero");
            return;
        }
        
        System.out.println("\n=== CREAR TAREA ===");
        System.out.print("Título: ");
        String title = scanner.nextLine();
        System.out.print("Descripción: ");
        String description = scanner.nextLine();
        System.out.print("Categoría (opcional): ");
        String category = scanner.nextLine();
        System.out.print("Estado (PENDING/IN_PROGRESS/COMPLETED): ");
        String status = scanner.nextLine();
        if (status.isEmpty()) status = "PENDING";
        
        StringBuilder json = new StringBuilder();
        json.append("{\"title\":\"").append(title).append("\"");
        json.append(",\"description\":\"").append(description).append("\"");
        json.append(",\"status\":\"").append(status).append("\"");
        if (!category.isEmpty()) {
            json.append(",\"category\":\"").append(category).append("\"");
        }
        json.append("}");
        
        String response = sendRequest(host, port, "POST", "/api/tasks", currentToken, json.toString());
        System.out.println("\n--- Respuesta ---");
        prettyPrintJson(response);
    }
    
    private void doUpdateTask(String host, int port, Scanner scanner) throws IOException {
        if (currentToken == null) {
            System.out.println("✗ Debes iniciar sesión primero");
            return;
        }
        
        System.out.println("\n=== ACTUALIZAR TAREA ===");
        System.out.print("ID de la tarea: ");
        String taskId = scanner.nextLine();
        System.out.print("Nuevo título: ");
        String title = scanner.nextLine();
        System.out.print("Nueva descripción: ");
        String description = scanner.nextLine();
        System.out.print("Nuevo estado (PENDING/IN_PROGRESS/COMPLETED): ");
        String status = scanner.nextLine();
        
        StringBuilder json = new StringBuilder();
        json.append("{\"title\":\"").append(title).append("\"");
        json.append(",\"description\":\"").append(description).append("\"");
        if (!status.isEmpty()) {
            json.append(",\"status\":\"").append(status).append("\"");
        }
        json.append("}");
        
        String response = sendRequest(host, port, "PUT", "/api/tasks/" + taskId, currentToken, json.toString());
        System.out.println("\n--- Respuesta ---");
        prettyPrintJson(response);
    }
    
    private void doDeleteTask(String host, int port, Scanner scanner) throws IOException {
        if (currentToken == null) {
            System.out.println("✗ Debes iniciar sesión primero");
            return;
        }
        
        System.out.println("\n=== ELIMINAR TAREA ===");
        System.out.print("ID de la tarea: ");
        String taskId = scanner.nextLine();
        
        String response = sendRequest(host, port, "DELETE", "/api/tasks/" + taskId, currentToken, null);
        System.out.println("\n--- Respuesta ---");
        System.out.println(response.isEmpty() ? "✓ Tarea eliminada correctamente" : response);
    }
    
    private void doCustomRequest(String host, int port, Scanner scanner) throws IOException {
        System.out.println("\n=== PETICIÓN PERSONALIZADA ===");
        System.out.print("Método (GET/POST/PUT/DELETE): ");
        String method = scanner.nextLine().toUpperCase();
        System.out.print("Ruta (ej: /api/tasks): ");
        String path = scanner.nextLine();
        System.out.print("¿Incluir token? (s/n): ");
        String includeToken = scanner.nextLine();
        String token = includeToken.equalsIgnoreCase("s") ? currentToken : null;
        
        String body = null;
        if (method.equals("POST") || method.equals("PUT")) {
            System.out.println("Body JSON (termina con línea vacía):");
            StringBuilder bodyBuilder = new StringBuilder();
            String line;
            while (!(line = scanner.nextLine()).isEmpty()) {
                bodyBuilder.append(line);
            }
            body = bodyBuilder.toString();
        }
        
        String response = sendRequest(host, port, method, path, token, body);
        System.out.println("\n--- Respuesta ---");
        prettyPrintJson(response);
    }
    
    private void showCurrentToken() {
        if (currentToken != null) {
            System.out.println("\n=== TOKEN ACTUAL ===");
            System.out.println("User ID: " + currentUserId);
            System.out.println("Token: " + currentToken);
        } else {
            System.out.println("\n✗ No hay token - Inicia sesión primero");
        }
    }
    
    /**
     * envía una petición HTTP al servidor
     */
    private String sendRequest(String host, int port, String method, String path, 
                              String token, String body) throws IOException {
        
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            StringBuilder request = new StringBuilder();
            request.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
            request.append("Host: ").append(host).append(":").append(port).append("\r\n");
            
            if (token != null) {
                request.append("Authorization: Bearer ").append(token).append("\r\n");
            }
            
            if (body != null && !body.isEmpty()) {
                request.append("Content-Type: application/json\r\n");
                request.append("Content-Length: ").append(body.getBytes().length).append("\r\n");
            }
            
            request.append("\r\n");
            
            if (body != null && !body.isEmpty()) {
                request.append(body);
            }
            
            // Enviar petición
            out.print(request);
            out.flush();
            
            StringBuilder response = new StringBuilder();
            String line;
            int contentLength = 0;
            boolean readingHeaders = true;
            
            while ((line = in.readLine()) != null) {
                if (readingHeaders) {
                    if (line.isEmpty()) {
                        readingHeaders = false;
                        // Leer body si hay
                        if (contentLength > 0) {
                            char[] bodyChars = new char[contentLength];
                            int read = in.read(bodyChars, 0, contentLength);
                            if (read > 0) {
                                response.append(new String(bodyChars, 0, read));
                            }
                        }
                        break;
                    } else if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }
            }
            
            return response.toString();
        }
    }
    
    /**
     * imprime JSON de forma más legible
     */
    private void prettyPrintJson(String json) {
        if (json == null || json.isEmpty()) {
            System.out.println("(vacío)");
            return;
        }
        
        int indent = 0;
        boolean inString = false;
        StringBuilder formatted = new StringBuilder();
        
        for (char c : json.toCharArray()) {
            if (c == '"' && (formatted.length() == 0 || formatted.charAt(formatted.length() - 1) != '\\')) {
                inString = !inString;
                formatted.append(c);
            } else if (!inString) {
                switch (c) {
                    case '{', '[' -> {
                        formatted.append(c).append("\n").append("  ".repeat(++indent));
                    }
                    case '}', ']' -> {
                        formatted.append("\n").append("  ".repeat(--indent)).append(c);
                    }
                    case ',' -> {
                        formatted.append(c).append("\n").append("  ".repeat(indent));
                    }
                    case ':' -> {
                        formatted.append(": ");
                    }
                    default -> {
                        if (!Character.isWhitespace(c)) formatted.append(c);
                    }
                }
            } else {
                formatted.append(c);
            }
        }
        
        System.out.println(formatted);
    }
}

