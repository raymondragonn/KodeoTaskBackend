package com.kodeotask.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * configuración de conexión a MySQL usando JDBC puro
 */
public class DatabaseConfig {
    
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3306;
    private static final String DATABASE = "kodeotask";
    private static final String USER = "root";
    private static final String PASSWORD = "password";
    
    private static final String URL = String.format(
        "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        HOST, PORT, DATABASE
    );
    
    private static Connection connection = null;
    
    /**
     * obtiene una conexión a la base de datos
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✓ Conexión a MySQL establecida");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL no encontrado: " + e.getMessage());
            }
        }
        return connection;
    }
    
    /**
     * crea una nueva conexión para uso en threads separados
     */
    public static Connection createNewConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado: " + e.getMessage());
        }
    }
    
    /**
     * cierra la conexión principal
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("✓ Conexión a MySQL cerrada");
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }
    
    /**
     * inicializa las tablas de la base de datos
     */
    public static void initializeTables() throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                email VARCHAR(100) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                first_name VARCHAR(50),
                last_name VARCHAR(50),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;
        stmt.executeUpdate(createUsersTable);
        System.out.println("✓ Tabla 'users' verificada/creada");
        
        String createTasksTable = """
            CREATE TABLE IF NOT EXISTS tasks (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                description TEXT,
                status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'PENDING',
                category VARCHAR(50),
                created_by BIGINT NOT NULL,
                assigned_to BIGINT,
                due_date DATETIME,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                completed_at TIMESTAMP NULL,
                FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
            )
            """;
        stmt.executeUpdate(createTasksTable);
        System.out.println("✓ Tabla 'tasks' verificada/creada");
        
        stmt.close();
        System.out.println("✓ Base de datos inicializada correctamente");
    }
    
    /**
     * verifica la conexión a la base de datos
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }
}

