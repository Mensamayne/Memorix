package io.memorix.lifecycle;

import io.memorix.model.DecayContext;
import io.memorix.model.Memory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Time-based decay strategy.
 * 
 * <p>Key behavior:
 * <ul>
 *   <li>Decay by real time elapsed</li>
 *   <li>User's activity doesn't matter</li>
 *   <li>Gradual, predictable degradation</li>
 *   <li>Auto-cleanup after expiration</li>
 * </ul>
 * 
 * <p>Perfect for:
 * <ul>
 *   <li>News articles ("Bitcoin hits 40K")</li>
 *   <li>Event announcements ("Conference on June 15")</li>
 *   <li>Time-sensitive data</li>
 *   <li>Prices, statuses</li>
 * </ul>
 */
@Component
public class TimeBasedDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        Duration age = context.getTimeSinceCreated();
        Duration interval = context.getDecayInterval();
        
        // How many intervals have passed?
        long intervalsElapsed = age.toMillis() / interval.toMillis();
        
        // Calculate total decay reduction
        int totalReduction = (int) (intervalsElapsed * context.getDecayReduction());
        
        // Return initial - reduction (clamped to min)
        return Math.max(
            context.getInitialDecay() - totalReduction,
            context.getMinDecay()
        );
    }
    
    @Override
    public String getStrategyName() {
        return "TIME_BASED";
    }
}

