package io.memorix.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Memory model.
 */
class MemoryTest {
    
    @Test
    void shouldBuildMemoryWithRequiredFields() {
        // When
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test content")
                .build();
        
        // Then
        assertThat(memory.getUserId()).isEqualTo("user123");
        assertThat(memory.getContent()).isEqualTo("Test content");
        assertThat(memory.getDecay()).isEqualTo(100);  // Default
        assertThat(memory.getImportance()).isEqualTo(0.5f);  // Default
        assertThat(memory.getCreatedAt()).isNotNull();
        assertThat(memory.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void shouldBuildMemoryWithAllFields() {
        // Given
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> metadata = Map.of("key", "value");
        
        // When
        Memory memory = Memory.builder()
                .id("123")
                .userId("user123")
                .content("Test content")
                .embedding(embedding)
                .decay(110)
                .importance(0.9f)
                .tokenCount(50)
                .metadata(metadata)
                .createdAt(now)
                .updatedAt(now)
                .lastAccessedAt(now)
                .build();
        
        // Then
        assertThat(memory.getId()).isEqualTo("123");
        assertThat(memory.getUserId()).isEqualTo("user123");
        assertThat(memory.getContent()).isEqualTo("Test content");
        assertThat(memory.getEmbedding()).isEqualTo(embedding);
        assertThat(memory.getDecay()).isEqualTo(110);
        assertThat(memory.getImportance()).isEqualTo(0.9f);
        assertThat(memory.getTokenCount()).isEqualTo(50);
        assertThat(memory.getMetadata()).containsEntry("key", "value");
        assertThat(memory.getCreatedAt()).isEqualTo(now);
        assertThat(memory.getUpdatedAt()).isEqualTo(now);
        assertThat(memory.getLastAccessedAt()).isEqualTo(now);
    }
    
    @Test
    void shouldThrowExceptionWhenUserIdNull() {
        // When/Then
        assertThatThrownBy(() ->
            Memory.builder()
                    .content("Test")
                    .build()
        ).isInstanceOf(NullPointerException.class)
         .hasMessageContaining("userId");
    }
    
    @Test
    void shouldThrowExceptionWhenContentNull() {
        // When/Then
        assertThatThrownBy(() ->
            Memory.builder()
                    .userId("user123")
                    .build()
        ).isInstanceOf(NullPointerException.class)
         .hasMessageContaining("content");
    }
    
    @Test
    void shouldAddMetadata() {
        // When
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test")
                .addMetadata("key1", "value1")
                .addMetadata("key2", 123)
                .build();
        
        // Then
        assertThat(memory.getMetadata())
                .containsEntry("key1", "value1")
                .containsEntry("key2", 123);
    }
    
    @Test
    void shouldTestEquality() {
        // Given
        Memory memory1 = new Memory();
        memory1.setId("123");
        
        Memory memory2 = new Memory();
        memory2.setId("123");
        
        Memory memory3 = new Memory();
        memory3.setId("456");
        
        // Then
        assertThat(memory1).isEqualTo(memory2);
        assertThat(memory1).isNotEqualTo(memory3);
        assertThat(memory1.hashCode()).isEqualTo(memory2.hashCode());
    }
    
    @Test
    void shouldProvideToString() {
        // Given
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test")
                .decay(110)
                .importance(0.8f)
                .tokenCount(50)
                .build();
        
        // When
        String toString = memory.toString();
        
        // Then
        assertThat(toString).contains("user123");
        assertThat(toString).contains("decay=110");
        assertThat(toString).contains("importance=0.8");
        assertThat(toString).contains("tokenCount=50");
    }
}

