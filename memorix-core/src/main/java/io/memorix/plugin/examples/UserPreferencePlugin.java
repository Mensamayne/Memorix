package io.memorix.plugin.examples;

import io.memorix.api.MemoryPlugin;
import io.memorix.model.DecayConfig;
import io.memorix.model.DeduplicationConfig;
import io.memorix.model.DeduplicationStrategy;
import io.memorix.model.LimitStrategy;
import io.memorix.model.QueryLimit;
import io.memorix.plugin.MemoryType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example plugin for user preferences.
 * 
 * <p>Use case: Store user preferences that persist even if user takes break from app.
 * <ul>
 *   <li>Strategy: Usage-Based (freeze during inactivity)</li>
 *   <li>Decay: Slow decay, strong reinforcement</li>
 *   <li>Auto-delete: Enabled</li>
 * </ul>
 * 
 * <p>Examples:
 * <ul>
 *   <li>"User prefers dark mode"</li>
 *   <li>"User likes pizza"</li>
 *   <li>"User speaks Polish"</li>
 * </ul>
 */
@Component
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin {
    
    @Override
    public String getType() {
        return "USER_PREFERENCE";
    }
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
                .strategyClassName("io.memorix.lifecycle.UsageBasedDecayStrategy")
                .initialDecay(100)
                .minDecay(0)
                .maxDecay(200)              // Higher max for important preferences
                .decayReduction(3)          // Slow decay
                .decayReinforcement(8)      // Strong reinforcement
                .autoDelete(true)
                .affectsSearchRanking(true)  // More used = higher in results
                .build();
    }
    
    @Override
    public QueryLimit getDefaultQueryLimit() {
        return QueryLimit.builder()
                .maxCount(20)
                .maxTokens(400)
                .minSimilarity(0.5)
                .strategy(LimitStrategy.GREEDY)
                .build();
    }
    
    @Override
    public DeduplicationConfig getDeduplicationConfig() {
        return DeduplicationConfig.builder()
                .enabled(true)
                .strategy(DeduplicationStrategy.MERGE)  // Reinforce existing preferences
                .normalizeContent(true)                 // "loves pizza" = "Loves pizza"
                .semanticEnabled(true)                  // Enable semantic detection
                .semanticThreshold(0.88)                // 88% similarity = duplicate
                .reinforceOnMerge(true)                 // Duplicate = signal of importance!
                .build();
        // Examples:
        // "User loves pizza" saved twice → decay 100 -> 108 (reinforced!)
        // "Prefers dark mode" = "Likes dark theme" → MERGE + reinforce
        // Frequent mentions = higher importance in search results
    }
    
    @Override
    public Map<String, Object> extractProperties(String memory) {
        // Could extract category, confidence, etc. from content
        return Map.of(
            "category", "preference",
            "persistent", true
        );
    }
}

