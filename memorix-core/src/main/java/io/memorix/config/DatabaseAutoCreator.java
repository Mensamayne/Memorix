package io.memorix.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Automatically creates PostgreSQL database if it doesn't exist.
 * 
 * <p>This runs BEFORE Spring context initialization (using EnvironmentPostProcessor),
 * ensuring the database exists before any beans (including Flyway) are created.
 * 
 * <p><b>Usage in application:</b>
 * <pre>{@code
 * # In application.yml
 * spring:
 *   datasource:
 *     url: jdbc:postgresql://localhost:5432/my_database
 *     username: postgres
 *     password: postgres
 * 
 * memorix:
 *   auto-create-database: true  # Enable auto-creation
 * }</pre>
 * 
 * <p>The auto-creator will:
 * <ol>
 *   <li>Extract database name from spring.datasource.url</li>
 *   <li>Connect to PostgreSQL 'postgres' database</li>
 *   <li>Check if target database exists</li>
 *   <li>Create it if missing</li>
 * </ol>
 * 
 * @author Memorix Team
 */
public class DatabaseAutoCreator implements EnvironmentPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseAutoCreator.class);
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Check if auto-creation is enabled
        Boolean autoCreate = environment.getProperty("memorix.auto-create-database", Boolean.class, false);
        if (!autoCreate) {
            log.debug("Database auto-creation disabled (memorix.auto-create-database=false)");
            return;
        }
        
        // Get datasource configuration
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username", "postgres");
        String password = environment.getProperty("spring.datasource.password", "postgres");
        
        if (url == null || url.trim().isEmpty()) {
            log.debug("No spring.datasource.url found, skipping auto-creation");
            return;
        }
        
        // Only support PostgreSQL for now
        if (!url.startsWith("jdbc:postgresql://")) {
            log.debug("Database auto-creation only supports PostgreSQL (found: {})", url);
            return;
        }
        
        log.info("🔧 Memorix database auto-creation enabled");
        ensureDatabaseExists(url, username, password);
    }
    
    /**
     * Ensures the database exists, creates it if missing.
     */
    private void ensureDatabaseExists(String url, String username, String password) {
        try {
            // Extract database name from URL
            String databaseName = extractDatabaseName(url);
            
            if (databaseName == null) {
                log.warn("Cannot extract database name from URL: {}", url);
                return;
            }
            
            // Connect to PostgreSQL server (default 'postgres' database)
            String serverUrl = url.replace("/" + databaseName, "/postgres");
            
            log.info("🔍 Checking if database '{}' exists...", databaseName);
            
            try (HikariDataSource tempDs = createTempDataSource(serverUrl, username, password);
                 Connection conn = tempDs.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Check if database exists
                ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'");
                
                if (!rs.next()) {
                    // Database doesn't exist - create it
                    log.info("📦 Database '{}' does not exist, creating...", databaseName);
                    stmt.executeUpdate("CREATE DATABASE \"" + databaseName + "\"");
                    log.info("✅ Successfully created database '{}'", databaseName);
                } else {
                    log.debug("✓ Database '{}' already exists", databaseName);
                }
                
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to auto-create database: {}", e.getMessage());
            log.debug("Stack trace:", e);
            // Don't throw - let application attempt normal connection
            // If database truly doesn't exist, Flyway will report error
        }
    }
    
    /**
     * Creates a temporary datasource for database checking/creation.
     */
    private HikariDataSource createTempDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setPoolName("Memorix-DB-Creator");
        
        return new HikariDataSource(config);
    }
    
    /**
     * Extracts database name from JDBC URL.
     * 
     * @param url JDBC URL (e.g., jdbc:postgresql://localhost:5432/my_database)
     * @return Database name or null if not found
     */
    private String extractDatabaseName(String url) {
        try {
            // Parse jdbc:postgresql://host:port/database
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0) {
                String dbPart = url.substring(lastSlash + 1);
                // Remove query parameters if present
                int questionMark = dbPart.indexOf('?');
                if (questionMark > 0) {
                    return dbPart.substring(0, questionMark);
                }
                return dbPart;
            }
        } catch (Exception e) {
            log.debug("Failed to extract database name from URL: {}", url);
        }
        return null;
    }
}
