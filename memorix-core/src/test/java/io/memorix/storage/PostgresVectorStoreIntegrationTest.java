package io.memorix.storage;

import io.memorix.TestApplication;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PostgresVectorStore using Testcontainers.
 * 
 * <p>Tests real PostgreSQL + pgvector functionality.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class PostgresVectorStoreIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Disable multi-datasource routing for tests
        registry.add("memorix.multi-datasource.enabled", () -> "false");
    }
    
    @Autowired
    private PostgresVectorStore memoryStore;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        jdbcTemplate.execute("TRUNCATE TABLE memories CASCADE");
    }
    
    @Test
    void shouldSaveMemory() {
        // Given
        Memory memory = Memory.builder()
                .userId("user123")
                .content("User loves pizza")
                .importance(0.8f)
                .build();
        
        // When
        Memory saved = memoryStore.save(memory);
        
        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("user123");
        assertThat(saved.getContent()).isEqualTo("User loves pizza");
        assertThat(saved.getImportance()).isEqualTo(0.8f);
        assertThat(saved.getDecay()).isEqualTo(100);  // Default
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void shouldThrowExceptionWhenSavingNullMemory() {
        // When/Then
        assertThatThrownBy(() -> memoryStore.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Memory cannot be null");
    }
    
    @Test
    void shouldThrowExceptionWhenSavingMemoryWithoutUserId() {
        // Given
        Memory memory = new Memory();
        memory.setContent("Test content");
        // userId is null
        
        // When/Then
        assertThatThrownBy(() -> memoryStore.save(memory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }
    
    @Test
    void shouldThrowExceptionWhenSavingMemoryWithoutContent() {
        // Given
        Memory memory = new Memory();
        memory.setUserId("user123");
        
        // When/Then
        assertThatThrownBy(() -> memoryStore.save(memory))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void shouldUpdateMemory() {
        // Given
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Original content")
                .importance(0.5f)
                .build();
        
        Memory saved = memoryStore.save(memory);
        
        // When
        saved.setContent("Updated content");
        saved.setImportance(0.9f);
        saved.setDecay(110);
        Memory updated = memoryStore.update(saved);
        
        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(updated.getImportance()).isEqualTo(0.9f);
        assertThat(updated.getDecay()).isEqualTo(110);
    }
    
    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentMemory() {
        // Given
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test")
                .build();
        memory.setId("non-existent-id");
        
        // When/Then
        assertThatThrownBy(() -> memoryStore.update(memory))
                .isInstanceOf(StorageException.class);
    }
    
    @Test
    void shouldDeleteMemory() {
        // Given
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test content")
                .build();
        
        Memory saved = memoryStore.save(memory);
        
        // When
        memoryStore.delete(saved.getId());
        
        // Then
        Optional<Memory> found = memoryStore.findById(saved.getId());
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldNotThrowWhenDeletingNonExistentMemory() {
        // When/Then - Should not throw
        assertThatCode(() -> memoryStore.delete("non-existent-id"))
                .doesNotThrowAnyException();
    }
    
    @Test
    void shouldDeleteAllMemoriesForUser() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .build();
        
        Memory memory3 = Memory.builder()
                .userId("user456")
                .content("Memory 3")
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        memoryStore.save(memory3);
        
        // When
        int deleted = memoryStore.deleteByUserId("user123");
        
        // Then
        assertThat(deleted).isEqualTo(2);
        assertThat(memoryStore.findByUserId("user123")).isEmpty();
        assertThat(memoryStore.findByUserId("user456")).hasSize(1);
    }
    
    @Test
    void shouldFindMemoryById() {
        // Given
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test content")
                .build();
        
        Memory saved = memoryStore.save(memory);
        
        // When
        Optional<Memory> found = memoryStore.findById(saved.getId());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getContent()).isEqualTo("Test content");
    }
    
    @Test
    void shouldReturnEmptyWhenMemoryNotFound() {
        // When
        Optional<Memory> found = memoryStore.findById("non-existent-id");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldFindAllMemoriesForUser() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .build();
        
        Memory memory3 = Memory.builder()
                .userId("user456")
                .content("Memory 3")
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        memoryStore.save(memory3);
        
        // When
        List<Memory> memories = memoryStore.findByUserId("user123");
        
        // Then
        assertThat(memories).hasSize(2);
        assertThat(memories).allMatch(m -> m.getUserId().equals("user123"));
    }
    
    @Test
    void shouldFindMemoriesByDecayThreshold() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .decay(120)
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .decay(50)
                .build();
        
        Memory memory3 = Memory.builder()
                .userId("user123")
                .content("Memory 3")
                .decay(10)
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        memoryStore.save(memory3);
        
        // When
        List<Memory> memories = memoryStore.findByUserIdAndDecayAbove("user123", 40);
        
        // Then
        assertThat(memories).hasSize(2);
        assertThat(memories).allMatch(m -> m.getDecay() > 40);
    }
    
    @Test
    void shouldCountMemoriesForUser() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        
        // When
        long count = memoryStore.countByUserId("user123");
        
        // Then
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void shouldAutoCalculateTokenCount() {
        // Given
        String content = "This is a test content with approximately 10 tokens in it for testing.";
        Memory memory = Memory.builder()
                .userId("user123")
                .content(content)
                .build();
        
        // When
        Memory saved = memoryStore.save(memory);
        
        // Then
        assertThat(saved.getTokenCount()).isGreaterThan(0);
        assertThat(saved.getTokenCount()).isEqualTo(content.length() / 3);  // Approximate
    }
    
    @Test
    void shouldHandleEmbeddingVector() {
        // Given - Create 1536-dimensional embedding (OpenAI standard)
        float[] embedding = new float[1536];
        for (int i = 0; i < 1536; i++) {
            embedding[i] = (float) (Math.random() * 2 - 1);  // Random values [-1, 1]
        }
        // Set first 3 for testing
        embedding[0] = 0.1f;
        embedding[1] = 0.2f;
        embedding[2] = 0.3f;
        
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test content")
                .embedding(embedding)
                .build();
        
        // When
        Memory saved = memoryStore.save(memory);
        Memory found = memoryStore.findById(saved.getId()).orElseThrow();
        
        // Then
        assertThat(found.getEmbedding()).isNotNull();
        assertThat(found.getEmbedding()).hasSize(1536);
        assertThat(found.getEmbedding()[0]).isEqualTo(0.1f, within(0.001f));
        assertThat(found.getEmbedding()[1]).isEqualTo(0.2f, within(0.001f));
        assertThat(found.getEmbedding()[2]).isEqualTo(0.3f, within(0.001f));
    }
}

