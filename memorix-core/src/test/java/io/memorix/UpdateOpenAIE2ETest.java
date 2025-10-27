package io.memorix;

import io.memorix.model.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
 * End-to-End test for update functionality with REAL OpenAI API.
 * 
 * <p>Tests that update correctly regenerates semantic embeddings when content changes.
 * 
 * <p>Requirements:
 * <ul>
 *   <li>Set OPENAI_API_KEY environment variable</li>
 *   <li>Docker must be running (for Testcontainers PostgreSQL)</li>
 *   <li>Internet connection required</li>
 * </ul>
 * 
 * <p>To run: {@code mvn test -Dtest=UpdateOpenAIE2ETest}
 */
@SpringBootTest(
    classes = TestApplication.class,
    properties = {
        "spring.main.web-application-type=none"  // Disable web server & command line runners
    }
)
@Testcontainers
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class UpdateOpenAIE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_openai_e2e_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Use REAL OpenAI API
        registry.add("memorix.embedding.provider", () -> "openai");
        registry.add("memorix.embedding.openai.api-key", 
            () -> System.getenv("OPENAI_API_KEY"));
        registry.add("memorix.embedding.openai.model", () -> "text-embedding-3-small");
        
        // Disable multi-datasource
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
    void shouldRegenerateSemanticEmbeddingOnContentUpdate() throws InterruptedException {
        // Given - Save memory about pizza
        Memory original = memorix.store("user123")
            .content("User loves pizza margherita with fresh basil")
            .withType("USER_PREFERENCE")
            .withImportance(0.8f)
            .save();
        
        float[] pizzaEmbedding = original.getEmbedding();
        assertThat(pizzaEmbedding).isNotNull();
        assertThat(pizzaEmbedding.length).isEqualTo(1536);  // OpenAI text-embedding-3-small
        
        // Wait a bit to ensure updatedAt timestamp is different
        Thread.sleep(100);
        
        // When - Update to completely different content (sushi)
        Memory updated = memorix.update(original.getId())
            .content("User prefers sushi and sashimi")
            .execute();
        
        float[] sushiEmbedding = updated.getEmbedding();
        
        // Then - Embeddings should be DIFFERENT (different semantic meaning)
        assertThat(sushiEmbedding).isNotNull();
        assertThat(sushiEmbedding.length).isEqualTo(1536);
        assertThat(sushiEmbedding).isNotEqualTo(pizzaEmbedding);
        
        // Verify content changed
        assertThat(updated.getContent()).isEqualTo("User prefers sushi and sashimi");
        
        // Other fields unchanged
        assertThat(updated.getImportance()).isEqualTo(0.8f);
        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getUserId()).isEqualTo("user123");
    }
    
    @Test
    void shouldPreserveEmbeddingWhenContentUnchanged() {
        // Given
        Memory original = memorix.store("user123")
            .content("User enjoys reading books")
            .withType("USER_PREFERENCE")
            .withImportance(0.6f)
            .save();
        
        float[] originalEmbedding = original.getEmbedding();
        
        // When - Update only importance (content unchanged)
        Memory updated = memorix.update(original.getId())
            .importance(0.9f)
            .execute();
        
        // Then - Embedding should be IDENTICAL (no regeneration)
        assertThat(updated.getEmbedding()).isEqualTo(originalEmbedding);
        assertThat(updated.getContent()).isEqualTo("User enjoys reading books");
        assertThat(updated.getImportance()).isEqualTo(0.9f);
    }
    
    @Test
    void shouldFindUpdatedMemoryBySemanticSearch() {
        // Given - Save memory about programming
        Memory original = memorix.store("user123")
            .content("Loves programming in Java")
            .withType("USER_PREFERENCE")
            .withImportance(0.7f)
            .save();
        
        // When - Update to Python
        Memory updated = memorix.update(original.getId())
            .content("Loves programming in Python")
            .execute();
        
        // Then - Search for "Python programming" should find updated memory
        var results = memorix.query("user123")
            .search("Python programming")
            .withType("USER_PREFERENCE")
            .execute();
        
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getId()).isEqualTo(updated.getId());
        assertThat(results.get(0).getContent()).contains("Python");
        
        // Search for "Java programming" should have lower similarity
        var javaResults = memorix.query("user123")
            .search("Java programming")
            .withType("USER_PREFERENCE")
            .execute();
        
        // Memory content is about Python now, so it might still match but less relevant
        if (!javaResults.isEmpty()) {
            // If found, verify it's our memory but content is about Python
            assertThat(javaResults.get(0).getContent()).contains("Python");
        }
    }
    
    @Test
    void shouldUpdateAndQueryWithMultipleMemories() {
        // Given - Save 3 different memories
        Memory pizza = memorix.store("user123")
            .content("User loves pizza")
            .withType("USER_PREFERENCE")
            .withImportance(0.8f)
            .save();
        
        Memory sushi = memorix.store("user123")
            .content("User likes sushi")
            .withType("USER_PREFERENCE")
            .withImportance(0.7f)
            .save();
        
        Memory pasta = memorix.store("user123")
            .content("User enjoys pasta")
            .withType("USER_PREFERENCE")
            .withImportance(0.6f)
            .save();
        
        // When - Update pasta to burger
        Memory updated = memorix.update(pasta.getId())
            .content("User prefers burgers")
            .execute();
        
        // Then - Search for "Italian food" should find pizza, NOT the updated one
        var italianResults = memorix.query("user123")
            .search("Italian food")
            .withType("USER_PREFERENCE")
            .limit(2)
            .execute();
        
        assertThat(italianResults).isNotEmpty();
        // Should find pizza (most relevant to Italian food)
        assertThat(italianResults.get(0).getContent()).contains("pizza");
        
        // Search for "fast food" should find burger
        var fastFoodResults = memorix.query("user123")
            .search("fast food")
            .withType("USER_PREFERENCE")
            .limit(2)
            .execute();
        
        assertThat(fastFoodResults).isNotEmpty();
        // Burger is now most relevant to fast food
        boolean foundBurger = fastFoodResults.stream()
            .anyMatch(m -> m.getContent().contains("burgers"));
        assertThat(foundBurger).isTrue();
    }
    
    @Test
    void shouldUpdateWithMetadataAndPreserveSearchability() {
        // Given
        Memory original = memorix.store("user123")
            .content("User studies machine learning")
            .withType("USER_PREFERENCE")
            .withImportance(0.9f)
            .save();
        
        // When - Update with metadata
        Memory updated = memorix.update(original.getId())
            .metadata(Map.of(
                "category", "education",
                "level", "advanced",
                "updated", true
            ))
            .execute();
        
        // Then - Should still be findable by semantic search
        var results = memorix.query("user123")
            .search("machine learning study")
            .withType("USER_PREFERENCE")
            .execute();
        
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getId()).isEqualTo(updated.getId());
        assertThat(results.get(0).getMetadata()).containsEntry("category", "education");
        assertThat(results.get(0).getMetadata()).containsEntry("level", "advanced");
    }
    
    @Test
    void shouldHandleImmutableProtectionWithRealEmbeddings() {
        // Given - Save immutable user fact
        Memory immutable = memorix.store("user123")
            .content("User is 25 years old")
            .withType("USER_PREFERENCE")
            .withImportance(1.0f)
            .withProperties(Map.of(
                "immutable", true,
                "source", "user_form",
                "verified", true
            ))
            .save();
        
        // Verify embedding was generated
        assertThat(immutable.getEmbedding()).isNotNull();
        assertThat(immutable.getEmbedding().length).isEqualTo(1536);
        
        // When/Then - Should reject update
        assertThatThrownBy(() -> 
            memorix.update(immutable.getId())
                .content("User is 30 years old")
                .execute()
        ).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot update immutable memory")
          .hasMessageContaining("user_form");
        
        // Verify original memory unchanged
        var results = memorix.query("user123")
            .search("user age")
            .withType("USER_PREFERENCE")
            .execute();
        
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getContent()).contains("25 years old");
    }
}

