package io.memorix.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Runs Flyway migrations for all configured datasources.
 * 
 * <p>When multi-datasource is enabled, this component ensures
 * that database migrations are applied to ALL configured databases,
 * not just the default one.
 * 
 * <p>Migration execution:
 * <ul>
 *   <li>Waits for application to be fully started (ApplicationReadyEvent)</li>
 *   <li>Iterates through all configured datasources</li>
 *   <li>Runs Flyway migrations for each datasource</li>
 *   <li>Uses same migration scripts for all databases (db/migration)</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "memorix.multi-datasource.enabled", havingValue = "true")
public class MultiDataSourceFlywayConfig implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(MultiDataSourceFlywayConfig.class);
    
    @Autowired(required = false)
    private DataSourceConfigProperties configProperties;
    
    @Autowired
    private FlywayProperties flywayProperties;
    
    private boolean migrationsExecuted = false;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (migrationsExecuted) {
            return; // Already executed
        }
        
        if (configProperties == null || configProperties.getDatasources().isEmpty()) {
            log.warn("Multi-datasource enabled but no datasources configured - skipping migrations");
            return;
        }
        
        log.info("Running Flyway migrations for {} datasources", 
            configProperties.getDatasources().size());
        
        // Run migrations for each datasource
        for (Map.Entry<String, DataSourceConfigProperties.DataSourceProperties> entry : 
                configProperties.getDatasources().entrySet()) {
            
            String name = entry.getKey();
            
            // Skip default - already migrated by Spring Boot auto-configuration
            if ("default".equals(name)) {
                log.debug("Skipping 'default' datasource - already migrated by Spring Boot");
                continue;
            }
            
            runMigrations(name, entry.getValue());
        }
        
        migrationsExecuted = true;
        log.info("Completed Flyway migrations for all datasources");
    }
    
    /**
     * Run Flyway migrations for a specific datasource.
     * 
     * @param name Datasource name
     * @param properties Datasource properties
     */
    private void runMigrations(String name, DataSourceConfigProperties.DataSourceProperties properties) {
        try {
            log.info("Running migrations for datasource '{}'...", name);
            
            // Create temporary datasource for migrations
            Map<Object, Object> dataSources = DataSourceFactory.createDataSources(
                Map.of(name, properties)
            );
            DataSource dataSource = (DataSource) dataSources.get(name);
            
            if (dataSource == null) {
                log.warn("Datasource '{}' was not created - skipping migrations", name);
                return;
            }
            
            // Configure Flyway
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(flywayProperties.getLocations().toArray(new String[0]))
                    .baselineOnMigrate(true)
                    .baselineVersion(flywayProperties.getBaselineVersion())
                    .load();
            
            // Run migrations
            int applied = flyway.migrate().migrationsExecuted;
            
            log.info("Applied {} migration(s) to datasource '{}'", applied, name);
            
        } catch (Exception e) {
            log.error("Failed to run migrations for datasource '{}': {}", name, e.getMessage());
            
            // Failsafe: Skip failed migrations for non-default datasources
            if (!"default".equals(name)) {
                log.warn("Skipping failed migrations for datasource '{}' - continuing with available datasources", name);
                return;
            } else {
                // Default datasource migrations are critical
                throw new IllegalStateException("Migration failed for critical datasource: " + name, e);
            }
        }
    }
}

