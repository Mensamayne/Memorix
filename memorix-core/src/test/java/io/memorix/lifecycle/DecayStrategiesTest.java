package io.memorix.lifecycle;

import io.memorix.model.DecayConfig;
import io.memorix.model.DecayContext;
import io.memorix.model.Memory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for all decay strategies.
 */
class DecayStrategiesTest {
    
    // ===== Usage-Based Strategy Tests =====
    
    @Test
    void usageBasedStrategy_shouldReinforceWhenUsed() {
        // Given
        DecayStrategy strategy = new UsageBasedDecayStrategy();
        Memory memory = createMemory(100);
        DecayContext context = DecayContext.builder()
                .wasUsedInSession(true)
                .decayConfig(DecayConfig.builder()
                        .decayReinforcement(6)
                        .maxDecay(128)
                        .build())
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        assertThat(newDecay).isEqualTo(106);  // 100 + 6
    }
    
    @Test
    void usageBasedStrategy_shouldDecayWhenNotUsedInActiveSession() {
        // Given
        DecayStrategy strategy = new UsageBasedDecayStrategy();
        Memory memory = createMemory(100);
        DecayContext context = DecayContext.builder()
                .wasUsedInSession(false)
                .isActiveSession(true)
                .decayConfig(DecayConfig.builder()
                        .decayReduction(4)
                        .minDecay(0)
                        .build())
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        assertThat(newDecay).isEqualTo(96);  // 100 - 4
    }
    
    @Test
    void usageBasedStrategy_shouldFreezeWhenSessionInactive() {
        // Given
        DecayStrategy strategy = new UsageBasedDecayStrategy();
        Memory memory = createMemory(100);
        DecayContext context = DecayContext.builder()
                .wasUsedInSession(false)
                .isActiveSession(false)  // User not using app
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        assertThat(newDecay).isEqualTo(100);  // FROZEN!
    }
    
    @Test
    void usageBasedStrategy_shouldNotExceedMaxDecay() {
        // Given
        DecayStrategy strategy = new UsageBasedDecayStrategy();
        Memory memory = createMemory(125);
        DecayContext context = DecayContext.builder()
                .wasUsedInSession(true)
                .decayConfig(DecayConfig.builder()
                        .decayReinforcement(10)
                        .maxDecay(128)
                        .build())
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        assertThat(newDecay).isEqualTo(128);  // Clamped to max
    }
    
    // ===== Time-Based Strategy Tests =====
    
    @Test
    void timeBasedStrategy_shouldDecayByTime() {
        // Given
        DecayStrategy strategy = new TimeBasedDecayStrategy();
        Memory memory = createMemory(100);
        DecayContext context = DecayContext.builder()
                .timeSinceCreated(Duration.ofDays(21))  // 3 weeks
                .decayConfig(DecayConfig.builder()
                        .initialDecay(100)
                        .decayReduction(5)
                        .decayInterval(Duration.ofDays(7))  // Weekly
                        .minDecay(0)
                        .build())
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        // 3 weeks = 3 intervals → 100 - (3 × 5) = 85
        assertThat(newDecay).isEqualTo(85);
    }
    
    @Test
    void timeBasedStrategy_shouldNotGoBelowMin() {
        // Given
        DecayStrategy strategy = new TimeBasedDecayStrategy();
        Memory memory = createMemory(100);
        DecayContext context = DecayContext.builder()
                .timeSinceCreated(Duration.ofDays(365))  // 1 year
                .decayConfig(DecayConfig.builder()
                        .initialDecay(100)
                        .decayReduction(5)
                        .decayInterval(Duration.ofDays(7))
                        .minDecay(0)
                        .build())
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        assertThat(newDecay).isEqualTo(0);  // Clamped to min
    }
    
    // ===== Hybrid Strategy Tests =====
    
    @Test
    void hybridStrategy_shouldCombineUsageAndTime() {
        // Given
        DecayStrategy strategy = new HybridDecayStrategy();
        Memory memory = createMemory(100);
        memory.setImportance(0.5f);
        
        DecayContext context = DecayContext.builder()
                .wasUsedInSession(true)
                .timeSinceLastUse(Duration.ofDays(100))  // Long inactivity
                .decayConfig(DecayConfig.builder()
                        .decayReinforcement(6)
                        .maxDecay(150)
                        .minDecay(0)
                        .strategyParam("inactivityThreshold", 90)
                        .strategyParam("timeDecay", 2)
                        .build())
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        // Usage: +6, Time: -2 → 100 + 6 - 2 = 104
        assertThat(newDecay).isGreaterThan(100);  // Reinforced despite time decay
    }
    
    @Test
    void hybridStrategy_shouldBoostImportantMemories() {
        // Given
        DecayStrategy strategy = new HybridDecayStrategy();
        Memory memory = createMemory(100);
        memory.setImportance(0.9f);  // Very important
        
        DecayContext context = DecayContext.builder()
                .isActiveSession(true)
                .wasUsedInSession(false)
                .decayConfig(DecayConfig.builder()
                        .decayReduction(4)
                        .minDecay(0)
                        .maxDecay(150)
                        .build())
                .build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        // Gentle decay (-2) + importance bonus (+1) = -1
        assertThat(newDecay).isEqualTo(99);
    }
    
    // ===== Permanent Strategy Tests =====
    
    @Test
    void permanentStrategy_shouldNeverChange() {
        // Given
        DecayStrategy strategy = new PermanentDecayStrategy();
        Memory memory = createMemory(100);
        DecayContext context = DecayContext.builder().build();
        
        // When
        int newDecay = strategy.calculateDecay(memory, context);
        
        // Then
        assertThat(newDecay).isEqualTo(100);  // Never changes
    }
    
    @Test
    void permanentStrategy_shouldNeverAutoDelete() {
        // Given
        DecayStrategy strategy = new PermanentDecayStrategy();
        Memory memory = createMemory(0);  // Even at 0!
        DecayContext context = DecayContext.builder()
                .decayConfig(DecayConfig.builder()
                        .autoDelete(true)
                        .minDecay(0)
                        .build())
                .build();
        
        // When
        boolean shouldDelete = strategy.shouldAutoDelete(memory, context);
        
        // Then
        assertThat(shouldDelete).isFalse();  // Never delete
    }
    
    // ===== Strategy Name Tests =====
    
    @Test
    void shouldHaveCorrectStrategyNames() {
        assertThat(new UsageBasedDecayStrategy().getStrategyName())
                .isEqualTo("USAGE_BASED");
        assertThat(new TimeBasedDecayStrategy().getStrategyName())
                .isEqualTo("TIME_BASED");
        assertThat(new HybridDecayStrategy().getStrategyName())
                .isEqualTo("HYBRID");
        assertThat(new PermanentDecayStrategy().getStrategyName())
                .isEqualTo("PERMANENT");
    }
    
    // Helper method
    private Memory createMemory(int decay) {
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test content")
                .build();
        memory.setDecay(decay);
        return memory;
    }
}

