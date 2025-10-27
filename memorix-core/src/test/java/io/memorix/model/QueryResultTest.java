package io.memorix.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for QueryResult model.
 */
class QueryResultTest {
    
    @Test
    void shouldCreateQueryResult() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .build();
        
        List<Memory> memories = List.of(memory1, memory2);
        
        QueryResult.QueryMetadata metadata = new QueryResult.QueryMetadata(
                10,     // totalFound
                2,      // returned
                150,    // totalTokens
                0.85,   // avgSimilarity
                "maxTokens",  // limitReason
                50L     // executionTimeMs
        );
        
        // When
        QueryResult result = new QueryResult(memories, metadata);
        
        // Then
        assertThat(result.getMemories()).hasSize(2);
        assertThat(result.getMetadata().getTotalFound()).isEqualTo(10);
        assertThat(result.getMetadata().getReturned()).isEqualTo(2);
        assertThat(result.getMetadata().getTotalTokens()).isEqualTo(150);
        assertThat(result.getMetadata().getAvgSimilarity()).isEqualTo(0.85);
        assertThat(result.getMetadata().getLimitReason()).isEqualTo("maxTokens");
        assertThat(result.getMetadata().getExecutionTimeMs()).isEqualTo(50L);
    }
    
    @Test
    void shouldReturnDefensiveCopyOfMemories() {
        // Given
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test")
                .build();
        
        QueryResult.QueryMetadata metadata = new QueryResult.QueryMetadata(
                1, 1, 50, 0.9, "none", 10L
        );
        
        QueryResult result = new QueryResult(List.of(memory), metadata);
        
        // When
        List<Memory> memories = result.getMemories();
        
        // Then - Should be defensive copy
        assertThat(memories).isNotSameAs(result.getMemories());
    }
    
    @Test
    void shouldProvideMetadataToString() {
        // Given
        QueryResult.QueryMetadata metadata = new QueryResult.QueryMetadata(
                47,     // totalFound
                18,     // returned
                496,    // totalTokens
                0.78,   // avgSimilarity
                "maxTokens",
                45L
        );
        
        // When
        String toString = metadata.toString();
        
        // Then
        assertThat(toString).contains("totalFound=47");
        assertThat(toString).contains("returned=18");
        assertThat(toString).contains("totalTokens=496");
        assertThat(toString).contains("avgSimilarity=0");  // Locale-independent
        assertThat(toString).contains("78");  // Should contain number
        assertThat(toString).contains("limitReason='maxTokens'");
        assertThat(toString).contains("executionTimeMs=45");
    }
}

