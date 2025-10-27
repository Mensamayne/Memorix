package io.memorix;

import io.memorix.lifecycle.LifecycleManager;
import io.memorix.model.*;
import io.memorix.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for Memorix framework.
 * 
 * <p>Provides fluent API for all memory operations.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Save
 * Memory saved = memorix
 *     .store("user123")
 *     .content("User loves pizza")
 *     .withType("USER_PREFERENCE")
 *     .withImportance(0.9f)
 *     .save();
 * 
 * // Search
 * List<Memory> results = memorix
 *     .query("user123")
 *     .search("What does user like?")
 *     .withType("USER_PREFERENCE")
 *     .limit(20)
 *     .execute();
 * 
 * // Update
 * Memory updated = memorix
 *     .update("memory-id-123")
 *     .content("Updated content")
 *     .importance(0.9f)
 *     .execute();
 * 
 * // Lifecycle
 * memorix
 *     .lifecycle()
 *     .forUser("user123")
 *     .markUsed(usedIds)
 *     .applyDecay()
 *     .cleanupExpired()
 *     .execute();
 * }</pre>
 */
@Component
public class Memorix {
    
    private final MemoryService memoryService;
    private final LifecycleManager lifecycleManager;
    
    @Autowired
    public Memorix(MemoryService memoryService, LifecycleManager lifecycleManager) {
        this.memoryService = memoryService;
        this.lifecycleManager = lifecycleManager;
    }
    
    /**
     * Start store operation.
     * 
     * @param userId User ID
     * @return Store builder
     */
    public StoreBuilder store(String userId) {
        return new StoreBuilder(userId);
    }
    
    /**
     * Start query operation.
     * 
     * @param userId User ID
     * @return Query builder
     */
    public QueryBuilder query(String userId) {
        return new QueryBuilder(userId);
    }
    
    /**
     * Start update operation.
     * 
     * @param memoryId Memory ID to update
     * @return Update builder
     */
    public UpdateBuilder update(String memoryId) {
        return new UpdateBuilder(memoryId);
    }
    
    /**
     * Delete a memory by ID.
     * 
     * @param memoryId Memory ID to delete
     * @throws IllegalArgumentException if memoryId is null or empty
     * @throws io.memorix.exception.StorageException if deletion fails
     */
    public void delete(String memoryId) {
        memoryService.delete(memoryId);
    }
    
    /**
     * Delete all memories for a user.
     * 
     * @param userId User ID
     * @return Number of memories deleted
     * @throws IllegalArgumentException if userId is null or empty
     * @throws io.memorix.exception.StorageException if deletion fails
     */
    public int deleteByUserId(String userId) {
        return memoryService.deleteByUserId(userId);
    }
    
    /**
     * Access lifecycle manager directly.
     * 
     * @return Lifecycle manager
     */
    public LifecycleManager lifecycle() {
        return lifecycleManager;
    }
    
    /**
     * Get memory statistics.
     * 
     * @param userId User ID
     * @return Statistics
     */
    public MemoryService.MemoryStats stats(String userId) {
        return memoryService.getStats(userId);
    }
    
    /**
     * Fluent builder for storing memories.
     */
    public class StoreBuilder {
        private final String userId;
        private String content;
        private String pluginType;
        private Float importance;
        private Map<String, Object> properties;
        
        public StoreBuilder(String userId) {
            this.userId = userId;
        }
        
        public StoreBuilder content(String content) {
            this.content = content;
            return this;
        }
        
        public StoreBuilder withType(String pluginType) {
            this.pluginType = pluginType;
            return this;
        }
        
        public StoreBuilder withImportance(float importance) {
            this.importance = importance;
            return this;
        }
        
        public StoreBuilder withProperties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }
        
        /**
         * Add single metadata key-value pair.
         * Multiple calls accumulate metadata.
         * 
         * @param key Metadata key
         * @param value Metadata value
         * @return this builder for chaining
         */
        public StoreBuilder withMetadata(String key, Object value) {
            if (this.properties == null) {
                this.properties = new HashMap<>();
            }
            this.properties.put(key, value);
            return this;
        }
        
        public Memory save() {
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Content cannot be null or empty");
            }
            if (pluginType == null || pluginType.trim().isEmpty()) {
                throw new IllegalArgumentException("Plugin type must be specified");
            }
            
