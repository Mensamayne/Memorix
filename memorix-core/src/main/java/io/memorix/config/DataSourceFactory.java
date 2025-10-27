package io.memorix.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating datasources from configuration.
 * 
 * <p>Builds HikariCP datasources with proper connection pooling
 * and performance settings.
 */
public class DataSourceFactory {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceFactory.class);
    
    /**
     * Create multiple datasources from configuration properties.
     * 
     * @param properties Datasource configuration properties
     * @return Map of datasource name to DataSource instance
     */
    public static Map<Object, Object> createDataSources(
            Map<String, DataSourceConfigProperties.DataSourceProperties> properties) {
        
        Map<Object, Object> dataSources = new HashMap<>();
        
        for (Map.Entry<String, DataSourceConfigProperties.DataSourceProperties> entry : properties.entrySet()) {
            String name = entry.getKey();
            DataSourceConfigProperties.DataSourceProperties props = entry.getValue();
            
            try {
                DataSource dataSource = createDataSource(name, props);
                dataSources.put(name, dataSource);
                log.info("Created datasource '{}': {}", name, props.getUrl());
            } catch (Exception e) {
                log.error("Failed to create datasource '{}': {}", name, e.getMessage());
                
                // Failsafe: If non-default datasource fails, skip it and continue
                if (!"default".equals(name)) {
                    log.warn("Skipping failed datasource '{}' - continuing with available datasources", name);
                    continue;
                } else {
                    // Default datasource is critical - fail fast
                    throw new IllegalStateException("Failed to create critical default datasource: " + name, e);
                }
            }
        }
        
        return dataSources;
    }
    
    /**
     * Create a single HikariCP datasource.
     * 
     * @param name Datasource name
     * @param props Datasource properties
     * @return Configured HikariDataSource
     */
    private static HikariDataSource createDataSource(
            String name,
            DataSourceConfigProperties.DataSourceProperties props) {
        
        HikariConfig config = new HikariConfig();
        
        // Basic connection properties
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setDriverClassName(props.getDriverClassName());
        
        // Pool name for debugging
        config.setPoolName("Memorix-" + name);
        
        // Connection pool settings
        DataSourceConfigProperties.HikariProperties hikari = props.getHikari();
        config.setMaximumPoolSize(hikari.getMaximumPoolSize());
        config.setMinimumIdle(hikari.getMinimumIdle());
        config.setConnectionTimeout(hikari.getConnectionTimeout());
        config.setIdleTimeout(hikari.getIdleTimeout());
        config.setMaxLifetime(hikari.getMaxLifetime());
        
        // Performance optimizations
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        
        // Leak detection in development
        config.setLeakDetectionThreshold(60000); // 60 seconds
        
        log.debug("Creating HikariCP pool for '{}': pool={}, idle={}", 
            name, hikari.getMaximumPoolSize(), hikari.getMinimumIdle());
        
        // Auto-create database if it doesn't exist
        ensureDatabaseExists(props);
        
        return new HikariDataSource(config);
    }

    /**
     * Ensure database exists, create if it doesn't.
     * 
     * @param props Datasource properties
     */
    private static void ensureDatabaseExists(DataSourceConfigProperties.DataSourceProperties props) {
        try {
            // Extract database name from URL
            String url = props.getUrl();
            String databaseName = extractDatabaseName(url);
            
            if (databaseName == null || "default".equals(databaseName)) {
                return; // Skip auto-creation for default database
            }

            // Create connection to PostgreSQL server (not specific database)
            String serverUrl = url.replace("/" + databaseName, "/postgres");
            
            log.info("Checking if database '{}' exists...", databaseName);
            
            try (HikariDataSource tempDs = new HikariDataSource()) {
                tempDs.setJdbcUrl(serverUrl);
                tempDs.setUsername(props.getUsername());
                tempDs.setPassword(props.getPassword());
                tempDs.setMaximumPoolSize(1);
                tempDs.setMinimumIdle(1);
                tempDs.setConnectionTimeout(5000);
                
                try (var conn = tempDs.getConnection();
                     var stmt = conn.createStatement()) {
                    
                    // Check if database exists
                    var rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'");
                    
                    if (!rs.next()) {
                        // Database doesn't exist, create it
                        log.info("Database '{}' does not exist, creating...", databaseName);
                        stmt.executeUpdate("CREATE DATABASE \"" + databaseName + "\"");
                        log.info("Successfully created database '{}'", databaseName);
                    } else {
                        log.debug("Database '{}' already exists", databaseName);
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to auto-create database: {}", e.getMessage());
            // Don't throw exception - let the connection attempt handle it
        }
    }

    /**
     * Extract database name from JDBC URL.
     * 
     * @param url JDBC URL
     * @return Database name or null if not found
     */
    private static String extractDatabaseName(String url) {
        try {
            // Parse jdbc:postgresql://host:port/database
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0) {
                String dbPart = url.substring(lastSlash + 1);
                // Remove query parameters if any
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

