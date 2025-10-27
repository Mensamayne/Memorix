package io.memorix.service;

import io.memorix.api.MemoryPlugin;
import io.memorix.api.MemoryStore;
import io.memorix.deduplication.ContentHashGenerator;
import io.memorix.deduplication.DeduplicationDetector;
import io.memorix.deduplication.HashDeduplicationDetector;
import io.memorix.deduplication.HybridDeduplicationDetector;
import io.memorix.deduplication.SemanticDeduplicationDetector;
import io.memorix.embedding.EmbeddingProvider;
import io.memorix.exception.DuplicateMemoryException;
import io.memorix.lifecycle.LifecycleManager;
import io.memorix.model.DeduplicationConfig;
import io.memorix.model.Memory;
import io.memorix.model.QueryLimit;
import io.memorix.model.QueryResult;
import io.memorix.plugin.PluginRegistry;
import io.memorix.query.QueryExecutor;
import io.memorix.storage.PluginDataSourceContext;
import io.memorix.util.TokenCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * High-level memory service facade.
 * 
 * <p>Orchestrates all memory operations:
 * <ul>
 *   <li>Save with auto-embedding</li>
 *   <li>Semantic search</li>
 *   <li>Lifecycle management</li>
 *   <li>Plugin integration</li>
 * </ul>
 */
@Service
public class MemoryService {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    
    private final MemoryStore memoryStore;
    private final QueryExecutor queryExecutor;
    private final LifecycleManager lifecycleManager;
    private final PluginRegistry pluginRegistry;
    private final EmbeddingProvider embeddingProvider;
    private final TokenCounter tokenCounter;
    private final HashDeduplicationDetector hashDetector;
    private final SemanticDeduplicationDetector semanticDetector;
    private final HybridDeduplicationDetector hybridDetector;
    
    @Autowired
    public MemoryService(MemoryStore memoryStore,
                        QueryExecutor queryExecutor,
                        LifecycleManager lifecycleManager,
                        PluginRegistry pluginRegistry,
                        EmbeddingProvider embeddingProvider,
                        TokenCounter tokenCounter,
                        HashDeduplicationDetector hashDetector,
                        SemanticDeduplicationDetector semanticDetector,
                        HybridDeduplicationDetector hybridDetector) {
        this.memoryStore = memoryStore;
        this.queryExecutor = queryExecutor;
        this.lifecycleManager = lifecycleManager;
        this.pluginRegistry = pluginRegistry;
        this.embeddingProvider = embeddingProvider;
        this.tokenCounter = tokenCounter;
        this.hashDetector = hashDetector;
        this.semanticDetector = semanticDetector;
        this.hybridDetector = hybridDetector;
    }
    
    /**
     * Save memory with auto-embedding and token count.
     * 
     * @param userId User ID
     * @param content Memory content
     * @param pluginType Plugin type
     * @return Saved memory
     */
    public Memory save(String userId, String content, String pluginType) {
        return save(userId, content, pluginType, null, null);
    }
    
    /**
     * Save memory with auto-embedding, token count, and custom properties.
     * 
     * <p>Handles deduplication based on plugin configuration:
     * <ul>
     *   <li>REJECT - Throws exception if duplicate exists</li>
     *   <li>MERGE - Updates existing memory (reinforces decay, updates importance)</li>
     *   <li>UPDATE - Replaces existing memory completely</li>
     * </ul>
     * 
     * @param userId User ID
     * @param content Memory content
     * @param pluginType Plugin type
     * @param properties Custom properties
     * @return Saved or updated memory
     * @throws DuplicateMemoryException if duplicate found and strategy is REJECT
     */
    public Memory save(String userId, String content, String pluginType, 
                      Map<String, Object> properties) {
        return save(userId, content, pluginType, properties, null);
    }
    