            // Pass importance to service so it gets persisted to database
            return memoryService.save(userId, content, pluginType, properties, importance);
        }
    }
    
    /**
     * Fluent builder for querying memories.
     */
    public class QueryBuilder {
        private final String userId;
        private String queryText;
        private String pluginType;
        private QueryLimit limit;
        private Map<String, Object> metadataFilters;
        
        public QueryBuilder(String userId) {
            this.userId = userId;
            this.metadataFilters = new HashMap<>();
        }
        
        public QueryBuilder search(String query) {
            this.queryText = query;
            return this;
        }
        
        public QueryBuilder withType(String pluginType) {
            this.pluginType = pluginType;
            return this;
        }
        
        public QueryBuilder limit(int maxCount) {
            this.limit = QueryLimit.builder()
                    .maxCount(maxCount)
                    .build();
            return this;
        }
        
        public QueryBuilder limit(QueryLimit limit) {
            this.limit = limit;
            return this;
        }
        
        /**
         * Filter results by metadata key-value pair.
         * Multiple calls add additional filters (AND logic).
         * 
         * @param key Metadata key
         * @param value Metadata value
         * @return this builder for chaining
         */
        public QueryBuilder whereMetadata(String key, Object value) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Metadata key cannot be null or empty");
            }
            this.metadataFilters.put(key, value);
            return this;
        }
        
        public List<Memory> execute() {
            return executeWithMetadata().getMemories();
        }
        
        public QueryResult executeWithMetadata() {
            if (queryText == null || queryText.trim().isEmpty()) {
                throw new IllegalArgumentException("Query cannot be null or empty");
            }
            
            // Add pluginType to metadata filters if specified
            Map<String, Object> finalFilters = new HashMap<>(metadataFilters);
            if (pluginType != null && !pluginType.trim().isEmpty()) {
                finalFilters.put("pluginType", pluginType);
            }
            
            // When limit is provided, use it
            if (limit != null) {
                return memoryService.searchWithMetadata(userId, queryText, limit, finalFilters);
            }
            
            // When limit is null AND pluginType is provided, use plugin defaults
            if (pluginType != null && !pluginType.trim().isEmpty()) {
                return memoryService.searchWithMetadata(userId, queryText, pluginType, finalFilters);
            }
            
            // When both limit and pluginType are null, use system defaults
            // Create default limit for searching all types
            QueryLimit defaultLimit = QueryLimit.builder()
                .maxCount(20)
                .maxTokens(500)
                .minSimilarity(0.0)
                .strategy(LimitStrategy.GREEDY)
                .build();
            
            return memoryService.searchWithMetadata(userId, queryText, defaultLimit, finalFilters);
        }
    }
    
    /**
     * Fluent builder for updating memories.
     * 
     * <p>Allows partial updates of existing memories. Only specified fields are updated.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Memory updated = memorix
     *     .update("memory-id-123")
     *     .content("Updated content")
     *     .importance(0.9f)
     *     .execute();
     * }</pre>
     * 
     * <p>Immutability protection: Memories with metadata "immutable=true" cannot be updated.
     * Attempting to update immutable memories will throw {@link IllegalStateException}.
     */
    public class UpdateBuilder {
        private final String memoryId;
        private String content;
        private Float importance;
        private Map<String, Object> metadata;
        
        public UpdateBuilder(String memoryId) {
            if (memoryId == null || memoryId.trim().isEmpty()) {
                throw new IllegalArgumentException("Memory ID cannot be null or empty");
            }
            this.memoryId = memoryId;
        }
        
        /**
         * Update memory content.
         * 
         * <p>If content is changed, embedding will be automatically regenerated.
         * 
         * @param content New content
         * @return This builder
         */
        public UpdateBuilder content(String content) {
            this.content = content;
            return this;
        }
        
        /**
         * Update memory importance.
         * 
         * @param importance New importance (0.0-1.0)
         * @return This builder
         * @throws IllegalArgumentException if importance is out of range
         */
        public UpdateBuilder importance(float importance) {
            if (importance < 0.0f || importance > 1.0f) {
                throw new IllegalArgumentException("Importance must be between 0.0 and 1.0");
            }
            this.importance = importance;
            return this;
        }
        
        /**
         * Update memory metadata.
         * 
         * <p>Replaces entire metadata map. To add/modify single keys, retrieve memory first.
         * 
         * @param metadata New metadata
         * @return This builder
         */
        public UpdateBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        /**
         * Execute the update operation.
         * 
         * @return Updated memory
         * @throws IllegalArgumentException if memory ID is invalid
         * @throws IllegalStateException if memory is immutable
         * @throws io.memorix.exception.StorageException if memory not found or update fails
         */
        public Memory execute() {
            return memoryService.update(memoryId, content, importance, metadata);
        }
    }
}

