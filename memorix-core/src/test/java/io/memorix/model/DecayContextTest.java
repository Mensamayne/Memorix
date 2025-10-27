package io.memorix.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DecayContext.
 */
class DecayContextTest {
    
    @Test
    void shouldBuildWithDefaults() {
        // When
        DecayContext context = DecayContext.builder().build();
        
        // Then
        assertThat(context.getNow()).isNotNull();
        assertThat(context.wasUsedInSession()).isFalse();
        assertThat(context.isActiveSession()).isTrue();
        assertThat(context.getSessionsSinceLastUse()).isEqualTo(0);
        assertThat(context.getTimeSinceLastUse()).isEqualTo(Duration.ZERO);
        assertThat(context.getTimeSinceCreated()).isEqualTo(Duration.ZERO);
        assertThat(context.getTotalUsageCount()).isEqualTo(0);
    }
    
    @Test
    void shouldBuildWithCustomValues() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        DecayConfig config = DecayConfig.builder().build();
        
        // When
        DecayContext context = DecayContext.builder()
                .now(now)
                .wasUsedInSession(true)
                .isActiveSession(true)
                .sessionsSinceLastUse(5)
                .timeSinceLastUse(Duration.ofDays(10))
                .timeSinceCreated(Duration.ofDays(30))
                .totalUsageCount(25)
                .decayConfig(config)
                .customParam("key", "value")
                .build();
        
        // Then
        assertThat(context.getNow()).isEqualTo(now);
        assertThat(context.wasUsedInSession()).isTrue();
        assertThat(context.isActiveSession()).isTrue();
        assertThat(context.getSessionsSinceLastUse()).isEqualTo(5);
        assertThat(context.getTimeSinceLastUse()).isEqualTo(Duration.ofDays(10));
        assertThat(context.getTimeSinceCreated()).isEqualTo(Duration.ofDays(30));
        assertThat(context.getTotalUsageCount()).isEqualTo(25);
        assertThat(context.getDecayConfig()).isEqualTo(config);
        assertThat(context.getCustomParams()).containsEntry("key", "value");
    }
    
    @Test
    void shouldCreateContextForMemory() {
        // Given
        LocalDateTime past = LocalDateTime.now().minusDays(10);
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test")
                .createdAt(past)
                .lastAccessedAt(past.plusDays(5))
                .build();
        
        DecayConfig config = DecayConfig.builder().build();
        
        // When
        DecayContext context = DecayContext.forMemory(memory, config);
        
        // Then
        assertThat(context.getTimeSinceCreated().toDays()).isEqualTo(10);
        assertThat(context.getTimeSinceLastUse().toDays()).isEqualTo(5);
        assertThat(context.getDecayConfig()).isEqualTo(config);
    }
    
    @Test
    void shouldHandleMemoryWithoutLastAccessed() {
        // Given
        LocalDateTime past = LocalDateTime.now().minusDays(10);
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test")
                .createdAt(past)
                .build();
        // lastAccessedAt is null
        
        DecayConfig config = DecayConfig.builder().build();
        
        // When
        DecayContext context = DecayContext.forMemory(memory, config);
        
        // Then - Uses createdAt as fallback
        assertThat(context.getTimeSinceLastUse()).isEqualTo(context.getTimeSinceCreated());
    }
    
    @Test
    void shouldProvideConvenienceMethods() {
        // Given
        DecayConfig config = DecayConfig.builder()
                .initialDecay(100)
                .minDecay(0)
                .maxDecay(200)
                .decayReduction(5)
                .decayReinforcement(10)
                .decayInterval(Duration.ofDays(14))
                .strategyParam("testParam", 42)
                .build();
        
        DecayContext context = DecayContext.builder()
                .decayConfig(config)
                .build();
        
        // Then
        assertThat(context.getInitialDecay()).isEqualTo(100);
        assertThat(context.getMinDecay()).isEqualTo(0);
        assertThat(context.getMaxDecay()).isEqualTo(200);
        assertThat(context.getDecayReduction()).isEqualTo(5);
        assertThat(context.getDecayReinforcement()).isEqualTo(10);
        assertThat(context.getDecayInterval()).isEqualTo(Duration.ofDays(14));
        assertThat(context.getStrategyParam("testParam", 0)).isEqualTo(42);
    }
    
    @Test
    void shouldProvideDefaultsWhenConfigNull() {
        // Given
        DecayContext context = DecayContext.builder().build();
        
        // Then - Should use defaults
        assertThat(context.getInitialDecay()).isEqualTo(100);
        assertThat(context.getMinDecay()).isEqualTo(0);
        assertThat(context.getMaxDecay()).isEqualTo(128);
        assertThat(context.getDecayReduction()).isEqualTo(4);
        assertThat(context.getDecayReinforcement()).isEqualTo(6);
        assertThat(context.getDecayInterval()).isEqualTo(Duration.ofDays(7));
    }
    
    @Test
    void shouldGetCustomParamWithDefault() {
        // Given
        DecayContext context = DecayContext.builder()
                .customParam("existing", "value")
                .build();
        
        // Then
        assertThat(context.getCustomParam("existing", "default"))
                .isEqualTo("value");
        assertThat(context.getCustomParam("nonExistent", "default"))
                .isEqualTo("default");
    }
}

