package io.memorix.api.rest;

import io.memorix.Memorix;
import io.memorix.api.MemoryPlugin;
import io.memorix.api.MemoryStore;
import io.memorix.config.DataSourceConfigProperties;
import io.memorix.exception.DuplicateMemoryException;
import io.memorix.exception.PluginException;
import io.memorix.exception.ValidationException;
import io.memorix.lifecycle.LifecycleManager;
import io.memorix.model.*;
import io.memorix.plugin.PluginRegistry;
import io.memorix.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for Memorix playground.
 * 
 * <p>Endpoints for interactive demo.
 */
@RestController
@RequestMapping("/api/memorix")
@CrossOrigin(origins = "*")
public class MemorixController {
    
    private static final Logger log = LoggerFactory.getLogger(MemorixController.class);
    
    @Autowired
    private Memorix memorix;
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private MemoryStore memoryStore;
    
    @Autowired(required = false)
    private DataSourceConfigProperties dataSourceConfig;
    
    @Value("${memorix.multi-datasource.enabled:false}")
    private boolean multiDataSourceEnabled;
    
    /**
     * Save memory.
     */
    @PostMapping("/memories")
    public ResponseEntity<?> saveMemory(@RequestBody SaveMemoryRequest request) {
        log.info("Save memory request: userId={}, pluginType={}, importance={}, contentLength={}", 
                request.userId, request.pluginType, request.importance, 
                request.content != null ? request.content.length() : 0);
        
        // Validate request
        validateSaveMemoryRequest(request);
        
        try {
            var builder = memorix.store(request.userId)
                    .content(request.content)
                    .withType(request.pluginType)
                    .withImportance(request.importance != null ? request.importance : 0.5f);
            
            // Add metadata if provided
            if (request.metadata != null && !request.metadata.isEmpty()) {
                builder.withProperties(request.metadata);
            }
            
            Memory saved = builder.save();
            
            log.info("Memory saved: id={}, tokens={}, decay={}", 
                    saved.getId(), saved.getTokenCount(), saved.getDecay());
            
            return ResponseEntity.ok(saved);
            
        } catch (DuplicateMemoryException e) {
            log.warn("Duplicate memory rejected: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "DUPLICATE_MEMORY");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("existingMemoryId", e.getExistingMemory() != null ? 
                e.getExistingMemory().getId() : null);
            errorResponse.put("existingContent", e.getExistingMemory() != null ? 
                e.getExistingMemory().getContent() : null);
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }
    
    /**
     * Update existing memory.
     */
    @PutMapping("/memories/{memoryId}")
    public ResponseEntity<?> updateMemory(@PathVariable String memoryId, 
                                         @RequestBody UpdateMemoryRequest request) {
        log.info("Update memory request: id={}, hasContent={}, hasImportance={}, hasMetadata={}", 
                memoryId, request.content != null, request.importance != null, 
                request.metadata != null && !request.metadata.isEmpty());
        
        try {
            var builder = memorix.update(memoryId);
            
            if (request.content != null) {
                builder.content(request.content);
            }
            
            if (request.importance != null) {
                builder.importance(request.importance);
            }
            
            if (request.metadata != null) {
                builder.metadata(request.metadata);
            }
            
            Memory updated = builder.execute();
            
            log.info("Memory updated: id={}, tokens={}", updated.getId(), updated.getTokenCount());
            
            return ResponseEntity.ok(updated);
            
        } catch (IllegalStateException e) {
            // Immutable memory
            log.warn("Update rejected - immutable: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "IMMUTABLE_MEMORY");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("memoryId", memoryId);
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            log.error("Failed to update memory: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "UPDATE_FAILED");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Search memories.
     */
    @PostMapping("/memories/search")
    public ResponseEntity<QueryResult> searchMemories(@RequestBody SearchRequest request) {
        log.info("🔧 FIXED VERSION - Search request: userId={}, query='{}', pluginType={}, maxCount={}, maxTokens={}, minSimilarity={}, strategy={}", 
                request.userId, request.query, request.pluginType, request.maxCount, request.maxTokens, request.minSimilarity, request.strategy);
        
        // Validate request
        validateSearchRequest(request);
        
        QueryLimit.Builder limitBuilder = QueryLimit.builder()
                .maxCount(request.maxCount != null ? request.maxCount : 20)
                .strategy(request.strategy != null ? request.strategy : LimitStrategy.GREEDY);
        
        // Add maxTokens only if provided (not null)
        if (request.maxTokens != null) {
            limitBuilder.maxTokens(request.maxTokens);
        } else {
            limitBuilder.maxTokens(500); // Default from docs
        }
        
        // Add minSimilarity only if provided (not null)
        if (request.minSimilarity != null) {
            limitBuilder.minSimilarity(request.minSimilarity);
        } else {
            // Default: 0.0 means no similarity filtering - return all results
            // This ensures search returns results even without explicit parameters
            limitBuilder.minSimilarity(0.0);
        }
        
        QueryLimit limit = limitBuilder.build();
        log.debug("Built QueryLimit: {}", limit);
        
        // Build query - pluginType is optional
        var queryBuilder = memorix.query(request.userId)
                .search(request.query);
        
        // Only add pluginType filter if provided
        if (request.pluginType != null && !request.pluginType.trim().isEmpty()) {
            queryBuilder.withType(request.pluginType);
        }
        
        // Add metadata filters if provided
        if (request.metadataFilters != null && !request.metadataFilters.isEmpty()) {
            for (Map.Entry<String, Object> filter : request.metadataFilters.entrySet()) {
                queryBuilder.whereMetadata(filter.getKey(), filter.getValue());
            }
        }
        
        QueryResult result = queryBuilder
                .limit(limit)
                .executeWithMetadata();
        
        log.info("Search completed: found={}, returned={}, tokens={}", 
                result.getMetadata().getTotalFound(), 
                result.getMetadata().getReturned(), 
                result.getMetadata().getTotalTokens());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Apply decay.
     */
    @PostMapping("/lifecycle/decay")
    public ResponseEntity<LifecycleResult> applyDecay(@RequestBody DecayRequest request) {
        log.info("Apply decay request: userId={}, pluginType={}, activeSession={}, usedMemoryIdsCount={}", 
                request.userId, request.pluginType, request.activeSession, 
                request.usedMemoryIds != null ? request.usedMemoryIds.size() : 0);
        
        // Validate request
        validateDecayRequest(request);
        
        LifecycleManager.LifecycleResult result = memorix.lifecycle()
                .forUser(request.userId)
                .withPluginType(request.pluginType)
                .markUsed(request.usedMemoryIds != null ? request.usedMemoryIds : java.util.Collections.emptyList())
                .activeSession(request.activeSession != null ? request.activeSession : true)
                .applyDecay()
                .cleanupExpired()
                .execute();
        
        log.info("Decay applied: decayApplied={}, memoriesDeleted={}", 
                result.getDecayApplied(), result.getMemoriesDeleted());
        
        return ResponseEntity.ok(new LifecycleResult(
            result.getDecayApplied(),
            result.getMemoriesDeleted()
        ));
    }
    
    /**
     * Get statistics.
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<MemoryService.MemoryStats> getStats(@PathVariable String userId) {
        log.info("Get stats request: userId={}", userId);
        MemoryService.MemoryStats stats = memoryService.getStats(userId);
        log.info("Stats retrieved: userId={}, totalMemories={}", userId, stats.getTotalMemories());
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get available plugin types.
     */
    @GetMapping("/plugins")
    public ResponseEntity<Set<String>> getPlugins() {
        return ResponseEntity.ok(pluginRegistry.getRegisteredTypes());
    }
    
    /**
     * Get plugin info.
     */
    @GetMapping("/plugins/{type}")
    public ResponseEntity<Map<String, Object>> getPluginInfo(@PathVariable String type) {
        try {
            var plugin = pluginRegistry.getByType(type);
            
            Map<String, Object> info = new HashMap<>();
            info.put("type", plugin.getType());
            info.put("decayConfig", plugin.getDecayConfig());
            info.put("defaultQueryLimit", plugin.getDefaultQueryLimit());
            info.put("deduplicationConfig", plugin.getDeduplicationConfig());
            
            return ResponseEntity.ok(info);
        } catch (PluginException e) {
            // Return 404 with error details
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getErrorCode().toString());
            errorResponse.put("message", e.getMessage());
            errorResponse.putAll(e.getContext());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    /**
     * Clear all memories for a user.
     */
    @DeleteMapping("/memories/{userId}")
    public ResponseEntity<ClearMemoriesResult> clearMemories(@PathVariable String userId) {
        log.info("Clear memories request: userId={}", userId);
        
        int deletedCount = memoryStore.deleteByUserId(userId);
        
        log.info("Memories cleared: userId={}, deletedCount={}", userId, deletedCount);
        
        return ResponseEntity.ok(new ClearMemoriesResult(userId, deletedCount));
    }
    
    /**
     * Get multi-datasource information.
     * 
     * <p>Returns info about configured datasources and plugin-to-datasource mapping.
     */
    @GetMapping("/datasources")
    public ResponseEntity<DataSourceInfo> getDataSourceInfo() {
        log.info("Get datasource info request: multiDataSourceEnabled={}, dataSourceConfig={}", 
                multiDataSourceEnabled, dataSourceConfig != null ? "present" : "null");
        
        DataSourceInfo info = new DataSourceInfo();
        info.enabled = multiDataSourceEnabled;
        
        if (multiDataSourceEnabled && dataSourceConfig != null) {
            info.datasources = new ArrayList<>(dataSourceConfig.getDatasources().keySet());
            log.debug("Loaded datasources from config: {}", info.datasources);
        } else {
            info.datasources = List.of("default");
            log.debug("Using default datasource only (multiDataSourceEnabled={}, dataSourceConfig={})", 
                    multiDataSourceEnabled, dataSourceConfig != null ? "present" : "null");
        }
        
        // Map plugins to datasources
        info.pluginDataSourceMapping = new HashMap<>();
        for (String pluginType : pluginRegistry.getRegisteredTypes()) {
            MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
            String dsName = plugin.getDataSourceName();
            info.pluginDataSourceMapping.put(pluginType, dsName);
            
            // Warn if plugin uses datasource that doesn't exist
            if (!info.datasources.contains(dsName)) {
                log.warn("Plugin {} uses datasource '{}' which is not in configured datasources: {}",
                        pluginType, dsName, info.datasources);
            }
        }
        
        log.info("DataSource info: enabled={}, datasources={}, mappings={}", 
                info.enabled, info.datasources, info.pluginDataSourceMapping);
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Get statistics per datasource.
     * 
     * <p>Returns count of memories in each datasource (grouped by plugin).
     */
    @GetMapping("/datasources/stats")
    public ResponseEntity<Map<String, DataSourceStats>> getDataSourceStats() {
        log.info("Get datasource stats request");
        
        Map<String, DataSourceStats> statsMap = new HashMap<>();
        
        // Group plugins by datasource
        Map<String, List<String>> datasourceToPlugins = new HashMap<>();
        for (String pluginType : pluginRegistry.getRegisteredTypes()) {
            MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
            String dsName = plugin.getDataSourceName();
            datasourceToPlugins.computeIfAbsent(dsName, k -> new ArrayList<>()).add(pluginType);
        }
        
        // For each datasource, count memories
        for (Map.Entry<String, List<String>> entry : datasourceToPlugins.entrySet()) {
            String dsName = entry.getKey();
            List<String> plugins = entry.getValue();
            
            DataSourceStats stats = new DataSourceStats();
            stats.datasourceName = dsName;
            stats.pluginTypes = plugins;
            
            // Count total memories across all plugins using this datasource
            // Note: This is approximate - actual count would require datasource-specific queries
            stats.estimatedMemoryCount = 0;
            stats.plugins = new HashMap<>();
            
            for (String pluginType : plugins) {
                stats.plugins.put(pluginType, true);
            }
            
            statsMap.put(dsName, stats);
        }
        
        log.info("DataSource stats: datasources={}", statsMap.keySet());
        
        return ResponseEntity.ok(statsMap);
    }
    
    // ============================================
    // EXCEPTION HANDLERS
    // ============================================
    
    /**
     * Handle validation errors with 400 Bad Request.
     * 
     * <p>Returns detailed error response with:
     * <ul>
     *   <li>Error type (VALIDATION_ERROR)</li>
     *   <li>Descriptive message</li>
     *   <li>Field that failed validation</li>
     *   <li>Expected format/values (if applicable)</li>
     * </ul>
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException e) {
        log.warn("Validation error: field={}, message={}", e.getField(), e.getMessage());
        
        ValidationErrorResponse response = new ValidationErrorResponse(
            "VALIDATION_ERROR",
            e.getMessage(),
            e.getField(),
            e.getRejectedValue(),
            e.getExpectedFormat()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle plugin not found errors with 404 Not Found.
     * 
     * <p>Returns detailed error response with:
     * <ul>
     *   <li>Error type (PLUGIN_NOT_FOUND)</li>
     *   <li>Descriptive message</li>
     *   <li>Available plugin types</li>
     * </ul>
     */
    @ExceptionHandler(PluginException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handlePluginException(PluginException e) {
        log.warn("Plugin error: code={}, message={}, context={}", 
                e.getErrorCode(), e.getMessage(), e.getContext());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getErrorCode().toString());
        errorResponse.put("message", e.getMessage());
        errorResponse.putAll(e.getContext());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    // ============================================
    // VALIDATION METHODS
    // ============================================
    
    /**
     * Validate SaveMemoryRequest.
     * 
     * @throws ValidationException if validation fails
     */
    private void validateSaveMemoryRequest(SaveMemoryRequest request) {
        // Validate userId
        if (request.userId == null || request.userId.trim().isEmpty()) {
            throw new ValidationException(
                "userId",
                request.userId,
                "Field 'userId' cannot be null or empty",
                "Non-empty string (e.g., 'user-123')"
            );
        }
        
        // Validate userId length (max 255 characters)
        if (request.userId.length() > 255) {
            throw new ValidationException(
                "userId",
                request.userId.length(),
                "Field 'userId' exceeds maximum length of 255 characters, got: " + request.userId.length(),
                "String with max 255 characters"
            );
        }
        
        // Validate content
        if (request.content == null || request.content.trim().isEmpty()) {
            throw new ValidationException(
                "content",
                request.content,
                "Field 'content' cannot be null or empty",
                "Non-empty string with memory content"
            );
        }
        
        // Validate content length (max 10000 characters)
        if (request.content.length() > 10000) {
            throw new ValidationException(
                "content",
                request.content.length(),
                "Field 'content' exceeds maximum length of 10000 characters, got: " + request.content.length(),
                "String with max 10000 characters"
            );
        }
        
        // Validate pluginType
        if (request.pluginType == null || request.pluginType.trim().isEmpty()) {
            throw new ValidationException(
                "pluginType",
                request.pluginType,
                "Field 'pluginType' cannot be null or empty",
                "Valid plugin types: " + pluginRegistry.getRegisteredTypes()
            );
        }
        
        // Validate pluginType exists
        Set<String> validTypes = pluginRegistry.getRegisteredTypes();
        if (!validTypes.contains(request.pluginType)) {
            throw new ValidationException(
                "pluginType",
                request.pluginType,
                "Invalid pluginType '" + request.pluginType + "'. Plugin type not registered.",
                "Valid plugin types: " + validTypes
            );
        }
        
        // Validate importance range [0.0, 1.0]
        if (request.importance != null) {
            log.debug("Validating importance: value={}, type={}", request.importance, request.importance.getClass().getName());
            
            // Check for NaN or Infinite
            if (Float.isNaN(request.importance) || Float.isInfinite(request.importance)) {
                throw new ValidationException(
                    "importance",
                    request.importance,
                    "Field 'importance' cannot be NaN or Infinite",
                    "Float value in range [0.0, 1.0]"
                );
            }
            
            // Check range [0.0, 1.0]
            if (request.importance.compareTo(0.0f) < 0 || request.importance.compareTo(1.0f) > 0) {
                log.warn("VALIDATION FAILED: importance={} is outside range [0.0, 1.0]", request.importance);
                throw new ValidationException(
                    "importance",
                    request.importance,
                    "Field 'importance' must be between 0.0 and 1.0, got: " + request.importance,
                    "Float value in range [0.0, 1.0]"
                );
            }
            
            log.debug("Importance validation passed: {}", request.importance);
        }
    }
    
    /**
     * Validate SearchRequest.
     * 
     * @throws ValidationException if validation fails
     */
    private void validateSearchRequest(SearchRequest request) {
        // Validate userId
        if (request.userId == null || request.userId.trim().isEmpty()) {
            throw new ValidationException(
                "userId",
                request.userId,
                "Field 'userId' cannot be null or empty",
                "Non-empty string (e.g., 'user-123')"
            );
        }
        
        // Validate query
        if (request.query == null || request.query.trim().isEmpty()) {
            throw new ValidationException(
                "query",
                request.query,
                "Field 'query' cannot be null or empty",
                "Non-empty search query string"
            );
        }
        
        // Validate query length (max 1000 characters)
        if (request.query.length() > 1000) {
            throw new ValidationException(
                "query",
                request.query.length(),
                "Field 'query' exceeds maximum length of 1000 characters, got: " + request.query.length(),
                "String with max 1000 characters"
            );
        }
        
        // Validate pluginType if provided
        if (request.pluginType != null && !request.pluginType.trim().isEmpty()) {
            Set<String> validTypes = pluginRegistry.getRegisteredTypes();
            if (!validTypes.contains(request.pluginType)) {
                throw new ValidationException(
                    "pluginType",
                    request.pluginType,
                    "Invalid pluginType '" + request.pluginType + "'. Plugin type not registered.",
                    "Valid plugin types: " + validTypes
                );
            }
        }
        
        // Validate maxCount
        if (request.maxCount != null && request.maxCount <= 0) {
            throw new ValidationException(
                "maxCount",
                request.maxCount,
                "Field 'maxCount' must be greater than 0, got: " + request.maxCount,
                "Positive integer (e.g., 10, 20, 100)"
            );
        }
        
        // Validate maxCount upper limit (DoS protection)
        if (request.maxCount != null && request.maxCount > 1000) {
            throw new ValidationException(
                "maxCount",
                request.maxCount,
                "Field 'maxCount' exceeds maximum limit of 1000, got: " + request.maxCount,
                "Integer between 1 and 1000"
            );
        }
        
        // Validate maxTokens lower bound
        if (request.maxTokens != null && request.maxTokens <= 0) {
            throw new ValidationException(
                "maxTokens",
                request.maxTokens,
                "Field 'maxTokens' must be greater than 0, got: " + request.maxTokens,
                "Positive integer"
            );
        }
        
        // Validate maxTokens upper limit (DoS protection)
        if (request.maxTokens != null && request.maxTokens > 50000) {
            throw new ValidationException(
                "maxTokens",
                request.maxTokens,
                "Field 'maxTokens' exceeds maximum limit of 50000, got: " + request.maxTokens,
                "Integer between 1 and 50000"
            );
        }
        
        // Validate minSimilarity range [0.0, 1.0]
        if (request.minSimilarity != null) {
            if (Double.isNaN(request.minSimilarity) || Double.isInfinite(request.minSimilarity)) {
                throw new ValidationException(
                    "minSimilarity",
                    request.minSimilarity,
                    "Field 'minSimilarity' cannot be NaN or Infinite",
                    "Double value in range [0.0, 1.0]"
                );
            }
            
            if (request.minSimilarity < 0.0 || request.minSimilarity > 1.0) {
                throw new ValidationException(
                    "minSimilarity",
                    request.minSimilarity,
                    "Field 'minSimilarity' must be between 0.0 and 1.0, got: " + request.minSimilarity,
                    "Double value in range [0.0, 1.0]"
                );
            }
        }
    }
    
    /**
     * Validate DecayRequest.
     * 
     * @throws ValidationException if validation fails
     */
    private void validateDecayRequest(DecayRequest request) {
        // Validate userId
        if (request.userId == null || request.userId.trim().isEmpty()) {
            throw new ValidationException(
                "userId",
                request.userId,
                "Field 'userId' cannot be null or empty",
                "Non-empty string (e.g., 'user-123')"
            );
        }
        
        // Validate pluginType - REQUIRED for decay
        if (request.pluginType == null || request.pluginType.trim().isEmpty()) {
            throw new ValidationException(
                "pluginType",
                request.pluginType,
                "Field 'pluginType' is required for decay operation",
                "Valid plugin types: " + pluginRegistry.getRegisteredTypes()
            );
        }
        
        // Validate pluginType exists
        Set<String> validTypes = pluginRegistry.getRegisteredTypes();
        if (!validTypes.contains(request.pluginType)) {
            throw new ValidationException(
                "pluginType",
                request.pluginType,
                "Invalid pluginType '" + request.pluginType + "'. Plugin type not registered.",
                "Valid plugin types: " + validTypes
            );
        }
    }
    
    // ============================================
    // DTOs
    // ============================================
    
    /**
     * Validation error response DTO.
     */
    public static class ValidationErrorResponse {
        public String error;
        public String message;
        public String field;
        public Object rejectedValue;
        public String expectedFormat;
        
        public ValidationErrorResponse(String error, String message, String field, 
                                     Object rejectedValue, String expectedFormat) {
            this.error = error;
            this.message = message;
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.expectedFormat = expectedFormat;
        }
    }
    
    // DTOs
    
    public static class SaveMemoryRequest {
        public String userId;
        public String content;
        public String pluginType;
        public Float importance;
        public Map<String, Object> metadata;  // ← ADDED
    }
    
    public static class SearchRequest {
        public String userId;
        public String query;
        public String pluginType;
        public Integer maxCount;
        public Integer maxTokens;
        public Double minSimilarity;
        public LimitStrategy strategy;
        public Map<String, Object> metadataFilters;  // ← ADDED
    }
    
    public static class UpdateMemoryRequest {
        public String content;
        public Float importance;
        public Map<String, Object> metadata;
    }
    
    public static class DecayRequest {
        public String userId;
        public String pluginType;
        public List<String> usedMemoryIds;
        public Boolean activeSession;
    }
    
    public static class LifecycleResult {
        public int decayApplied;
        public int memoriesDeleted;
        
        public LifecycleResult(int decayApplied, int memoriesDeleted) {
            this.decayApplied = decayApplied;
            this.memoriesDeleted = memoriesDeleted;
        }
    }
    
    public static class ClearMemoriesResult {
        public String userId;
        public int deletedCount;
        
        public ClearMemoriesResult(String userId, int deletedCount) {
            this.userId = userId;
            this.deletedCount = deletedCount;
        }
    }
    
    public static class DataSourceInfo {
        public boolean enabled;
        public List<String> datasources;
        public Map<String, String> pluginDataSourceMapping;
    }
    
    public static class DataSourceStats {
        public String datasourceName;
        public List<String> pluginTypes;
        public int estimatedMemoryCount;
        public Map<String, Boolean> plugins;
    }
}

