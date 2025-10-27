package io.memorix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for multiple datasources.
 * 
 * <p>Binds to application.yml configuration:
 * <pre>{@code
 * memorix:
 *   datasources:
 *     default:
 *       url: jdbc:postgresql://localhost:5432/memorix
 *       username: postgres
 *       password: postgres
 *     documentation:
 *       url: jdbc:postgresql://localhost:5432/memorix_docs
 *       username: postgres
 *       password: postgres
 * }</pre>
 */
@ConfigurationProperties(prefix = "memorix")
public class DataSourceConfigProperties {
    
    private Map<String, DataSourceProperties> datasources = new HashMap<>();
    
    public Map<String, DataSourceProperties> getDatasources() {
        return datasources;
    }
    
    public void setDatasources(Map<String, DataSourceProperties> datasources) {
        this.datasources = datasources;
    }
    
    /**
     * Properties for a single datasource.
     */
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
        private HikariProperties hikari = new HikariProperties();
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getDriverClassName() {
            return driverClassName;
        }
        
        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
        
        public HikariProperties getHikari() {
            return hikari;
        }
        
        public void setHikari(HikariProperties hikari) {
            this.hikari = hikari;
        }
    }
    
    /**
     * Hikari connection pool properties.
     */
    public static class HikariProperties {
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }
        
        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
        
        public int getMinimumIdle() {
            return minimumIdle;
        }
        
        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
        
        public long getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public long getIdleTimeout() {
            return idleTimeout;
        }
        
        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }
        
        public long getMaxLifetime() {
            return maxLifetime;
        }
        
        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }
    }
}

