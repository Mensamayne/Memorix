package io.memorix;

import io.memorix.exception.StorageException;
import io.memorix.model.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for update functionality with real database.
 * 
 * <p>Tests complete update flow:
 * <ol>
 *   <li>Save memory</li>
 *   <li>Update content/importance/metadata</li>
 *   <li>Verify changes persisted</li>
 *   <li>Verify embedding regeneration on content change</li>
 *   <li>Test immutability protection</li>
 * </ol>
 */
@SpringBootTest(
    classes = TestApplication.class,
    properties = {
        "spring.main.web-application-type=none"  // Disable web server & command line runners
    }
)
@Testcontainers
class UpdateIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_update_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Disable multi-datasource for tests
        registry.add("memorix.multi-datasource.enabled", () -> "false");
    }
    
    @Autowired
    private Memorix memorix;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE memories CASCADE");
    }
    
    @Test
    void shouldUpdateContent() {
        // Given - Save original memory
        Memory original = memorix.store("user123")
            .content("Original content")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .save();
        
        String memoryId = original.getId();
        float[] originalEmbedding = original.getEmbedding();
        
        // When - Update content
        Memory updated = memorix.update(memoryId)
            .content("Updated content")
            .execute();
        
        // Then
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(memoryId);
        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(updated.getImportance()).isEqualTo(0.5f);  // Unchanged
        
        // Embedding should be regenerated
        assertThat(updated.getEmbedding()).isNotEqualTo(originalEmbedding);
        assertThat(updated.getEmbedding()).isNotNull();
        assertThat(updated.getEmbedding().length).isEqualTo(1536);
        
        // UpdatedAt should be changed
        assertThat(updated.getUpdatedAt()).isAfter(original.getCreatedAt());
    }
    
    @Test
    void shouldUpdateImportance() {
        // Given
        Memory original = memorix.store("user123")
            .content("Test content")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .save();
        
        // When
        Memory updated = memorix.update(original.getId())
            .importance(0.9f)
            .execute();
        
        // Then
        assertThat(updated.getContent()).isEqualTo("Test content");  // Unchanged
        assertThat(updated.getImportance()).isEqualTo(0.9f);
        
        // Embedding should NOT be regenerated (content unchanged)
        assertThat(updated.getEmbedding()).isEqualTo(original.getEmbedding());
    }
    
    @Test
    void shouldUpdateMetadata() {
        // Given
        Memory original = memorix.store("user123")
            .content("Test content")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .save();
        
        // When
        Map<String, Object> newMetadata = Map.of(
            "category", "updated",
            "priority", "high"
        );
        
        Memory updated = memorix.update(original.getId())
            .metadata(newMetadata)
            .execute();
        
        // Then
        assertThat(updated.getMetadata()).containsEntry("category", "updated");
        assertThat(updated.getMetadata()).containsEntry("priority", "high");
        assertThat(updated.getContent()).isEqualTo("Test content");  // Unchanged
        assertThat(updated.getImportance()).isEqualTo(0.5f);  // Unchanged
    }
    
    @Test
    void shouldUpdateMultipleFields() {
        // Given
        Memory original = memorix.store("user123")
            .content("Original")
            .withType("USER_PREFERENCE")
            .withImportance(0.3f)
            .save();
        
        // When
        Map<String, Object> metadata = Map.of("updated", true);
        Memory updated = memorix.update(original.getId())
            .content("New content")
            .importance(0.8f)
            .metadata(metadata)
            .execute();
        
        // Then
        assertThat(updated.getContent()).isEqualTo("New content");
        assertThat(updated.getImportance()).isEqualTo(0.8f);
        assertThat(updated.getMetadata()).containsEntry("updated", true);
        
        // Embedding regenerated (content changed)
        assertThat(updated.getEmbedding()).isNotEqualTo(original.getEmbedding());
    }
    
    @Test
    void shouldFailUpdateNonExistentMemory() {
        // When/Then
        assertThatThrownBy(() -> 
            memorix.update("non-existent-id")
                .content("New content")
                .execute()
        ).isInstanceOf(StorageException.class)
          .hasMessageContaining("Memory not found");
    }
    
    @Test
    void shouldRejectUpdateImmutableMemory() {
        // Given - Save immutable memory
        Memory original = memorix.store("user123")
            .content("Immutable fact")
            .withType("USER_PREFERENCE")
            .withImportance(0.9f)
            .withProperties(Map.of(
                "immutable", true,
                "source", "user_form"
            ))
            .save();
        
        // When/Then
        assertThatThrownBy(() -> 
            memorix.update(original.getId())
                .content("Try to change")
                .execute()
        ).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot update immutable memory")
          .hasMessageContaining("user_form");
    }
    
    @Test
    void shouldAllowUpdateWhenImmutableFalse() {
        // Given - Save memory with immutable=false
        Memory original = memorix.store("user123")
            .content("Mutable content")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .withProperties(Map.of("immutable", false))
            .save();
        
        // When
        Memory updated = memorix.update(original.getId())
            .content("Changed content")
            .execute();
        
        // Then
        assertThat(updated.getContent()).isEqualTo("Changed content");
    }
    
    @Test
    void shouldAllowUpdateWhenImmutableNotSet() {
        // Given - Save memory without immutable field
        Memory original = memorix.store("user123")
            .content("Normal content")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .save();
        
        // When
        Memory updated = memorix.update(original.getId())
            .content("Changed content")
            .execute();
        
        // Then
        assertThat(updated.getContent()).isEqualTo("Changed content");
    }
    
    @Test
    void shouldUpdateOnlySpecifiedFields() {
        // Given
        Memory original = memorix.store("user123")
            .content("Original content")
            .withType("USER_PREFERENCE")
            .withImportance(0.7f)
            .save();
        
        float[] originalEmbedding = original.getEmbedding();
        
        // When - Update only importance
        Memory updated = memorix.update(original.getId())
            .importance(0.9f)
            .execute();
        
        // Then
        assertThat(updated.getContent()).isEqualTo("Original content");  // Unchanged
        assertThat(updated.getImportance()).isEqualTo(0.9f);  // Changed
        assertThat(updated.getEmbedding()).isEqualTo(originalEmbedding);  // Unchanged
    }
    
    @Test
    void shouldPreserveDecayOnUpdate() {
        // Given
        Memory original = memorix.store("user123")
            .content("Test content")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .save();
        
        int originalDecay = original.getDecay();
        
        // When
        Memory updated = memorix.update(original.getId())
            .content("Updated content")
            .execute();
        
        // Then
        assertThat(updated.getDecay()).isEqualTo(originalDecay);
    }
    
    @Test
    void shouldUpdateTokenCountOnContentChange() {
        // Given
        Memory original = memorix.store("user123")
            .content("Short")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .save();
        
        int originalTokenCount = original.getTokenCount();
        
        // When
        Memory updated = memorix.update(original.getId())
            .content("Much longer content that has more tokens than before")
            .execute();
        
        // Then
        assertThat(updated.getTokenCount()).isGreaterThan(originalTokenCount);
    }
}

