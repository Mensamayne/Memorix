package io.memorix.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DecayConfig model.
 */
class DecayConfigTest {
    
    @Test
    void shouldBuildWithDefaults() {
        // When
        DecayConfig config = DecayConfig.builder().build();
        
        // Then
        assertThat(config.getStrategyClassName())
                .isEqualTo("io.memorix.lifecycle.UsageBasedDecayStrategy");
        assertThat(config.getInitialDecay()).isEqualTo(100);
        assertThat(config.getMinDecay()).isEqualTo(0);
        assertThat(config.getMaxDecay()).isEqualTo(128);
        assertThat(config.getDecayReduction()).isEqualTo(4);
        assertThat(config.getDecayReinforcement()).isEqualTo(6);
        assertThat(config.isAutoDelete()).isTrue();
        assertThat(config.affectsSearchRanking()).isTrue();
    }
    
    @Test
    void shouldBuildWithCustomValues() {
        // When
        DecayConfig config = DecayConfig.builder()
                .strategyClassName("CustomStrategy")
                .initialDecay(150)
                .minDecay(10)
                .maxDecay(200)
                .decayReduction(5)
                .decayReinforcement(10)
                .autoDelete(false)
                .affectsSearchRanking(false)
                .decayInterval(Duration.ofDays(30))
                .strategyParam("key", "value")
                .build();
        
        // Then
        assertThat(config.getStrategyClassName()).isEqualTo("CustomStrategy");
        assertThat(config.getInitialDecay()).isEqualTo(150);
        assertThat(config.getMinDecay()).isEqualTo(10);
        assertThat(config.getMaxDecay()).isEqualTo(200);
        assertThat(config.getDecayReduction()).isEqualTo(5);
        assertThat(config.getDecayReinforcement()).isEqualTo(10);
        assertThat(config.isAutoDelete()).isFalse();
        assertThat(config.affectsSearchRanking()).isFalse();
        assertThat(config.getDecayInterval()).isEqualTo(Duration.ofDays(30));
        assertThat(config.getStrategyParams()).containsEntry("key", "value");
    }
    
    @Test
    void shouldThrowExceptionWhenMinGreaterThanMax() {
        // When/Then
        assertThatThrownBy(() ->
            DecayConfig.builder()
                    .minDecay(150)
                    .maxDecay(100)
                    .build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("minDecay cannot be greater than maxDecay");
    }
    
    @Test
    void shouldThrowExceptionWhenInitialBelowMin() {
        // When/Then
        assertThatThrownBy(() ->
            DecayConfig.builder()
                    .initialDecay(50)
                    .minDecay(100)
                    .build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("initialDecay must be between minDecay and maxDecay");
    }
    
    @Test
    void shouldThrowExceptionWhenInitialAboveMax() {
        // When/Then
        assertThatThrownBy(() ->
            DecayConfig.builder()
                    .initialDecay(150)
                    .maxDecay(100)
                    .build()
        ).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void shouldThrowExceptionForNegativeValues() {
        // initialDecay
        assertThatThrownBy(() ->
            DecayConfig.builder().initialDecay(-1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("initialDecay must be >= 0");
        
        // minDecay
        assertThatThrownBy(() ->
            DecayConfig.builder().minDecay(-1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("minDecay must be >= 0");
        
        // maxDecay
        assertThatThrownBy(() ->
            DecayConfig.builder().maxDecay(-1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("maxDecay must be >= 0");
        
        // decayReduction
        assertThatThrownBy(() ->
            DecayConfig.builder().decayReduction(-1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("decayReduction must be >= 0");
        
        // decayReinforcement
        assertThatThrownBy(() ->
            DecayConfig.builder().decayReinforcement(-1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("decayReinforcement must be >= 0");
    }
    
    @Test
    void shouldAddStrategyParams() {
        // When
        DecayConfig config = DecayConfig.builder()
                .strategyParam("key1", "value1")
                .strategyParam("key2", 123)
                .build();
        
        // Then
        assertThat(config.getStrategyParams())
                .containsEntry("key1", "value1")
                .containsEntry("key2", 123);
    }
    
    @Test
    void shouldReplaceStrategyParams() {
        // When
        DecayConfig config = DecayConfig.builder()
                .strategyParam("key1", "value1")
                .strategyParams(Map.of("key2", "value2"))
                .build();
        
        // Then
        assertThat(config.getStrategyParams())
                .doesNotContainKey("key1")
                .containsEntry("key2", "value2");
    }
}

