package io.memorix.plugin.examples;

import io.memorix.api.MemoryPlugin;
import io.memorix.model.DecayConfig;
import io.memorix.model.DeduplicationConfig;
import io.memorix.model.DeduplicationStrategy;
import io.memorix.model.LimitStrategy;
import io.memorix.model.QueryLimit;
import io.memorix.model.TableSchema;
import io.memorix.plugin.MemoryType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example plugin for documentation and knowledge base.
 * 
 * <p>Use case: Store permanent documentation that never expires.
 * <ul>
 *   <li>Strategy: Permanent (no decay)</li>
 *   <li>Decay: Disabled</li>
 *   <li>Auto-delete: Disabled</li>
 * </ul>
 * 
 * <p>Examples:
 * <ul>
 *   <li>"API endpoint /users returns user list"</li>
 *   <li>"PostgreSQL connection string format"</li>
 *   <li>"How to configure email service"</li>
 * </ul>
 */
@Component
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin {
    
    @Override
    public String getType() {
        return "DOCUMENTATION";
    }
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
                .strategyClassName("io.memorix.lifecycle.PermanentDecayStrategy")
                .initialDecay(100)
                .minDecay(100)              // Never goes below 100
                .maxDecay(100)              // Never changes
                .decayReduction(0)          // No decay
                .decayReinforcement(0)      // No reinforcement needed
                .autoDelete(false)          // Never delete
                .affectsSearchRanking(false) // All docs equal in search
                .build();
    }
    
    @Override
    public QueryLimit getDefaultQueryLimit() {
        return QueryLimit.builder()
                .maxCount(10)
                .maxTokens(1000)            // Docs can be longer
                .minSimilarity(0.6)         // Higher precision
                .strategy(LimitStrategy.GREEDY)
                .build();
    }
    
    @Override
    public DeduplicationConfig getDeduplicationConfig() {
        return DeduplicationConfig.builder()
                .enabled(true)
                .strategy(DeduplicationStrategy.REJECT)  // Documentation must be unique
                .normalizeContent(true)
                .semanticEnabled(true)                   // Enable semantic detection
                .semanticThreshold(0.92)                 // Higher threshold (stricter)
                .reinforceOnMerge(false)                 // N/A for REJECT, but explicit
                .build();
        // REJECT strategy = throw exception, so reinforceOnMerge doesn't apply
        // But we set it to false to be explicit about intent
    }
    
    @Override
    public Map<String, Object> extractProperties(String memory) {
        return Map.of(
            "category", "documentation",
            "permanent", true,
            "searchable", true
        );
    }
    
    /**
     * Use separate datasource for documentation.
     * 
     * <p>Documentation memories are stored in physically separate database
     * for better isolation and independent scaling.
     * 
     * <p>Behavior:
     * <ul>
     *   <li>If multi-datasource enabled + 'documentation' configured: Uses separate DB</li>
     *   <li>Otherwise: Falls back to 'default' datasource</li>
     * </ul>
     * 
     * <p>To enable separate database:
     * <pre>{@code
     * # application.yml
     * memorix:
     *   multi-datasource:
     *     enabled: true
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
     * <p><b>NOTE:</b> Currently uses 'default' datasource.
     * Change to "documentation" only if you have configured separate datasource in application.yml
     */
    @Override
    public String getDataSourceName() {
        // Use 'documentation' datasource for physical separation
        // Falls back to 'default' if not configured
        return "documentation";
    }
    
    /**
     * Use custom table schema with additional columns.
     * 
     * <p>Documentation memories can have additional metadata like version, category, etc.
     */
    @Override
    public TableSchema getTableSchema() {
        // For now, use default schema
        // In future, can be extended with custom columns
        return TableSchema.builder()
                .tableName("memories")  // Uses default table
                .vectorDimension(1536)
                // Future: add custom columns
                // .addCustomColumn("doc_version VARCHAR(50)")
                // .addCustomColumn("doc_category VARCHAR(100)")
                // .addCustomIndex("CREATE INDEX idx_doc_category ON memories(doc_category)")
                .build();
    }
}

