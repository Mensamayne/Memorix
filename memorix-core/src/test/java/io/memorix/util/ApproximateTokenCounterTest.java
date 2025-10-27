package io.memorix.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ApproximateTokenCounter.
 */
class ApproximateTokenCounterTest {
    
    private final TokenCounter tokenCounter = new ApproximateTokenCounter();
    
    @Test
    void shouldCountTokens() {
        // Given
        String text = "This is a test sentence with twelve words in it indeed.";
        
        // When
        int tokens = tokenCounter.count(text);
        
        // Then
        // ~57 chars / 3 = ~19 tokens
        assertThat(tokens).isGreaterThan(15).isLessThan(25);
    }
    
    @Test
    void shouldReturnZeroForNullText() {
        assertThat(tokenCounter.count(null)).isEqualTo(0);
    }
    
    @Test
    void shouldReturnZeroForEmptyText() {
        assertThat(tokenCounter.count("")).isEqualTo(0);
    }
    
    @Test
    void shouldReturnOneForShortText() {
        assertThat(tokenCounter.count("Hi")).isEqualTo(1);
    }
    
    @Test
    void shouldApproximateLongerText() {
        // Given
        String longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                         "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";
        
        // When
        int tokens = tokenCounter.count(longText);
        
        // Then
        // ~123 chars / 3 = ~41 tokens
        assertThat(tokens).isGreaterThan(35).isLessThan(50);
    }
}

