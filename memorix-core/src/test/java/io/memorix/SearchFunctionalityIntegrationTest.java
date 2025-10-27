package io.memorix;

import io.memorix.model.Memory;
import io.memorix.model.QueryLimit;
import io.memorix.model.QueryResult;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for search functionality.
 * 
 * This test verifies that the complete search flow works:
 * 1. Save memories with embeddings
 * 2. Search for similar content
 * 3. Verify results are returned with correct similarity
 * 
 * Tests the bug reported in API_TESTING_REPORT.md (BUG #3).
 */
@SpringBootTest(classes = io.memorix.MemorixApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
class SearchFunctionalityIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres")
    ).withDatabaseName("memorix_test")
     .withUsername("test")
     .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private Memorix memorix;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String TEST_USER = "search-test-user";
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        jdbcTemplate.update("DELETE FROM memories WHERE user_id = ?", TEST_USER);
    }
    
    @Test
    @Order(1)
    void testSearchReturnsResultsForSimilarContent() {
        // Given: Save memory about dark mode
        Memory saved = memorix.store(TEST_USER)
            .content("I prefer dark mode in applications")
            .withType("USER_PREFERENCE")
            .withImportance(0.8f)
            .save();
        
        assertNotNull(saved.getId(), "Memory should be saved with ID");
        assertNotNull(saved.getEmbedding(), "Memory should have embedding");
        assertEquals(1536, saved.getEmbedding().length, "Embedding should have correct dimension");
        
        // When: Search for similar content
        List<Memory> results = memorix.query(TEST_USER)
            .search("dark theme preferences")
            .withType("USER_PREFERENCE")
            .limit(QueryLimit.builder()
                .maxCount(10)
                .minSimilarity(0.2f)  // Lower threshold for mock provider
                .build())
            .execute();
        
        // Then: Should find the memory
        assertFalse(results.isEmpty(), 
            "Search should return results for similar content (BUG #3 fix verification)");
        assertEquals(1, results.size(), "Should find exactly 1 memory");
        assertEquals(saved.getId(), results.get(0).getId(), "Should return the correct memory");
    }
    
    @Test
    @Order(2)
    void testSearchReturnsSimilarityScores() {
        // Given: Save memories
        memorix.store(TEST_USER)
            .content("I love using dark mode")
            .withType("USER_PREFERENCE")
            .withImportance(0.8f)
            .save();
        
        memorix.store(TEST_USER)
            .content("Weather is sunny today")
            .withType("USER_PREFERENCE")
            .withImportance(0.5f)
            .save();
        
        // When: Search for dark mode
        List<Memory> results = memorix.query(TEST_USER)
            .search("dark theme preferences")
            .withType("USER_PREFERENCE")
            .limit(QueryLimit.builder()
                .maxCount(10)
                .minSimilarity(0.2f)
                .build())
            .execute();
        
        // Then: Results should have similarity scores
        assertFalse(results.isEmpty(), "Should find memories");
        
        for (Memory memory : results) {
            Object similarity = memory.getMetadata().get("similarity");
            assertNotNull(similarity, 
                "Memory should have similarity in metadata (BUG: similarity=0 fix)");
            
            assertTrue(similarity instanceof Double, 
                "Similarity should be Double type");
            
            double similarityValue = (Double) similarity;
            assertTrue(similarityValue >= 0.0 && similarityValue <= 1.0,
                "Similarity should be between 0.0 and 1.0, got: " + similarityValue);
            
            System.out.println("Memory: " + memory.getContent().substring(0, Math.min(30, memory.getContent().length())) + 
                "... -> Similarity: " + String.format("%.4f", similarityValue));
        }
        
        // First result (dark mode) should have higher similarity than second (weather)
        if (results.size() >= 2) {
            double firstSimilarity = (Double) results.get(0).getMetadata().get("similarity");
            double secondSimilarity = (Double) results.get(1).getMetadata().get("similarity");
            
            assertTrue(firstSimilarity >= secondSimilarity,
                "Results should be sorted by similarity (highest first)");
        }
    }
    
    @Test
    @Order(3)
    void testSearchWithPartialWordMatch() {
        // Given: Save multiple memories
        memorix.store(TEST_USER)
            .content("User loves pizza margherita")
            .withType("USER_PREFERENCE")
            .save();
        
        memorix.store(TEST_USER)
            .content("Pizza delivery preferred on weekends")
            .withType("USER_PREFERENCE")
            .save();
        
        memorix.store(TEST_USER)
            .content("Weather forecast shows sunny days")
            .withType("USER_PREFERENCE")
            .save();
        
        // When: Search for pizza-related content
        List<Memory> results = memorix.query(TEST_USER)
            .search("pizza preferences")
            .withType("USER_PREFERENCE")
            .limit(QueryLimit.builder()
                .maxCount(10)
                .minSimilarity(0.2f)  // Lower threshold for mock provider
                .build())
            .execute();
        
        // Then: Should find pizza memories
        assertFalse(results.isEmpty(), "Should find memories with word 'pizza'");
        assertTrue(results.size() >= 2, "Should find at least 2 pizza memories");
        
        // Verify all results contain "pizza"
        for (Memory memory : results) {
            assertTrue(memory.getContent().toLowerCase().contains("pizza"),
                "Result should be relevant: " + memory.getContent());
        }
    }
    
    @Test
    @Order(4)
    void testSearchWithSimilarityThreshold() {
        // Given: Save memories
        memorix.store(TEST_USER)
            .content("Dark mode is my favorite setting")
            .withType("USER_PREFERENCE")
            .save();
        
        memorix.store(TEST_USER)
            .content("Sunny weather makes me happy")
            .withType("USER_PREFERENCE")
            .save();
        
        // When: Search with low similarity threshold
        QueryLimit limit = QueryLimit.builder()
            .maxCount(10)
            .minSimilarity(0.2)  // Low threshold for mock provider
            .build();
        
        QueryResult result = memorix.query(TEST_USER)
            .search("dark theme")
            .withType("USER_PREFERENCE")
            .limit(limit)
            .executeWithMetadata();
        
        // Then: Should return high similarity result
        assertFalse(result.getMemories().isEmpty(), "Should find similar memories");
        assertTrue(result.getMemories().size() <= 2, "Should not return too many results");
        
        // First result should be about dark mode
        Memory firstResult = result.getMemories().get(0);
        assertTrue(firstResult.getContent().toLowerCase().contains("dark"),
            "Most similar result should contain 'dark': " + firstResult.getContent());
    }
    
    @Test
    @Order(5)
    void testSearchReturnsEmptyForCompletelyDifferentContent() {
        // Given: Save memory about dark mode
        memorix.store(TEST_USER)
            .content("I prefer dark mode in applications")
            .withType("USER_PREFERENCE")
            .save();
        
        // When: Search for completely unrelated content with high threshold
        QueryLimit limit = QueryLimit.builder()
            .maxCount(10)
            .minSimilarity(0.5)  // High threshold
            .build();
        
        List<Memory> results = memorix.query(TEST_USER)
            .search("weather sunny temperature")
            .withType("USER_PREFERENCE")
            .limit(limit)
            .execute();
        
        // Then: Should return empty (no similar content)
        assertTrue(results.isEmpty(), 
            "Unrelated search should return empty with high similarity threshold");
    }
    
    @Test
    @Order(6)
    void testSearchScenarioFromBugReport() {
        // This is the exact scenario from API_TESTING_REPORT.md
        
        // Given: Save the memory from the bug report
        Memory saved = memorix.store(TEST_USER)
            .content("I prefer dark mode in applications")
            .withType("USER_PREFERENCE")
            .withImportance(0.8f)
            .save();
        
        System.out.println("Saved memory ID: " + saved.getId());
        System.out.println("Embedding first 5 values: " + 
            formatEmbedding(saved.getEmbedding(), 5));
        
        // When: Execute the search query from bug report
        QueryResult result = memorix.query(TEST_USER)
            .search("dark theme preferences")
            .withType("USER_PREFERENCE")
            .limit(QueryLimit.builder()
                .maxCount(5)
                .minSimilarity(0.2)  // Adjusted for mock provider
                .build())
            .executeWithMetadata();
        
        // Then: Should find results (BUG #3 was: returns 0 results)
        System.out.println("Search results: " + result.getMemories().size());
        System.out.println("Total found: " + result.getMetadata().getTotalFound());
        System.out.println("Returned: " + result.getMetadata().getReturned());
        
        assertFalse(result.getMemories().isEmpty(), 
            "BUG #3 FIXED: Search should return results, was returning 0");
        assertEquals(1, result.getMemories().size(), "Should find the saved memory");
        assertEquals(saved.getId(), result.getMemories().get(0).getId());
    }
    
    @Test
    @Order(7)
    void testBulkSaveAndSearch() {
        // Given: Save 10 memories
        for (int i = 0; i < 10; i++) {
            memorix.store(TEST_USER)
                .content("Performance test memory number " + i)
                .withType("USER_PREFERENCE")
                .save();
        }
        
        // When: Search for performance-related memories
        List<Memory> results = memorix.query(TEST_USER)
            .search("performance test")
            .withType("USER_PREFERENCE")
            .limit(QueryLimit.builder()
                .maxCount(5)
                .minSimilarity(0.2)
                .build())
            .execute();
        
        // Then: Should find results (BUG #3 was: 0 results for 10 saved memories)
        assertFalse(results.isEmpty(), 
            "BUG #3 FIXED: Should find memories, was returning 0 for 10 saved memories");
        assertTrue(results.size() <= 5, "Should respect maxCount limit");
        
        // All results should contain "performance" or "test"
        for (Memory memory : results) {
            String content = memory.getContent().toLowerCase();
            assertTrue(content.contains("performance") || content.contains("test"),
                "Result should be relevant: " + memory.getContent());
        }
    }
    
    private String formatEmbedding(float[] embedding, int count) {
        if (embedding == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(count, embedding.length); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.4f", embedding[i]));
        }
        sb.append(", ...]");
        return sb.toString();
    }
}

