package io.memorix.lifecycle;

import io.memorix.api.MemoryStore;
import io.memorix.model.DecayContext;
import io.memorix.model.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level API for memory lifecycle management.
 * 
 * <p>Provides fluent API for:
 * <ul>
 *   <li>Applying decay</li>
 *   <li>Reinforcing memories</li>
 *   <li>Cleaning up expired memories</li>
 *   <li>Batch operations</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * lifecycleManager.forUser("user123")
 *     .withPluginType("USER_PREFERENCE")
 *     .markUsed(usedMemoryIds)
 *     .applyDecay()
 *     .cleanupExpired()
 *     .execute();
 * }</pre>
 */
@Component
public class LifecycleManager {
    
    private static final Logger log = LoggerFactory.getLogger(LifecycleManager.class);
    
    private final MemoryStore memoryStore;
    private final DecayEngine decayEngine;
    
    @Autowired
    public LifecycleManager(MemoryStore memoryStore, DecayEngine decayEngine) {
        this.memoryStore = memoryStore;
        this.decayEngine = decayEngine;
    }
    
    /**
     * Start lifecycle operation for a user.
     * 
     * @param userId User ID
     * @return Builder for fluent API
     */
    public LifecycleBuilder forUser(String userId) {
        return new LifecycleBuilder(userId);
    }
    
    /**
     * Fluent builder for lifecycle operations.
     */
    public class LifecycleBuilder {
        private final String userId;
        private String pluginType;
        private List<String> usedMemoryIds = new ArrayList<>();
        private DecayContext.Builder contextBuilder = DecayContext.builder();
        private boolean shouldApplyDecay = false;
        private boolean shouldCleanup = false;
        
        public LifecycleBuilder(String userId) {
            this.userId = userId;
        }
        
        /**
         * Set plugin type for memories.
         */
        public LifecycleBuilder withPluginType(String pluginType) {
            this.pluginType = pluginType;
            return this;
        }
        
        /**
         * Mark memories as used in this session.
         */
        public LifecycleBuilder markUsed(List<String> memoryIds) {
            this.usedMemoryIds = new ArrayList<>(memoryIds);
            return this;
        }
        
        /**
         * Set if this is an active session.
         */
        public LifecycleBuilder activeSession(boolean isActive) {
            this.contextBuilder.isActiveSession(isActive);
            return this;
        }
        
        /**
         * Apply decay to all memories.
         */
        public LifecycleBuilder applyDecay() {
            this.shouldApplyDecay = true;
            return this;
        }
        
        /**
         * Clean up expired memories.
         */
        public LifecycleBuilder cleanupExpired() {
            this.shouldCleanup = true;
            return this;
        }
        
        /**
         * Execute lifecycle operations.
         * 
         * @return Result of operations
         */
        public LifecycleResult execute() {
            if (pluginType == null || pluginType.trim().isEmpty()) {
                throw new IllegalArgumentException("Plugin type must be specified");
            }
            
            LifecycleResult result = new LifecycleResult();
            
            if (shouldApplyDecay) {
                result.decayApplied = applyDecayInternal();
            }
            
            if (shouldCleanup) {
                result.memoriesDeleted = decayEngine.deleteExpired(userId, pluginType);
            }
            
            return result;
        }
        
        private int applyDecayInternal() {
            List<Memory> memories = memoryStore.findByUserId(userId);
            int updated = 0;
            
            for (Memory memory : memories) {
                // Build context for this memory
                DecayContext context = contextBuilder
                    .wasUsedInSession(usedMemoryIds.contains(memory.getId()))
                    .build();
                
                decayEngine.applyDecay(memory, pluginType, context);
                updated++;
            }
            
            log.info("Applied decay to {} memories for user {}", updated, userId);
            return updated;
        }
    }
    
    /**
     * Result of lifecycle operations.
     */
    public static class LifecycleResult {
        private int decayApplied;
        private int memoriesDeleted;
        
        public int getDecayApplied() {
            return decayApplied;
        }
        
        public int getMemoriesDeleted() {
            return memoriesDeleted;
        }
    }
}

