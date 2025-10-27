package io.memorix.lifecycle;

import io.memorix.api.MemoryPlugin;
import io.memorix.api.MemoryStore;
import io.memorix.exception.ErrorCode;
import io.memorix.exception.MemorixException;
import io.memorix.model.DecayConfig;
import io.memorix.model.DecayContext;
import io.memorix.model.Memory;
import io.memorix.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Engine for applying decay to memories.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Apply decay strategy to memories</li>
 *   <li>Reinforce used memories</li>
 *   <li>Auto-delete expired memories</li>
 *   <li>Batch operations for efficiency</li>
 * </ul>
 */
@Component
public class DecayEngine {
    
    private static final Logger log = LoggerFactory.getLogger(DecayEngine.class);
    
    private final PluginRegistry pluginRegistry;
    private final MemoryStore memoryStore;
    private final ApplicationContext context;
    
    @Autowired
    public DecayEngine(PluginRegistry pluginRegistry, 
                      MemoryStore memoryStore,
                      ApplicationContext context) {
        this.pluginRegistry = pluginRegistry;
        this.memoryStore = memoryStore;
        this.context = context;
    }
    
    /**
     * Apply decay to a single memory.
     * 
     * @param memory Memory to apply decay to
     * @param pluginType Plugin type for this memory
     * @param decayContext Context for decay calculation
     * @return Updated memory
     */
    public Memory applyDecay(Memory memory, String pluginType, DecayContext decayContext) {
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        DecayConfig config = plugin.getDecayConfig();
        
        // Get strategy instance
        DecayStrategy strategy = getStrategy(config.getStrategyClassName());
        
        // Calculate new decay
        int newDecay = strategy.calculateDecay(memory, decayContext);
        
        // Update memory
        memory.setDecay(newDecay);
        Memory updated = memoryStore.update(memory);
        
        log.debug("Applied decay to memory {}: {} → {} (strategy: {})",
            memory.getId(), memory.getDecay(), newDecay, strategy.getStrategyName());
        
        return updated;
    }
    
    /**
     * Reinforce a memory (increase decay).
     * 
     * @param memory Memory to reinforce
     * @param pluginType Plugin type
     * @return Updated memory
     */
    public Memory reinforce(Memory memory, String pluginType) {
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        DecayConfig config = plugin.getDecayConfig();
        
        int newDecay = Math.min(
            memory.getDecay() + config.getDecayReinforcement(),
            config.getMaxDecay()
        );
        
        memory.setDecay(newDecay);
        Memory updated = memoryStore.update(memory);
        
        log.debug("Reinforced memory {}: {} → {}",
            memory.getId(), memory.getDecay(), newDecay);
        
        return updated;
    }
    
    /**
     * Check if memory should be deleted.
     * 
     * @param memory Memory to check
     * @param pluginType Plugin type
     * @param context Decay context
     * @return true if should be deleted
     */
    public boolean shouldDelete(Memory memory, String pluginType, DecayContext context) {
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        DecayConfig config = plugin.getDecayConfig();
        
        if (!config.isAutoDelete()) {
            return false;
        }
        
        DecayStrategy strategy = getStrategy(config.getStrategyClassName());
        return strategy.shouldAutoDelete(memory, context);
    }
    
    /**
     * Delete expired memories for a user.
     * 
     * @param userId User ID
     * @param pluginType Plugin type
     * @return Number of memories deleted
     */
    public int deleteExpired(String userId, String pluginType) {
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        DecayConfig config = plugin.getDecayConfig();
        
        if (!config.isAutoDelete()) {
            return 0;
        }
        
        // Find memories below threshold
        List<Memory> memories = memoryStore.findByUserIdAndDecayAbove(
            userId, config.getMinDecay() - 1);
        
        int deleted = 0;
        for (Memory memory : memories) {
            if (memory.getDecay() <= config.getMinDecay()) {
                memoryStore.delete(memory.getId());
                deleted++;
            }
        }
        
        if (deleted > 0) {
            log.info("Deleted {} expired memories for user {} (type: {})",
                deleted, userId, pluginType);
        }
        
        return deleted;
    }
    
    /**
     * Get strategy instance by class name.
     */
    private DecayStrategy getStrategy(String className) {
        try {
            // Try to get from Spring context first
            @SuppressWarnings("unchecked")
            Class<? extends DecayStrategy> strategyClass = 
                (Class<? extends DecayStrategy>) Class.forName(className);
            
            return context.getBean(strategyClass);
            
        } catch (ClassNotFoundException e) {
            throw new MemorixException(ErrorCode.INTERNAL_ERROR,
                "Decay strategy class not found: " + className)
                .withContext("className", className);
        } catch (Exception e) {
            throw new MemorixException(ErrorCode.INTERNAL_ERROR,
                "Failed to instantiate decay strategy: " + e.getMessage(), e)
                .withContext("className", className);
        }
    }
}

