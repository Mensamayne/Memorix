package io.memorix.storage;

/**
 * Thread-local context for tracking current plugin datasource.
 * 
 * <p>Used by {@link PluginDataSourceRouter} to route database connections
 * to the correct datasource based on plugin configuration.
 * 
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     PluginDataSourceContext.setCurrentDataSource("documentation");
 *     // All database operations here use 'documentation' datasource
 *     storageService.save(memory);
 * } finally {
 *     PluginDataSourceContext.clear();
 * }
 * }</pre>
 */
public class PluginDataSourceContext {
    
    private static final ThreadLocal<String> currentDataSource = new ThreadLocal<>();
    
    /**
     * Set current datasource for this thread.
     * 
     * @param dataSourceName Datasource name
     */
    public static void setCurrentDataSource(String dataSourceName) {
        currentDataSource.set(dataSourceName);
    }
    
    /**
     * Get current datasource for this thread.
     * 
     * @return Datasource name, or "default" if not set
     */
    public static String getCurrentDataSource() {
        String ds = currentDataSource.get();
        return ds != null ? ds : "default";
    }
    
    /**
     * Clear datasource context for this thread.
     * 
     * <p>Should be called in finally block to prevent memory leaks.
     */
    public static void clear() {
        currentDataSource.remove();
    }
}