    /**
     * Save memory with auto-embedding, token count, custom properties, and importance.
     * 
     * <p>Handles deduplication based on plugin configuration:
     * <ul>
     *   <li>REJECT - Throws exception if duplicate exists</li>
     *   <li>MERGE - Updates existing memory (reinforces decay, updates importance)</li>
     *   <li>UPDATE - Replaces existing memory completely</li>
     * </ul>
     * 
     * @param userId User ID
     * @param content Memory content
     * @param pluginType Plugin type
     * @param properties Custom properties
     * @param importance Importance value (0.0-1.0), if null uses default 0.5
     * @return Saved or updated memory
     * @throws DuplicateMemoryException if duplicate found and strategy is REJECT
     */
    public Memory save(String userId, String content, String pluginType, 
                      Map<String, Object> properties, Float importance) {
        
        // Get plugin configuration
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        DeduplicationConfig deduplicationConfig = plugin.getDeduplicationConfig();
        
        // Set datasource context for this plugin
        String dataSourceName = plugin.getDataSourceName();
        PluginDataSourceContext.setCurrentDataSource(dataSourceName);
        log.trace("Using datasource '{}' for plugin '{}'", dataSourceName, pluginType);
        
        try {
        
        // Check for duplicates if enabled
        if (deduplicationConfig.isEnabled()) {
            // Choose appropriate detector based on configuration
            DeduplicationDetector detector = selectDetector(deduplicationConfig);
            
            Optional<Memory> duplicate = detector.findDuplicate(
                userId, content, deduplicationConfig
            );
            
            if (duplicate.isPresent()) {
                Memory existing = duplicate.get();
                log.info("Duplicate detected: existingId={}, strategy={}, detector={}", 
                    existing.getId(), deduplicationConfig.getStrategy(), 
                    detector.getClass().getSimpleName());
                
                return handleDuplicate(existing, content, plugin, 
                    deduplicationConfig, properties, importance);
            }
        }
        
        // No duplicate - save as new memory
        float[] embedding = embeddingProvider.embed(content);
        int tokenCount = tokenCounter.count(content);
        
        // Generate content hash for deduplication
        String contentHash = ContentHashGenerator.generateHash(
            content, 
            deduplicationConfig.isNormalizeContent()
        );
        
        // Calculate initial decay based on importance
        // importance 0.0 → 0.5x decay (50)
        // importance 0.5 → 1.0x decay (100) - default
        // importance 1.0 → 1.5x decay (150)
        float effectiveImportance = importance != null ? importance : 0.5f;
        int baseDecay = plugin.getDecayConfig().getInitialDecay();
        int scaledDecay = (int) (baseDecay * (0.5 + effectiveImportance));
        
        Memory.Builder builder = Memory.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .content(content)
                .contentHash(contentHash)
                .embedding(embedding)
                .tokenCount(tokenCount)
                .decay(scaledDecay)
                .importance(effectiveImportance)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
        
        // Set metadata from properties
        Map<String, Object> metadata = new HashMap<>();
        if (properties != null && !properties.isEmpty()) {
            metadata.putAll(properties);
        }
        
        // Always add pluginType to metadata
        metadata.put("pluginType", pluginType);
        builder.metadata(metadata);
        
        Memory memory = builder.build();
        Memory saved = memoryStore.save(memory);
        
        log.info("Saved new memory: {} for user {} (type: {}, {} tokens)",
            saved.getId(), userId, pluginType, tokenCount);
        
        return saved;
        
        } finally {
            // Always clear context to prevent leaks
            PluginDataSourceContext.clear();
        }
    }
    
