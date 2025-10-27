package io.memorix;

import io.memorix.model.Memory;
import io.memorix.service.MemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UpdateBuilder fluent API.
 * 
 * <p>Tests builder validation and method chaining without database dependency.
 */
@ExtendWith(MockitoExtension.class)
class UpdateBuilderTest {
    
    @Mock
    private MemoryService memoryService;
    
    private Memorix memorix;
    
    @BeforeEach
    void setUp() {
        memorix = new Memorix(memoryService, null);
    }
    
    @Test
    void shouldCreateUpdateBuilder() {
        // When
        Memorix.UpdateBuilder builder = memorix.update("memory-123");
        
        // Then
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldRejectNullMemoryId() {
        // When/Then
        assertThatThrownBy(() -> memorix.update(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Memory ID cannot be null or empty");
    }
    
    @Test
    void shouldRejectEmptyMemoryId() {
        // When/Then
        assertThatThrownBy(() -> memorix.update(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Memory ID cannot be null or empty");
    }
    
    @Test
    void shouldRejectBlankMemoryId() {
        // When/Then
        assertThatThrownBy(() -> memorix.update("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Memory ID cannot be null or empty");
    }
    
    @Test
    void shouldChainContentUpdate() {
        // When
        Memorix.UpdateBuilder builder = memorix.update("memory-123")
            .content("New content");
        
        // Then
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldChainImportanceUpdate() {
        // When
        Memorix.UpdateBuilder builder = memorix.update("memory-123")
            .importance(0.9f);
        
        // Then
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldChainMetadataUpdate() {
        // When
        Memorix.UpdateBuilder builder = memorix.update("memory-123")
            .metadata(Map.of("key", "value"));
        
        // Then
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldChainMultipleUpdates() {
        // When
        Memorix.UpdateBuilder builder = memorix.update("memory-123")
            .content("New content")
            .importance(0.8f)
            .metadata(Map.of("updated", true));
        
        // Then
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldRejectImportanceBelowZero() {
        // When/Then
        assertThatThrownBy(() -> 
            memorix.update("memory-123")
                .importance(-0.1f)
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Importance must be between 0.0 and 1.0");
    }
    
    @Test
    void shouldRejectImportanceAboveOne() {
        // When/Then
        assertThatThrownBy(() -> 
            memorix.update("memory-123")
                .importance(1.1f)
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Importance must be between 0.0 and 1.0");
    }
    
    @Test
    void shouldAcceptImportanceZero() {
        // When
        Memorix.UpdateBuilder builder = memorix.update("memory-123")
            .importance(0.0f);
        
        // Then
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldAcceptImportanceOne() {
        // When
        Memorix.UpdateBuilder builder = memorix.update("memory-123")
            .importance(1.0f);
        
        // Then
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldCallMemoryServiceUpdate() {
        // Given
        Memory updatedMemory = Memory.builder()
            .id("memory-123")
            .userId("user123")
            .content("Updated content")
            .importance(0.9f)
            .build();
        
        when(memoryService.update(anyString(), anyString(), any(), any()))
            .thenReturn(updatedMemory);
        
        // When
        Memory result = memorix.update("memory-123")
            .content("Updated content")
            .importance(0.9f)
            .execute();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("memory-123");
        assertThat(result.getContent()).isEqualTo("Updated content");
        
        verify(memoryService).update(
            eq("memory-123"),
            eq("Updated content"),
            eq(0.9f),
            isNull()
        );
    }
    
    @Test
    void shouldPassNullForUnspecifiedFields() {
        // Given
        Memory updatedMemory = Memory.builder()
            .id("memory-123")
            .userId("user123")
            .content("Original content")
            .importance(0.7f)
            .build();
        
        when(memoryService.update(anyString(), isNull(), any(), isNull()))
            .thenReturn(updatedMemory);
        
        // When - Only update importance
        Memory result = memorix.update("memory-123")
            .importance(0.7f)
            .execute();
        
        // Then
        assertThat(result).isNotNull();
        
        verify(memoryService).update(
            eq("memory-123"),
            isNull(),  // content not specified
            eq(0.7f),
            isNull()   // metadata not specified
        );
    }
    
    @Test
    void shouldPassAllFieldsWhenSpecified() {
        // Given
        Map<String, Object> metadata = Map.of("category", "test");
        Memory updatedMemory = Memory.builder()
            .id("memory-123")
            .userId("user123")
            .content("New content")
            .importance(0.6f)
            .build();
        
        when(memoryService.update(anyString(), anyString(), any(), any()))
            .thenReturn(updatedMemory);
        
        // When
        Memory result = memorix.update("memory-123")
            .content("New content")
            .importance(0.6f)
            .metadata(metadata)
            .execute();
        
        // Then
        assertThat(result).isNotNull();
        
        verify(memoryService).update(
            eq("memory-123"),
            eq("New content"),
            eq(0.6f),
            eq(metadata)
        );
    }
}

