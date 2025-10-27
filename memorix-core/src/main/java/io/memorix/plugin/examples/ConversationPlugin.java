package io.memorix.plugin.examples;

import io.memorix.api.MemoryPlugin;
import io.memorix.model.DecayConfig;
import io.memorix.model.DeduplicationConfig;
import io.memorix.model.LimitStrategy;
import io.memorix.model.QueryLimit;
import io.memorix.plugin.MemoryType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Example plugin for conversation context.
 * 
 * <p>Use case: Recent conversation history with time-based expiration.
 * <ul>
 *   <li>Strategy: Hybrid (usage + time)</li>
 *   <li>Decay: Both usage and time-based</li>
 *   <li>Auto-delete: Enabled after 30 days</li>
 * </ul>
 * 
 * <p>Examples:
 * <ul>
 *   <li>"User asked about pizza recipes yesterday"</li>
 *   <li>"Discussed vacation plans last week"</li>
 *   <li>"Mentioned being vegetarian"</li>
 * </ul>
 */
@Component
@MemoryType("CONVERSATION")
public class ConversationPlugin implements MemoryPlugin {
    
    @Override
    public String getType() {
        return "CONVERSATION";
    }
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
                .strategyClassName("io.memorix.lifecycle.HybridDecayStrategy")
                .initialDecay(100)
                .minDecay(0)
                .maxDecay(150)
                .decayReduction(4)
                .decayReinforcement(6)
                .decayInterval(Duration.ofDays(7))  // Weekly time decay
                .autoDelete(true)
                .affectsSearchRanking(true)
                .strategyParam("timeFactor", 0.3)         // 30% time weight
                .strategyParam("usageFactor", 0.7)        // 70% usage weight
                .strategyParam("inactivityThreshold", 30) // Days
                .build();
    }
    
    @Override
    public QueryLimit getDefaultQueryLimit() {
        return QueryLimit.builder()
                .maxCount(30)               // More context for conversations
                .maxTokens(800)
                .minSimilarity(0.4)         // Cast wider net
                .strategy(LimitStrategy.GREEDY)
                .build();
    }
    
    @Override
    public DeduplicationConfig getDeduplicationConfig() {
        // Disabled - conversations can repeat naturally
        return DeduplicationConfig.disabled();
    }
    
    @Override
    public Map<String, Object> extractProperties(String memory) {
        return Map.of(
            "category", "conversation",
            "context", true,
            "temporal", true
        );
    }
}

