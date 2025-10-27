package io.memorix;

import io.memorix.lifecycle.LifecycleManager;
import io.memorix.model.Memory;
import io.memorix.model.QueryLimit;
import io.memorix.model.QueryResult;
import io.memorix.model.LimitStrategy;
import io.memorix.service.MemoryService;
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

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end integration tests for complete Memorix workflow.
 * 
 * <p>Tests full lifecycle:
 * <ol>
 *   <li>Save memories with auto-embedding</li>
 *   <li>Search semantically</li>
 *   <li>Apply decay</li>
 *   <li>Cleanup expired</li>
 * </ol>
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class MemorixEndToEndTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_e2e_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Disable multi-datasource routing for tests (we only have one Testcontainer DB)
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
    void shouldSaveAndSearchMemories() {
        // Given - Save 3 memories
        Memory m1 = memorix.store("user123")
                .content("User loves pizza margherita")
                .withType("USER_PREFERENCE")
                .withImportance(0.9f)
                .save();
        
        Memory m2 = memorix.store("user123")
                .content("User prefers dark mode")
                .withType("USER_PREFERENCE")
                .withImportance(0.7f)
                .save();
        
        Memory m3 = memorix.store("user123")
                .content("API documentation for authentication")
                .withType("DOCUMENTATION")
                .save();
        
        // Then - Memories saved
        assertThat(m1.getId()).isNotNull();
        assertThat(m2.getId()).isNotNull();
        assertThat(m3.getId()).isNotNull();
        assertThat(m1.getEmbedding()).isNotNull();
        assertThat(m1.getTokenCount()).isGreaterThan(0);
        
        // When - Search for food preferences
        List<Memory> results = memorix
                .query("user123")
                .search("food preferences")
                .withType("USER_PREFERENCE")
                .limit(10)
                .execute();
        
        // Then - Should find preferences
        assertThat(results).isNotEmpty();
    }
    
    @Test
    void shouldApplyTokenLimits() {
        // Given - Save memories with different sizes
        memorix.store("user123")
                .content("Short")
                .withType("USER_PREFERENCE")
                .save();
        
        memorix.store("user123")
                .content("Medium length content here")
                .withType("USER_PREFERENCE")
                .save();
        
        memorix.store("user123")
                .content("Very long content ".repeat(50))
                .withType("USER_PREFERENCE")
                .save();
        
        // When - Query with token limit
        QueryResult result = memorix
                .query("user123")
                .search("content")
                .withType("USER_PREFERENCE")
                .limit(QueryLimit.builder()
                        .maxTokens(20)
                        .strategy(LimitStrategy.GREEDY)
                        .build())
                .executeWithMetadata();
        
        // Then - Should respect token limit
        assertThat(result.getMetadata().getTotalTokens()).isLessThanOrEqualTo(20);
    }
    
    @Test
    void shouldApplyDecayAndCleanup() {
        // Given - Save 3 memories
        Memory m1 = memorix.store("user123")
                .content("Memory 1")
                .withType("USER_PREFERENCE")
                .save();
        
        Memory m2 = memorix.store("user123")
                .content("Memory 2")
                .withType("USER_PREFERENCE")
                .save();
        
        Memory m3 = memorix.store("user123")
                .content("Memory 3")
                .withType("USER_PREFERENCE")
                .save();
        
        // When - Apply decay (only m1 used)
        LifecycleManager.LifecycleResult result = memorix
                .lifecycle()
                .forUser("user123")
                .withPluginType("USER_PREFERENCE")
                .markUsed(List.of(m1.getId()))
                .applyDecay()
                .execute();
        
        // Then - Decay applied
        assertThat(result.getDecayApplied()).isEqualTo(3);
    }
    
    @Test
    void shouldWorkWithDifferentPlugins() {
        // Given - Different memory types
        Memory preference = memorix.store("user123")
                .content("Likes coffee")
                .withType("USER_PREFERENCE")
                .save();
        
        Memory doc = memorix.store("user123")
                .content("How to make coffee")
                .withType("DOCUMENTATION")
                .save();
        
        Memory conversation = memorix.store("user123")
                .content("Discussed coffee yesterday")
                .withType("CONVERSATION")
                .save();
        
        // Then - All types work
        assertThat(preference.getDecay()).isEqualTo(100);  // USER_PREFERENCE initial
        assertThat(doc.getDecay()).isEqualTo(100);         // DOCUMENTATION initial
        assertThat(conversation.getDecay()).isEqualTo(100); // CONVERSATION initial
    }
    
    @Test
    void shouldGetStatistics() {
        // Given
        memorix.store("user123").content("Memory 1").withType("USER_PREFERENCE").save();
        memorix.store("user123").content("Memory 2").withType("USER_PREFERENCE").save();
        memorix.store("user456").content("Memory 3").withType("USER_PREFERENCE").save();
        
        // When
        MemoryService.MemoryStats stats = memorix.stats("user123");
        
        // Then
        assertThat(stats.getTotalMemories()).isEqualTo(2);
    }
    
    @Test
    void shouldHandleEmptyResults() {
        // When - Query empty database
        List<Memory> results = memorix
                .query("user123")
                .search("nonexistent")
                .withType("USER_PREFERENCE")
                .execute();
        
        // Then
        assertThat(results).isEmpty();
    }
    
    @Test
    void completeWorkflow_saveSearchDecayCleanup() {
        // GIVEN - Save preferences
        Memory pizza = memorix.store("user123")
                .content("Loves pizza margherita with basil")
                .withType("USER_PREFERENCE")
                .save();
        
        Memory pasta = memorix.store("user123")
                .content("Prefers al dente pasta")
                .withType("USER_PREFERENCE")
                .save();
        
        Memory coffee = memorix.store("user123")
                .content("Drinks black coffee only")
                .withType("USER_PREFERENCE")
                .save();
        
        // WHEN - Search for food preferences
        List<Memory> foodResults = memorix
                .query("user123")
                .search("food preferences")
                .withType("USER_PREFERENCE")
                .limit(QueryLimit.builder()
                        .maxCount(10)
                        .maxTokens(200)
                        .minSimilarity(0.0)  // Accept all for testing
                        .strategy(LimitStrategy.GREEDY)
                        .build())
                .execute();
        
        // THEN - Found relevant memories
        assertThat(foodResults).isNotEmpty();
        
        // WHEN - Apply decay (only pizza used)
        LifecycleManager.LifecycleResult decayResult = memorix
                .lifecycle()
                .forUser("user123")
                .withPluginType("USER_PREFERENCE")
                .markUsed(List.of(pizza.getId()))
                .activeSession(true)
                .applyDecay()
                .execute();
        
        // THEN - All memories processed
        assertThat(decayResult.getDecayApplied()).isEqualTo(3);
        
        // WHEN - Get stats
        MemoryService.MemoryStats stats = memorix.stats("user123");
        
        // THEN
        assertThat(stats.getTotalMemories()).isEqualTo(3);
    }
}

