package io.memorix.config;

import io.memorix.storage.PluginDataSourceRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Configuration for multiple datasources per plugin.
 * 
 * <p>Allows different memory plugins to use separate physical databases.
 * 
 * <p>Configuration example in application.yml:
 * <pre>{@code
 * memorix:
 *   multi-datasource:
 *     enabled: true  # Enable multi-datasource support
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
 * 
 * <p>Multi-datasource routing is OPTIONAL:
 * <ul>
 *   <li>If enabled=true: Creates PluginDataSourceRouter with multiple databases</li>
 *   <li>If enabled=false or not set: Uses Spring Boot auto-configured DataSource (single DB)</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(DataSourceConfigProperties.class)
public class MultiDataSourceConfig {
    
    private static final Logger log = LoggerFactory.getLogger(MultiDataSourceConfig.class);
    
    @Autowired(required = false)
    private DataSourceConfigProperties configProperties;
    
    /**
     * Creates routing datasource when multi-datasource is explicitly enabled.
     * 
     * <p>Requires:
     * <ul>
     *   <li>memorix.multi-datasource.enabled=true in application.yml</li>
     *   <li>At least 'default' datasource configured in memorix.datasources</li>
     * </ul>
     * 
     * @return Routing datasource that switches between configured databases
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "memorix.multi-datasource.enabled", havingValue = "true")
    public DataSource multiDataSource() {
        log.info("Multi-datasource support ENABLED");
        
        if (configProperties == null || configProperties.getDatasources().isEmpty()) {
            throw new IllegalStateException(
                "Multi-datasource enabled but no datasources configured! " +
                "Please configure memorix.datasources in application.yml"
            );
        }
        
        // Build datasources from configuration
        Map<Object, Object> targetDataSources = DataSourceFactory.createDataSources(
            configProperties.getDatasources()
        );
        
        if (!targetDataSources.containsKey("default")) {
            throw new IllegalStateException(
                "Multi-datasource requires 'default' datasource to be configured"
            );
        }
        
        // Create and configure router
        PluginDataSourceRouter router = new PluginDataSourceRouter();
        router.setTargetDataSources(targetDataSources);
        router.setDefaultTargetDataSource(targetDataSources.get("default"));
        router.afterPropertiesSet();
        
        log.info("Configured {} datasources: {}", 
            targetDataSources.size(), targetDataSources.keySet());
        
        return router;
    }
    
    /**
     * Transaction manager for multi-datasource routing.
     * 
     * <p>Only created when multi-datasource is enabled.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "memorix.multi-datasource.enabled", havingValue = "true")
    public PlatformTransactionManager multiDataSourceTransactionManager(DataSource dataSource) {
        log.info("Configuring transaction manager for multi-datasource");
        return new DataSourceTransactionManager(dataSource);
    }
}

