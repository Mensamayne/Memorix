package io.memorix.lifecycle;

import io.memorix.model.DecayContext;
import io.memorix.model.Memory;
import org.springframework.stereotype.Component;

/**
 * Hybrid decay strategy (usage + time).
 * 
 * <p>Key behavior:
 * <ul>
 *   <li>Usage is PRIMARY factor (70% weight by default)</li>
 *   <li>Time is SECONDARY factor (30% weight by default)</li>
 *   <li>Long inactivity adds gentle decay</li>
 *   <li>Recent usage overrides time</li>
 * </ul>
 * 
 * <p>Perfect for:
 * <ul>
 *   <li>User interests ("Plays Baldur's Gate")</li>
 *   <li>Hobbies and activities</li>
 *   <li>Context that changes over time</li>
 *   <li>Social connections</li>
 * </ul>
 * 
 * <p>Configuration params:
 * <ul>
 *   <li>timeFactor: Weight for time component (0.0-1.0, default 0.3)</li>
 *   <li>usageFactor: Weight for usage component (0.0-1.0, default 0.7)</li>
 *   <li>inactivityThreshold: Days before time decay kicks in (default 90)</li>
 * </ul>
 */
@Component
public class HybridDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        int current = memory.getDecay();
        
        // Component 1: USAGE (dominant)
        if (context.wasUsedInSession()) {
            // Strong reinforcement
            current += context.getDecayReinforcement();
        } else if (context.isActiveSession()) {
            // Gentle decay during activity
            current -= (context.getDecayReduction() / 2);
        }
        
        // Component 2: TIME (kicks in after inactivity)
        long daysSinceLastUse = context.getTimeSinceLastUse().toDays();
        int inactivityThreshold = ((Number) context.getStrategyParam(
            "inactivityThreshold", 90)).intValue();
        
        if (daysSinceLastUse > inactivityThreshold) {
            // User inactive for long time? Gentle time decay
            int timeDecay = ((Number) context.getStrategyParam(
                "timeDecay", 2)).intValue();
            current -= timeDecay;
        }
        
        // Component 3: IMPORTANCE (multiplier)
        if (memory.getImportance() > 0.8f) {
            // Bonus for important memories
            current += 1;
        }
        
        // Clamp to bounds
        return Math.max(context.getMinDecay(), 
                   Math.min(current, context.getMaxDecay()));
    }
    
    @Override
    public String getStrategyName() {
        return "HYBRID";
    }
}

