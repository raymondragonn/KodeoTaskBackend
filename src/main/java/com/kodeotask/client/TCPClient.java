package com.kodeotask.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// cliente tcp para enviar peticiones http al servidor
public class TCPClient {
    
    private static final String host_por_defecto = "localhost";
    private static final int puerto_por_defecto = 8081;
    
    private String token_actual = null;
    private Long id_usuario_actual = null;
    
    public static void main(String[] args) {
        String host = host_por_defecto;
        int puerto = puerto_por_defecto;
        
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            puerto = Integer.parseInt(args[1]);
        }
        
        System.out.println("========================================");
        System.out.println("  CLIENTE TCP - KodeoTask");
        System.out.println("========================================");
        System.out.println("Host: " + host);
        System.out.println("Puerto: " + puerto);
        System.out.println("========================================\n");
        
        TCPClient cliente = new TCPClient();
        cliente.ejecutar(host, puerto);
    }
    
    // modo interactivo con menu
    public void ejecutar(String host, int puerto) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            mostrar_menu();
            System.out.print("Selecciona una opcion: ");
            String opcion = scanner.nextLine().trim();
        
            try {
                if (opcion.equals("1")) {
                    registrar_usuario(host, puerto, scanner);
                } else if (opcion.equals("2")) {
                    hacer_login(host, puerto, scanner);
                } else if (opcion.equals("3")) {
                    crear_tarea(host, puerto, scanner);
                } else if (opcion.equals("4")) {
                    actualizar_tarea(host, puerto, scanner);
                } else if (opcion.equals("5")) {
                    eliminar_tarea(host, puerto, scanner);
                } else if (opcion.equals("6")) {
                    ver_tareas(host, puerto);
                } else if (opcion.equals("7")) {
                    asignar_tarea(host, puerto, scanner);
                } else if (opcion.equals("0") || opcion.equals("exit") || opcion.equals("quit")) {
                    System.out.println("\nHasta luego!");
                    scanner.close();
                    return;
                } else {
                    System.out.println("Opcion no valida");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            
            System.out.println();
        }
    }
    
    private void mostrar_menu() {
        System.out.println("========================================");
        System.out.println("  MENU PRINCIPAL");
        System.out.println("========================================");
        System.out.println("1. Registrar usuario");
        System.out.println("2. Login");
        System.out.println("3. Crear tarea");
        System.out.println("4. Actualizar tarea");
        System.out.println("5. Eliminar tarea");
        System.out.println("6. Ver tareas");
        System.out.println("7. Asignar tarea");
        System.out.println("0. Salir");
        System.out.println("========================================");
        
        if (token_actual != null) {
            System.out.println("Sesion activa - Usuario ID: " + id_usuario_actual);
        } else {
            System.out.println("No has iniciado sesion");
        }
        System.out.println();
    }
    
    private void registrar_usuario(String host, int puerto, Scanner scanner) throws IOException {
        System.out.println("\n=== REGISTRO DE USUARIO ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();
        System.out.print("Apellido: ");
        String apellido = scanner.nextLine();
        System.out.print("Contrasena: ");
        String contrasena = scanner.nextLine();
        
        String json = "{\"username\":\"" + username + "\",\"email\":\"" + email + 
                      "\",\"firstName\":\"" + nombre + "\",\"lastName\":\"" + apellido + 
                      "\",\"password\":\"" + contrasena + "\"}";
        
        String respuesta = enviar_peticion(host, puerto, "POST", "/api/auth/register", null, json);
        System.out.println("\n--- Respuesta ---");
        System.out.println(respuesta);
    }
    
    private void hacer_login(String host, int puerto, Scanner scanner) throws IOException {
        System.out.println("\n=== INICIO DE SESION ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Contrasena: ");
        String contrasena = scanner.nextLine();
        
        String json = "{\"username\":\"" + username + "\",\"password\":\"" + contrasena + "\"}";
        
        String respuesta = enviar_peticion(host, puerto, "POST", "/api/auth/login", null, json);
        System.out.println("\n--- Respuesta ---");
        System.out.println(respuesta);
        
        if (respuesta.contains("\"token\":\"")) {
            int inicio = respuesta.indexOf("\"token\":\"") + 9;
            int fin = respuesta.indexOf("\"", inicio);
            token_actual = respuesta.substring(inicio, fin);
            
            if (respuesta.contains("\"userId\":")) {
                int inicio_id = respuesta.indexOf("\"userId\":") + 9;
                int fin_id = respuesta.indexOf(",", inicio_id);
                if (fin_id == -1) {
                    fin_id = respuesta.indexOf("}", inicio_id);
                }
                id_usuario_actual = Long.parseLong(respuesta.substring(inicio_id, fin_id).trim());
            }
            
            System.out.println("\nLogin exitoso - Token guardado");
        }
    }
    
    private void ver_tareas(String host, int puerto) throws IOException {
        if (token_actual == null) {
            System.out.println("Debes iniciar sesion primero");
            return;
        }
        
        System.out.println("\n=== MIS TAREAS ===");
        String respuesta = enviar_peticion(host, puerto, "GET", "/api/tasks", token_actual, null);
        System.out.println("\n--- Respuesta ---");
        System.out.println(respuesta);
    }
    
    private void crear_tarea(String host, int puerto, Scanner scanner) throws IOException {
        if (token_actual == null) {
            System.out.println("Debes iniciar sesion primero");
            return;
        }
        
        System.out.println("\n=== CREAR TAREA ===");
        System.out.print("Titulo: ");
        String titulo = scanner.nextLine();
        System.out.print("Descripcion: ");
        String descripcion = scanner.nextLine();
        System.out.print("Categoria (opcional): ");
        String categoria = scanner.nextLine();
        System.out.print("Estado (PENDING/IN_PROGRESS/COMPLETED): ");
        String estado = scanner.nextLine();
        if (estado.length() == 0) {
            estado = "PENDING";
        }
        
        String json = "{\"title\":\"" + titulo + "\",\"description\":\"" + descripcion + 
                      "\",\"status\":\"" + estado + "\"";
        if (categoria.length() > 0) {
            json += ",\"category\":\"" + categoria + "\"";
        }
        json += "}";
        
        String respuesta = enviar_peticion(host, puerto, "POST", "/api/tasks", token_actual, json);
        System.out.println("\n--- Respuesta ---");
        System.out.println(respuesta);
    }
    
    private void actualizar_tarea(String host, int puerto, Scanner scanner) throws IOException {
        if (token_actual == null) {
            System.out.println("Debes iniciar sesion primero");
            return;
        }
        
        System.out.println("\n=== ACTUALIZAR TAREA ===");
        System.out.print("ID de la tarea: ");
        String id_tarea = scanner.nextLine();
        System.out.print("Nuevo titulo: ");
        String titulo = scanner.nextLine();
        System.out.print("Nueva descripcion: ");
        String descripcion = scanner.nextLine();
        System.out.print("Nuevo estado (PENDING/IN_PROGRESS/COMPLETED): ");
        String estado = scanner.nextLine();
        
        String json = "{\"title\":\"" + titulo + "\",\"description\":\"" + descripcion + "\"";
        if (estado.length() > 0) {
            json += ",\"status\":\"" + estado + "\"";
        }
        json += "}";
        
        String respuesta = enviar_peticion(host, puerto, "PUT", "/api/tasks/" + id_tarea, token_actual, json);
        System.out.println("\n--- Respuesta ---");
        System.out.println(respuesta);
    }
    
    private void eliminar_tarea(String host, int puerto, Scanner scanner) throws IOException {
        if (token_actual == null) {
            System.out.println("Debes iniciar sesion primero");
            return;
        }
        
        System.out.println("\n=== ELIMINAR TAREA ===");
        System.out.print("ID de la tarea: ");
        String id_tarea = scanner.nextLine();
        
        String respuesta = enviar_peticion(host, puerto, "DELETE", "/api/tasks/" + id_tarea, token_actual, null);
        System.out.println("\n--- Respuesta ---");
        if (respuesta.length() == 0) {
            System.out.println("Tarea eliminada correctamente");
        } else {
            System.out.println(respuesta);
        }
    }
    
    private void asignar_tarea(String host, int puerto, Scanner scanner) throws IOException {
        if (token_actual == null) {
            System.out.println("Debes iniciar sesion primero");
            return;
        }
        
        System.out.println("\n=== ASIGNAR TAREA ===");
        System.out.print("ID de la tarea: ");
        String id_tarea = scanner.nextLine();
        System.out.print("Username del usuario a asignar: ");
        String username = scanner.nextLine().trim();
        
        String respuesta_usuarios = enviar_peticion(host, puerto, "GET", "/api/users", token_actual, null);
        
        if (respuesta_usuarios == null || respuesta_usuarios.length() == 0 || respuesta_usuarios.contains("error")) {
            System.out.println("Error al obtener lista de usuarios");
            return;
        }
        
        Long id_usuario_destino = buscar_id_usuario(respuesta_usuarios, username);
        
        if (id_usuario_destino == null) {
            System.out.println("Usuario '" + username + "' no encontrado");
            return;
        }
        
        String respuesta_tarea = enviar_peticion(host, puerto, "GET", "/api/tasks/" + id_tarea, token_actual, null);
        
        if (respuesta_tarea == null || respuesta_tarea.length() == 0 || respuesta_tarea.contains("error")) {
            System.out.println("Error al obtener la tarea o tarea no encontrada");
            return;
        }
        
        com.kodeotask.model.Task tarea = com.kodeotask.util.JsonUtil.parseTask(respuesta_tarea);
        
        java.util.List<Long> usuarios_asignados = tarea.getAssignedUsers();
        if (usuarios_asignados == null) {
            usuarios_asignados = new java.util.ArrayList<Long>();
        }
        
        if (usuarios_asignados.contains(id_usuario_destino)) {
            System.out.println("El usuario '" + username + "' ya esta asignado a esta tarea");
            return;
        }
        
        usuarios_asignados.add(id_usuario_destino);
        
        String json_actualizar = "{\"assignedUsers\":[";
        for (int i = 0; i < usuarios_asignados.size(); i++) {
            if (i > 0) {
                json_actualizar += ",";
            }
            json_actualizar += usuarios_asignados.get(i);
        }
        json_actualizar += "]}";
        
        System.out.println("Asignando a usuarios: " + usuarios_asignados);
        
        String respuesta = enviar_peticion(host, puerto, "PUT", "/api/tasks/" + id_tarea, token_actual, json_actualizar);
        System.out.println("\n--- Respuesta ---");
        System.out.println(respuesta);
        
        if (!respuesta.contains("error")) {
            System.out.println("\nTarea asignada exitosamente a '" + username + "' (ID: " + id_usuario_destino + ")");
            System.out.println("Se enviara una notificacion UDP automaticamente");
        } else {
            System.out.println("\nError al asignar la tarea");
        }
    }
    
    private Long buscar_id_usuario(String json_usuarios, String username) {
        int indice = 0;
        while (true) {
            int inicio_usuario = json_usuarios.indexOf("{", indice);
            if (inicio_usuario == -1) {
                break;
            }
            
            int fin_usuario = json_usuarios.indexOf("}", inicio_usuario);
            if (fin_usuario == -1) {
                break;
            }
            
            String usuario_json = json_usuarios.substring(inicio_usuario, fin_usuario + 1);
            
            String patron_username = "\"username\":\"" + username.replace("\"", "\\\"") + "\"";
            if (usuario_json.contains(patron_username)) {
                int inicio_id = usuario_json.indexOf("\"id\":");
                if (inicio_id != -1) {
                    inicio_id = inicio_id + 5;
                    while (inicio_id < usuario_json.length() && Character.isWhitespace(usuario_json.charAt(inicio_id))) {
                        inicio_id++;
                    }
                    int fin_id = inicio_id;
                    while (fin_id < usuario_json.length() && Character.isDigit(usuario_json.charAt(fin_id))) {
                        fin_id++;
                    }
                    if (fin_id > inicio_id) {
                        try {
                            return Long.parseLong(usuario_json.substring(inicio_id, fin_id).trim());
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
            
            indice = fin_usuario + 1;
        }
        
        return null;
    }
    
    // envia una peticion http al servidor
    private String enviar_peticion(String host, int puerto, String metodo, String ruta, 
                                  String token, String cuerpo) throws IOException {
        
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        
        try {
            socket = new Socket(host, puerto);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            StringBuilder peticion = new StringBuilder();
            peticion.append(metodo).append(" ").append(ruta).append(" HTTP/1.1\r\n");
            peticion.append("Host: ").append(host).append(":").append(puerto).append("\r\n");
            
            if (token != null) {
                peticion.append("Authorization: Bearer ").append(token).append("\r\n");
            }
            
            if (cuerpo != null && cuerpo.length() > 0) {
                peticion.append("Content-Type: application/json\r\n");
                peticion.append("Content-Length: ").append(cuerpo.getBytes().length).append("\r\n");
            }
            
            peticion.append("\r\n");
            
            if (cuerpo != null && cuerpo.length() > 0) {
                peticion.append(cuerpo);
            }
            
            out.print(peticion.toString());
            out.flush();
            
            StringBuilder respuesta = new StringBuilder();
            String linea;
            int longitud_contenido = 0;
            boolean leyendo_headers = true;
            
            while ((linea = in.readLine()) != null) {
                if (leyendo_headers) {
                    if (linea.length() == 0) {
                        leyendo_headers = false;
                        if (longitud_contenido > 0) {
                            char[] buffer = new char[longitud_contenido];
                            int leidos = in.read(buffer, 0, longitud_contenido);
                            if (leidos > 0) {
                                respuesta.append(new String(buffer, 0, leidos));
                            }
                        }
                        break;
                    } else if (linea.toLowerCase().startsWith("content-length:")) {
                        longitud_contenido = Integer.parseInt(linea.substring(15).trim());
                    }
                }
            }
            
            return respuesta.toString();
            
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
