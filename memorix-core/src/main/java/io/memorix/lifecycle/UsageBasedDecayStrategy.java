package io.memorix.lifecycle;

import io.memorix.model.DecayContext;
import io.memorix.model.Memory;
import org.springframework.stereotype.Component;

/**
 * Usage-based decay strategy.
 * 
 * <p>Key behavior:
 * <ul>
 *   <li>Decay ONLY during active sessions</li>
 *   <li>User's break from app = freeze decay</li>
 *   <li>Used memories get reinforced</li>
 *   <li>Unused memories (during active sessions) decay</li>
 * </ul>
 * 
 * <p>Perfect for:
 * <ul>
 *   <li>User preferences ("Likes pizza")</li>
 *   <li>Permanent facts ("Lives in Warsaw")</li>
 *   <li>User profile data</li>
 *   <li>Long-lasting knowledge</li>
 * </ul>
 */
@Component
public class UsageBasedDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        int current = memory.getDecay();
        
        // Was this memory used in current session?
        if (context.wasUsedInSession()) {
            // REINFORCE: Memory was accessed!
            return Math.min(
                current + context.getDecayReinforcement(), 
                context.getMaxDecay()
            );
        }
        
        // Is this an active session (user using app)?
        if (context.isActiveSession()) {
            // DECAY: Memory not used during active session
            return Math.max(
                current - context.getDecayReduction(), 
                context.getMinDecay()
            );
        }
        
        // User not using app = FREEZE (no change)
        return current;
    }
    
    @Override
    public String getStrategyName() {
        return "USAGE_BASED";
    }
}

