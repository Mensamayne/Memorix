package io.memorix.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Routes database connections to appropriate datasource based on plugin configuration.
 * 
 * <p>Uses {@link PluginDataSourceContext} to determine which datasource to use for current thread.
 * This allows different memory plugins to use physically separate databases.
 * 
 * <p>Configuration example:
 * <pre>{@code
 * memorix:
 *   datasources:
 *     default:
 *       url: jdbc:postgresql://localhost:5432/memorix
 *     documentation:
 *       url: jdbc:postgresql://localhost:5432/memorix_docs
 * }</pre>
 * 
 * <p>Plugin example:
 * <pre>{@code
 * @Component
 * @MemoryType("DOCUMENTATION")
 * public class DocumentationPlugin implements MemoryPlugin {
 *     @Override
 *     public String getDataSourceName() {
 *         return "documentation";  // Uses memorix_docs database
 *     }
 * }
 * }</pre>
 */
public class PluginDataSourceRouter extends AbstractRoutingDataSource {
    
    private static final Logger log = LoggerFactory.getLogger(PluginDataSourceRouter.class);
    
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceName = PluginDataSourceContext.getCurrentDataSource();
        log.trace("Routing to datasource: {}", dataSourceName);
        return dataSourceName;
    }
}

