package io.memorix.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for QueryLimit model.
 */
class QueryLimitTest {
    
    @Test
    void shouldBuildWithDefaults() {
        // When
        QueryLimit limit = QueryLimit.builder().build();
        
        // Then
        assertThat(limit.getMaxCount()).isNull();
        assertThat(limit.getMaxTokens()).isNull();
        assertThat(limit.getMinSimilarity()).isNull();
        assertThat(limit.getStrategy()).isEqualTo(LimitStrategy.GREEDY);
    }
    
    @Test
    void shouldBuildWithAllValues() {
        // When
        QueryLimit limit = QueryLimit.builder()
                .maxCount(20)
                .maxTokens(500)
                .minSimilarity(0.6)
                .strategy(LimitStrategy.ALL)
                .build();
        
        // Then
        assertThat(limit.getMaxCount()).isEqualTo(20);
        assertThat(limit.getMaxTokens()).isEqualTo(500);
        assertThat(limit.getMinSimilarity()).isEqualTo(0.6);
        assertThat(limit.getStrategy()).isEqualTo(LimitStrategy.ALL);
    }
    
    @Test
    void shouldThrowExceptionForInvalidMaxCount() {
        // When/Then
        assertThatThrownBy(() ->
            QueryLimit.builder().maxCount(0).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("maxCount must be > 0");
        
        assertThatThrownBy(() ->
            QueryLimit.builder().maxCount(-1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("maxCount must be > 0");
    }
    
    @Test
    void shouldThrowExceptionForInvalidMaxTokens() {
        // When/Then
        assertThatThrownBy(() ->
            QueryLimit.builder().maxTokens(0).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("maxTokens must be > 0");
        
        assertThatThrownBy(() ->
            QueryLimit.builder().maxTokens(-1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("maxTokens must be > 0");
    }
    
    @Test
    void shouldThrowExceptionForInvalidMinSimilarity() {
        // Below 0
        assertThatThrownBy(() ->
            QueryLimit.builder().minSimilarity(-0.1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("minSimilarity must be between 0.0 and 1.0");
        
        // Above 1
        assertThatThrownBy(() ->
            QueryLimit.builder().minSimilarity(1.1).build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("minSimilarity must be between 0.0 and 1.0");
    }
    
    @Test
    void shouldAcceptValidMinSimilarityBounds() {
        // 0.0
        QueryLimit limit1 = QueryLimit.builder().minSimilarity(0.0).build();
        assertThat(limit1.getMinSimilarity()).isEqualTo(0.0);
        
        // 1.0
        QueryLimit limit2 = QueryLimit.builder().minSimilarity(1.0).build();
        assertThat(limit2.getMinSimilarity()).isEqualTo(1.0);
    }
    
    @Test
    void shouldProvideToString() {
        // Given
        QueryLimit limit = QueryLimit.builder()
                .maxCount(20)
                .maxTokens(500)
                .minSimilarity(0.6)
                .strategy(LimitStrategy.GREEDY)
                .build();
        
        // When
        String toString = limit.toString();
        
        // Then
        assertThat(toString).contains("maxCount=20");
        assertThat(toString).contains("maxTokens=500");
        assertThat(toString).contains("minSimilarity=0.6");
        assertThat(toString).contains("strategy=GREEDY");
    }
}

