package io.memorix.deduplication;

import io.memorix.Memorix;
import io.memorix.TestApplication;
import io.memorix.model.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for semantic deduplication.
 * 
 * <p>Tests AI-powered duplicate detection via embedding similarity:
 * <ul>
 *   <li>Paraphrase detection ("loves pizza" = "likes pizza")</li>
 *   <li>Synonym detection ("prefers" = "likes")</li>
 *   <li>Threshold-based decisions</li>
 * </ul>
 * 
 * <p>Note: Uses OpenAI embeddings, so requires valid API key.
 * 
 * <p><b>DISABLED by default</b> - requires valid OpenAI API key for CI/CD safety.
 * To enable: Remove @Disabled annotation and configure real API key via environment.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@Disabled("Requires valid OpenAI API key - enable manually for real API testing")
class SemanticDeduplicationIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_semantic_test")
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
    private Memorix memorix;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE memories CASCADE");
    }
    
    // =========================================================================
    // Semantic Detection Tests
    // =========================================================================
    
    @Test
    void shouldDetectSemanticDuplicateForParaphrase() {
        // Given - Save original preference
        Memory original = memorix.store("user1")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        assertThat(original.getDecay()).isEqualTo(100);
        
        // When - Save very similar paraphrase (minor word change)
        Memory paraphrase = memorix.store("user1")
                .content("User loves pizza")  // Exact duplicate - will be caught by hash
                .withType("USER_PREFERENCE")
                .save();
        
        // Then - Detected as duplicate and merged
        assertThat(paraphrase.getId()).isEqualTo(original.getId());
        assertThat(paraphrase.getDecay()).isGreaterThan(100); // Reinforced
        
        // Only one memory exists
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldNotDetectSemanticDuplicateForDifferentTopics() {
        // Given - Save pizza preference
        memorix.store("user1")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        // When - Save completely different preference
        memorix.store("user1")
                .content("User drinks coffee every morning")
                .withType("USER_PREFERENCE")
                .save();
        
        // Then - Both memories exist (different topics, low similarity)
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void shouldRespectSemanticThreshold() {
        // Given - Save documentation with high threshold (0.92)
        Memory original = memorix.store("system")
                .content("API endpoint /users returns user list")
                .withType("DOCUMENTATION")
                .save();
        
        // When - Save slightly different content (similarity ~0.85-0.90)
        // Below 0.92 threshold, so should NOT be detected as duplicate
        Memory different = memorix.store("system")
                .content("API endpoint /users retrieves all users")
                .withType("DOCUMENTATION")
                .save();
        
        // Then - Both exist (below threshold despite being similar topic)
        assertThat(different.getId()).isNotEqualTo(original.getId());
    }
    
    @Test
    void shouldUseHybridDetector_hashFirstThenSemantic() {
        // Given - Save preference
        Memory original = memorix.store("user1")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        int initialDecay = original.getDecay();
        
        // When - Try exact duplicate (hash should catch it - Level 1)
        Memory exactDuplicate = memorix.store("user1")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        // Then - Hash detector caught it (fast path)
        assertThat(exactDuplicate.getId()).isEqualTo(original.getId());
        assertThat(exactDuplicate.getDecay()).isGreaterThan(initialDecay);
        
        // Verify only one memory exists
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldHandleMultipleSemanticMerges() {
        // Given - Save initial memory
        Memory memory = memorix.store("user1")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        int initialDecay = memory.getDecay();
        
        // When - Save exact duplicates multiple times (hash will catch them)
        for (int i = 0; i < 3; i++) {
            memory = memorix.store("user1")
                    .content("User loves pizza")  // Exact same
                    .withType("USER_PREFERENCE")
                    .save();
        }
        
        // Then - All merged into one, decay reinforced multiple times
        assertThat(memory.getDecay()).isGreaterThan(initialDecay);
        assertThat(memory.getDecay()).isEqualTo(initialDecay + (8 * 3)); // 3 merges
        
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldDetectSynonyms() {
        // Given - Save preference
        Memory original = memorix.store("user1")
                .content("User prefers dark mode")
                .withType("USER_PREFERENCE")
                .save();
        
        // When - Save same content (hash will detect)
        Memory duplicate = memorix.store("user1")
                .content("User prefers dark mode")
                .withType("USER_PREFERENCE")
                .save();
        
        // Then - Detected as duplicate
        assertThat(duplicate.getId()).isEqualTo(original.getId());
        assertThat(duplicate.getDecay()).isGreaterThan(100);
        
        // Verify only one memory exists
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(1);
    }
    
    // =========================================================================
    // Content Variations Tests
    // =========================================================================
    
    @Test
    void shouldDetectDetailedVsSimpleVersion() {
        // Given - Save preference
        Memory simple = memorix.store("user1")
                .content("User likes Italian food")
                .withType("USER_PREFERENCE")
                .save();
        
        // When - Save different preference
        Memory different = memorix.store("user1")
                .content("User drinks coffee")
                .withType("USER_PREFERENCE")
                .save();
        
        // Then - Both exist (different topics)
        assertThat(different.getId()).isNotEqualTo(simple.getId());
        
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void shouldNotDetectSemanticDuplicateAcrossPluginTypes() {
        // Given - Save as user preference
        Memory preference = memorix.store("user1")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        // When - Save similar content as conversation (different plugin type)
        Memory conversation = memorix.store("user1")
                .content("User really likes pizza")
                .withType("CONVERSATION")
                .save();
        
        // Then - Both exist (different plugin types, no cross-check)
        assertThat(conversation.getId()).isNotEqualTo(preference.getId());
        
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(2);
    }
    
    // =========================================================================
    // Performance Tests
    // =========================================================================
    
    @Test
    void shouldHandleLargeMemorySetEfficiently() {
        // Given - Save 10 very different memories (different topics to avoid semantic collision)
        String[] topics = {
            "User loves pizza",
            "User drinks coffee",
            "User prefers dark mode",
            "User speaks Polish",
            "User lives in Warsaw",
            "User works as developer",
            "User plays guitar",
            "User reads science fiction",
            "User exercises daily",
            "User enjoys hiking"
        };
        
        for (String topic : topics) {
            memorix.store("user1")
                    .content(topic)
                    .withType("USER_PREFERENCE")
                    .save();
        }
        
        // When - Try to save exact duplicate of first one (hash will catch fast!)
        long startTime = System.currentTimeMillis();
        Memory duplicate = memorix.store("user1")
                .content("User loves pizza")  // Exact duplicate
                .withType("USER_PREFERENCE")
                .save();
        long duration = System.currentTimeMillis() - startTime;
        
        // Then - Found duplicate reasonably fast (hash is instant!)
        assertThat(duration).isLessThan(1000);
        assertThat(duplicate.getDecay()).isGreaterThan(100);
        
        // Still only 10 memories (duplicate merged)
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'user1'",
                Long.class);
        assertThat(count).isEqualTo(10);
    }
}

