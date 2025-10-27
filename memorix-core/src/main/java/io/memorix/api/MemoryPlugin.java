package io.memorix.api;

import io.memorix.model.DecayConfig;
import io.memorix.model.DeduplicationConfig;
import io.memorix.model.QueryLimit;
import io.memorix.model.TableSchema;
import java.util.Map;

/**
 * Plugin interface for custom memory types.
 * 
 * <p>Allows defining custom memory types with:
 * <ul>
 *   <li>Custom decay configuration</li>
 *   <li>Default query limits</li>
 *   <li>Type identification</li>
 * </ul>
 * 
 * <p>Example implementation:
 * <pre>{@code
 * @Component
 * @MemoryType("USER_PREFERENCE")
 * public class UserPreferencePlugin implements MemoryPlugin {
 *     
 *     @Override
 *     public String getType() {
 *         return "USER_PREFERENCE";
 *     }
 *     
 *     @Override
 *     public DecayConfig getDecayConfig() {
 *         return DecayConfig.builder()
 *             .strategyClassName("io.memorix.lifecycle.UsageBasedDecayStrategy")
 *             .initialDecay(100)
 *             .decayReduction(3)
 *             .decayReinforcement(8)
 *             .build();
 *     }
 * }
 * }</pre>
 */
public interface MemoryPlugin {
    
    /**
     * Get plugin type identifier.
     * 
     * @return Type identifier (e.g., "USER_PREFERENCE", "RECIPE", "DOCUMENTATION")
     */
    String getType();
    
    /**
     * Get decay configuration for this memory type.
     * 
     * @return Decay configuration
     */
    DecayConfig getDecayConfig();
    
    /**
     * Get default query limits for this memory type.
     * 
     * <p>Can be overridden in individual queries.
     * 
     * @return Default query limit (optional)
     */
    default QueryLimit getDefaultQueryLimit() {
        return QueryLimit.builder()
            .maxCount(20)
            .maxTokens(500)
            .build();
    }
    
    /**
     * Get deduplication configuration for this memory type.
     * 
     * <p>Controls duplicate detection and handling strategy.
     * By default, deduplication is disabled.
     * 
     * @return Deduplication configuration
     */
    default DeduplicationConfig getDeduplicationConfig() {
        return DeduplicationConfig.disabled();
    }
    
    /**
     * Extract additional properties for this memory type.
     * 
     * <p>Used for custom fields in memory metadata.
     * 
     * @param memory Memory content
     * @return Additional properties map
     */
    default Map<String, Object> extractProperties(String memory) {
        return Map.of();
    }
    
    /**
     * Get datasource name for this plugin.
     * 
     * <p>Allows plugins to use separate databases for physical data isolation.
     * By default, uses "default" datasource.
     * 
     * <p>Example:
     * <pre>{@code
     * // DocumentationPlugin uses its own database
     * @Override
     * public String getDataSourceName() {
     *     return "documentation";
     * }
     * }</pre>
     * 
     * @return Datasource name (must be configured in application.yml)
     */
    default String getDataSourceName() {
        return "default";
    }
    
    /**
     * Get table schema for this plugin.
     * 
     * <p>Allows plugins to define custom table structure with additional columns and indexes.
     * By default, uses standard 'memories' table.
     * 
     * <p>Example:
     * <pre>{@code
     * @Override
     * public TableSchema getTableSchema() {
     *     return TableSchema.builder()
     *         .tableName("documentation_memories")
     *         .vectorDimension(1536)
     *         .addCustomColumn("category VARCHAR(100)")
     *         .addCustomIndex("CREATE INDEX idx_category ON documentation_memories(category)")
     *         .build();
     * }
     * }</pre>
     * 
     * @return Table schema configuration
     */
    default TableSchema getTableSchema() {
        return TableSchema.DEFAULT;
    }
}

