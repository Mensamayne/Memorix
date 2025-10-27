package io.memorix.lifecycle;

import io.memorix.model.DecayContext;
import io.memorix.model.Memory;

/**
 * Strategy interface for decay calculation.
 * 
 * <p>Defines how memories decay over time/usage.
 * Implementations include:
 * <ul>
 *   <li>UsageBasedDecayStrategy - Decay based on usage, freeze during inactivity</li>
 *   <li>TimeBasedDecayStrategy - Decay based on calendar time</li>
 *   <li>HybridDecayStrategy - Mix of usage and time</li>
 *   <li>PermanentDecayStrategy - No decay</li>
 * </ul>
 */
public interface DecayStrategy {
    
    /**
     * Calculate new decay value for a memory.
     * 
     * @param memory Current memory state
     * @param context Execution context (time, usage, config)
     * @return New decay value (clamped to [minDecay, maxDecay])
     */
    int calculateDecay(Memory memory, DecayContext context);
    
    /**
     * Should this memory be auto-deleted when decay reaches threshold?
     * 
     * @param memory Memory to check
     * @param context Execution context
     * @return true if should be deleted
     */
    default boolean shouldAutoDelete(Memory memory, DecayContext context) {
        return context.getDecayConfig() != null 
            && context.getDecayConfig().isAutoDelete()
            && memory.getDecay() <= context.getMinDecay();
    }
    
    /**
     * Get strategy name for logging/debugging.
     * 
     * @return Strategy name
     */
    String getStrategyName();
}