    /**
     * Handle duplicate memory based on strategy.
     * 
     * @param existing Existing memory
     * @param newContent New content
     * @param plugin Memory plugin
     * @param deduplicationConfig Deduplication configuration
     * @param properties Custom properties
     * @param importance New importance value (if provided)
     * @return Updated memory
     * @throws DuplicateMemoryException if strategy is REJECT
     */
    private Memory handleDuplicate(Memory existing, String newContent, 
                                   MemoryPlugin plugin, DeduplicationConfig deduplicationConfig,
                                   Map<String, Object> properties, Float importance) {
        switch (deduplicationConfig.getStrategy()) {
            case REJECT:
                DuplicateMemoryException rejectException = new DuplicateMemoryException(
                    "Duplicate memory detected. Similar content already exists for this user.",
                    existing
                );
                throw rejectException.withExistingMemoryContext();
                
            case MERGE:
                // Update timestamp
                existing.setUpdatedAt(LocalDateTime.now());
                
                // Update importance if provided
                if (importance != null) {
                    existing.setImportance(importance);
                }
                
                // Merge properties into metadata (don't overwrite existing)
                if (properties != null && !properties.isEmpty()) {
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        existing.addMetadata(entry.getKey(), entry.getValue());
                    }
                }
                
                // Ensure pluginType is always present in metadata
                existing.addMetadata("pluginType", plugin.getType());
                
                // Mark as merged for transparency
                existing.addMetadata("merged", true);
                existing.addMetadata("mergedAt", LocalDateTime.now().toString());
                
                // Optionally reinforce decay if configured
                int oldDecay = existing.getDecay();
                if (deduplicationConfig.isReinforceOnMerge()) {
                    int reinforcement = plugin.getDecayConfig().getDecayReinforcement();
                    int maxDecay = plugin.getDecayConfig().getMaxDecay();
                    int newDecay = Math.min(oldDecay + reinforcement, maxDecay);
                    existing.setDecay(newDecay);
                    
                    log.info("Merged duplicate: id={}, decay {} -> {} (+{}), importance={}", 
                        existing.getId(), oldDecay, newDecay, reinforcement, existing.getImportance());
                } else {
                    log.info("Merged duplicate: id={}, decay unchanged ({}), importance={}", 
                        existing.getId(), oldDecay, existing.getImportance());
                }
                
                return memoryStore.update(existing);
                
            case UPDATE:
                // Replace existing memory with new content
                int initialDecay = plugin.getDecayConfig().getInitialDecay();
                float[] newEmbedding = embeddingProvider.embed(newContent);
                int newTokenCount = tokenCounter.count(newContent);
                
                existing.setContent(newContent);
                existing.setEmbedding(newEmbedding);
                existing.setTokenCount(newTokenCount);
                existing.setDecay(initialDecay);
                existing.setUpdatedAt(LocalDateTime.now());
                
                // Update metadata from properties if provided
                if (properties != null && !properties.isEmpty()) {
                    existing.setMetadata(properties);  // Full replace for UPDATE strategy
                }
                
                // Ensure pluginType is always present in metadata
                existing.addMetadata("pluginType", plugin.getType());
                
                log.info("Updated duplicate: id={}, new content (length={}), decay reset to {}", 
                    existing.getId(), newContent.length(), initialDecay);
                
                return memoryStore.update(existing);
                
            default:
                throw new IllegalStateException("Unknown deduplication strategy: " + deduplicationConfig.getStrategy());
        }
    }
    
    /**
     * Select appropriate deduplication detector based on configuration.
     * 
     * <p>Selection logic:
     * <ul>
     *   <li>If semantic enabled: {@link HybridDeduplicationDetector} (hash + semantic)</li>
     *   <li>If semantic disabled: {@link HashDeduplicationDetector} (hash only)</li>
     * </ul>
     * 
     * @param config Deduplication configuration
     * @return Selected detector
     */
    private DeduplicationDetector selectDetector(DeduplicationConfig config) {
        if (config.isSemanticEnabled()) {
            return hybridDetector;
        } else {
            return hashDetector;
        }
    }
    
    /**
     * Search memories with semantic similarity.
     * 
     * @param userId User ID
     * @param query Search query text
     * @param pluginType Plugin type
     * @return Search results
     */
    public List<Memory> search(String userId, String query, String pluginType) {
        return searchWithMetadata(userId, query, pluginType).getMemories();
    }
    
    /**
     * Search memories with metadata.
     * 
     * @param userId User ID
     * @param query Search query text
     * @param pluginType Plugin type
     * @return Query result with metadata
     */
    public QueryResult searchWithMetadata(String userId, String query, String pluginType) {
        // Get plugin default limits
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        QueryLimit limit = plugin.getDefaultQueryLimit();
        
        // Set datasource context for this plugin
        String dataSourceName = plugin.getDataSourceName();
        PluginDataSourceContext.setCurrentDataSource(dataSourceName);
        
        try {
            return searchWithMetadata(userId, query, limit);
        } finally {
            PluginDataSourceContext.clear();
        }
    }
    
    /**
     * Search memories with custom limits.
     * 
     * @param userId User ID
     * @param query Search query text
     * @param limit Custom query limits
     * @return Query result with metadata
     */
    public QueryResult searchWithMetadata(String userId, String query, QueryLimit limit) {
        return searchWithMetadata(userId, query, limit, new HashMap<>());
    }
    
    /**
     * Search memories with custom limits and metadata filters.
     * 
     * @param userId User ID
     * @param query Search query text
     * @param limit Custom query limits
     * @param metadataFilters Metadata key-value filters (AND logic)
     * @return Query result with metadata
     */
    public QueryResult searchWithMetadata(String userId, String query, QueryLimit limit, 
                                         Map<String, Object> metadataFilters) {
        // Generate query embedding
        float[] queryVector = embeddingProvider.embed(query);
        
        // Execute search with metadata filtering
        return queryExecutor.executeQuery(userId, queryVector, limit, metadataFilters);
    }
    
    /**
     * Search memories by plugin type with metadata filters.
     * 
     * @param userId User ID
     * @param query Search query text
     * @param pluginType Plugin type
     * @param metadataFilters Metadata key-value filters (AND logic)
     * @return Query result with metadata
     */
    public QueryResult searchWithMetadata(String userId, String query, String pluginType,
                                         Map<String, Object> metadataFilters) {
        // Get plugin default limits
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        QueryLimit limit = plugin.getDefaultQueryLimit();
        
        // Set datasource context for this plugin
        String dataSourceName = plugin.getDataSourceName();
        PluginDataSourceContext.setCurrentDataSource(dataSourceName);
        
        try {
            return searchWithMetadata(userId, query, limit, metadataFilters);
        } finally {
            PluginDataSourceContext.clear();
        }
    }
    
    /**
     * Update existing memory with partial changes.
     * 
     * <p>Allows updating specific fields of a memory without affecting others.
     * If content is changed, embedding is automatically regenerated.
     * 
     * <p>Immutability protection: Memories marked as immutable (metadata "immutable=true")
     * cannot be updated and will throw {@link IllegalStateException}.
     * 
     * @param memoryId Memory ID to update
     * @param newContent New content (null = no change)
     * @param newImportance New importance (null = no change)
     * @param newMetadata New metadata (null = no change)
     * @return Updated memory
     * @throws IllegalArgumentException if memoryId is null or empty
     * @throws IllegalStateException if memory is immutable
     * @throws io.memorix.exception.StorageException if memory not found or update fails
     */
    public Memory update(String memoryId, String newContent, Float newImportance, 
                        Map<String, Object> newMetadata) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
        
        // Find existing memory
        Optional<Memory> existingOpt = memoryStore.findById(memoryId);
        if (existingOpt.isEmpty()) {
            throw new io.memorix.exception.StorageException(
                io.memorix.exception.ErrorCode.QUERY_FAILED,
                "Memory not found: " + memoryId
            );
        }
        
        Memory existing = existingOpt.get();
        
        // Check immutability
        checkImmutability(existing);
        
        // Track if content changed (for embedding regeneration)
        boolean contentChanged = false;
        
        // Update content if provided
        if (newContent != null && !newContent.trim().isEmpty()) {
            if (!newContent.equals(existing.getContent())) {
                existing.setContent(newContent);
                contentChanged = true;
                
                // Update content hash
                String newHash = ContentHashGenerator.generateHash(newContent, false);
                existing.setContentHash(newHash);
                
                // Update token count
                int tokenCount = tokenCounter.count(newContent);
                existing.setTokenCount(tokenCount);
                
                log.debug("Content changed for memory {}, will regenerate embedding", memoryId);
            }
        }
        
        // Update importance if provided
        if (newImportance != null) {
            if (newImportance < 0.0f || newImportance > 1.0f) {
                throw new IllegalArgumentException("Importance must be between 0.0 and 1.0");
            }
            existing.setImportance(newImportance);
            log.debug("Updated importance for memory {} to {}", memoryId, newImportance);
        }
        
        // Update metadata if provided (merge with existing to preserve system fields)
        if (newMetadata != null) {
            Map<String, Object> mergedMetadata = new HashMap<>(existing.getMetadata());
            mergedMetadata.putAll(newMetadata);
            existing.setMetadata(mergedMetadata);
            log.debug("Updated metadata for memory {} (merged with existing)", memoryId);
        }
        
        // Regenerate embedding if content changed
        if (contentChanged) {
            float[] newEmbedding = embeddingProvider.embed(existing.getContent());
            existing.setEmbedding(newEmbedding);
            log.debug("Regenerated embedding for memory {}", memoryId);
        }
        
        // Update timestamp
        existing.setUpdatedAt(LocalDateTime.now());
        
        // Persist changes
        Memory updated = memoryStore.update(existing);
        
        log.info("Updated memory: {} (contentChanged={}, importanceChanged={}, metadataChanged={})",
            memoryId, contentChanged, newImportance != null, newMetadata != null);
        
        return updated;
    }
    
    /**
     * Check if memory is immutable and throw exception if attempt to modify.
     * 
     * <p>Memories with metadata "immutable=true" cannot be updated.
     * This is used to protect user-provided facts from being overwritten by AI agents.
     * 
     * @param memory Memory to check
     * @throws IllegalStateException if memory is immutable
     */
    private void checkImmutability(Memory memory) {
        Object immutable = memory.getMetadata().get("immutable");
        if (immutable != null && Boolean.TRUE.equals(immutable)) {
            throw new IllegalStateException(
                "Cannot update immutable memory: " + memory.getId() + 
                ". Memory is marked as immutable (source: " + 
                memory.getMetadata().getOrDefault("source", "unknown") + ")"
            );
        }
    }
    
    /**
     * Delete a memory by ID.
     * 
     * @param memoryId Memory ID to delete
     * @throws IllegalArgumentException if memoryId is null or empty
     * @throws io.memorix.exception.StorageException if deletion fails
     */
    public void delete(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
        
        log.info("Deleting memory: {}", memoryId);
        memoryStore.delete(memoryId);
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
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        log.info("Deleting all memories for user: {}", userId);
        return memoryStore.deleteByUserId(userId);
    }
    
    /**
     * Apply decay to user's memories.
     * 
     * @param userId User ID
     * @param pluginType Plugin type
     * @param usedMemoryIds IDs of memories used in this session
     * @return Lifecycle result
     */
    public LifecycleManager.LifecycleResult applyDecay(String userId, 
                                                       String pluginType,
                                                       List<String> usedMemoryIds) {
        return lifecycleManager
                .forUser(userId)
                .withPluginType(pluginType)
                .markUsed(usedMemoryIds)
                .activeSession(true)
                .applyDecay()
                .cleanupExpired()
                .execute();
    }
    
    /**
     * Get memory statistics for user.
     * 
     * @param userId User ID
     * @return Statistics
     */
    public MemoryStats getStats(String userId) {
        List<Memory> memories = memoryStore.findByUserId(userId);
        long total = memories.size();
        
        if (total == 0) {
            return new MemoryStats(0, 0.0, 0, new HashMap<>(), 0.0, null, null);
        }
        
        // Calculate average decay
        double averageDecay = memories.stream()
            .mapToInt(Memory::getDecay)
            .average()
            .orElse(0.0);
        
        // Calculate total tokens
        int totalTokens = memories.stream()
            .mapToInt(Memory::getTokenCount)
            .sum();
        
        // Group by plugin type
        Map<String, Long> byPluginType = memories.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                m -> m.getMetadata().getOrDefault("pluginType", "UNKNOWN").toString(),
                java.util.stream.Collectors.counting()
            ));
        
        // Calculate average importance
        double averageImportance = memories.stream()
            .map(Memory::getImportance)
            .mapToDouble(Float::doubleValue)
            .average()
            .orElse(0.0);
        
        // Find oldest and newest memory
        String oldestMemory = memories.stream()
            .map(Memory::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .min(java.time.LocalDateTime::compareTo)
            .map(java.time.LocalDateTime::toString)
            .orElse(null);
        
        String newestMemory = memories.stream()
            .map(Memory::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .max(java.time.LocalDateTime::compareTo)
            .map(java.time.LocalDateTime::toString)
            .orElse(null);
        
        return new MemoryStats(total, averageDecay, totalTokens, byPluginType, 
            averageImportance, oldestMemory, newestMemory);
    }
    
    /**
     * Memory statistics.
     */
    public static class MemoryStats {
        private final long totalMemories;
        private final double averageDecay;
        private final int totalTokens;
        private final Map<String, Long> byPluginType;
        private final double averageImportance;
        private final String oldestMemory;
        private final String newestMemory;
        
        public MemoryStats(long totalMemories, double averageDecay, int totalTokens,
                          Map<String, Long> byPluginType, double averageImportance,
                          String oldestMemory, String newestMemory) {
            this.totalMemories = totalMemories;
            this.averageDecay = averageDecay;
            this.totalTokens = totalTokens;
            this.byPluginType = byPluginType;
            this.averageImportance = averageImportance;
            this.oldestMemory = oldestMemory;
            this.newestMemory = newestMemory;
        }
        
        public long getTotalMemories() {
            return totalMemories;
        }
        
        public double getAverageDecay() {
            return averageDecay;
        }
        
        public int getTotalTokens() {
            return totalTokens;
        }
        
        public Map<String, Long> getByPluginType() {
            return byPluginType;
        }
        
        public double getAverageImportance() {
            return averageImportance;
        }
        
        public String getOldestMemory() {
            return oldestMemory;
        }
        
        public String getNewestMemory() {
            return newestMemory;
        }
    }
}

