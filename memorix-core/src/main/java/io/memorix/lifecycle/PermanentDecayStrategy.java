package io.memorix.lifecycle;

import io.memorix.model.DecayContext;
import io.memorix.model.Memory;
import org.springframework.stereotype.Component;

/**
 * Permanent decay strategy (no decay).
 * 
 * <p>Key behavior:
 * <ul>
 *   <li>Decay value never changes</li>
 *   <li>Auto-delete disabled</li>
 *   <li>Search ranking unaffected by decay</li>
 * </ul>
 * 
 * <p>Perfect for:
 * <ul>
 *   <li>Documentation</li>
 *   <li>API definitions</li>
 *   <li>System knowledge</li>
 *   <li>Permanent facts</li>
 * </ul>
 */
@Component
public class PermanentDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        // Never change
        return memory.getDecay();
    }
    
    @Override
    public boolean shouldAutoDelete(Memory memory, DecayContext context) {
        // Never delete
        return false;
    }
    
    @Override
    public String getStrategyName() {
        return "PERMANENT";
    }
}

