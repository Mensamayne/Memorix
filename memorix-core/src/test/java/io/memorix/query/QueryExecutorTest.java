package io.memorix.query;

import io.memorix.TestApplication;
import io.memorix.api.MemoryStore;
import io.memorix.model.*;
import io.memorix.util.TokenCounter;
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

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for QueryExecutor.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class QueryExecutorTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_query_test")
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
    private MemoryStore memoryStore;
    
    @Autowired
    private QueryExecutor queryExecutor;
    
    @Autowired
    private TokenCounter tokenCounter;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private Random random = new Random(42);
    
    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE memories CASCADE");
    }
    
    @Test
    void shouldExecuteBasicQuery() {
        // Given
        Memory memory = createMemoryWithEmbedding("Test content", randomVector());
        memoryStore.save(memory);
        
        QueryLimit limit = QueryLimit.builder()
                .maxCount(10)
                .build();
        
        // When
        QueryResult result = queryExecutor.executeQuery(
            memory.getUserId(), 
            memory.getEmbedding(), 
            limit
        );
        
        // Then
        assertThat(result.getMemories()).hasSize(1);
        assertThat(result.getMemories().get(0).getId()).isEqualTo(memory.getId());
    }
    
    @Test
    void shouldApplyMaxCountLimit() {
        // Given - Create 5 memories
        for (int i = 0; i < 5; i++) {
            Memory memory = createMemoryWithEmbedding("Content " + i, randomVector());
            memoryStore.save(memory);
        }
        
        QueryLimit limit = QueryLimit.builder()
                .maxCount(3)
                .build();
        
        // When
        QueryResult result = queryExecutor.executeQuery("user123", randomVector(), limit);
        
        // Then
        assertThat(result.getMemories()).hasSize(3);
        assertThat(result.getMetadata().getLimitReason()).isEqualTo("maxCount");
    }
    
    @Test
    void shouldApplyTokenLimit() {
        // Given - Create memories with different sizes
        Memory small1 = createMemoryWithEmbedding("Small", randomVector());
        Memory small2 = createMemoryWithEmbedding("Small", randomVector());
        Memory large = createMemoryWithEmbedding("Large ".repeat(100), randomVector());
        
        memoryStore.save(small1);
        memoryStore.save(small2);
        memoryStore.save(large);
        
        QueryLimit limit = QueryLimit.builder()
                .maxTokens(10)  // Only room for small memories
                .build();
        
        // When
        QueryResult result = queryExecutor.executeQuery("user123", randomVector(), limit);
        
        // Then
        assertThat(result.getMemories().size()).isLessThanOrEqualTo(2);
        assertThat(result.getMetadata().getTotalTokens()).isLessThanOrEqualTo(10);
    }
    
    @Test
    void shouldFilterByDecay() {
        // Given
        Memory goodDecay = createMemoryWithEmbedding("Good", randomVector());
        goodDecay.setDecay(50);
        memoryStore.save(goodDecay);
        
        Memory badDecay = createMemoryWithEmbedding("Bad", randomVector());
        badDecay.setDecay(0);  // Below threshold
        memoryStore.save(badDecay);
        
        QueryLimit limit = QueryLimit.builder()
                .maxCount(10)
                .build();
        
        // When
        QueryResult result = queryExecutor.executeQuery("user123", randomVector(), limit);
        
        // Then - Only good decay memory returned
        assertThat(result.getMemories()).hasSize(1);
        assertThat(result.getMemories().get(0).getDecay()).isEqualTo(50);
    }
    
    private Memory createMemoryWithEmbedding(String content, float[] embedding) {
        int tokens = tokenCounter.count(content);
        
        return Memory.builder()
                .userId("user123")
                .content(content)
                .embedding(embedding)
                .decay(100)
                .importance(0.5f)
                .tokenCount(tokens)
                .build();
    }
    
    private float[] randomVector() {
        float[] vector = new float[1536];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) random.nextGaussian() * 0.1f;
        }
        
        // Normalize
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
        
        return vector;
    }
}

