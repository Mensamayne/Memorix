package io.memorix;

import io.memorix.model.Memory;
import io.memorix.model.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = io.memorix.MemorixApplication.class)
@Testcontainers
@TestPropertySource(properties = {
    "memorix.embedding.provider=mock",
    "spring.flyway.clean-disabled=false"
})
class MetadataFilteringTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_test")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private Memorix memorix;
    
    private String generateUniqueUserId(String testName) {
        return "metadata-test-" + testName + "-" + System.currentTimeMillis();
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Disable multi-datasource routing for tests (we only have one Testcontainer DB)
        registry.add("memorix.multi-datasource.enabled", () -> "false");
    }
    
    @BeforeEach
    void setUp() {
        // Tests run in isolation
    }
    
    @Test
    void shouldFilterByMetadata() {
        String testUser = generateUniqueUserId("filter-by-metadata");
        
        // Given - Save memories with different metadata
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("category", "tool");
        metadata1.put("verified", "true");
        
        Memory tool1 = memorix.store(testUser)
            .content("REST API tool for making HTTP requests")
            .withType("DOCUMENTATION")
            .withImportance(0.9f)
            .withMetadata("category", "tool")
            .withMetadata("verified", "true")
            .save();
        
        Memory tool2 = memorix.store(testUser)
            .content("Database query tool")
            .withType("DOCUMENTATION")
            .withImportance(0.8f)
            .withMetadata("category", "tool")
            .withMetadata("verified", "false")
            .save();
        
        Memory fact = memorix.store(testUser)
            .content("User prefers dark mode")
            .withType("USER_PREFERENCE")
            .withImportance(0.7f)
            .withMetadata("category", "preference")
            .save();
        
        // When - Search with metadata filter
        List<Memory> verifiedTools = memorix.query(testUser)
            .search("tool")
            .whereMetadata("category", "tool")
            .whereMetadata("verified", "true")
            .limit(10)
            .execute();
        
        // Then - Should only return verified tools
        assertThat(verifiedTools).hasSize(1);
        assertThat(verifiedTools.get(0).getId()).isEqualTo(tool1.getId());
        assertThat(verifiedTools.get(0).getMetadata().get("category")).isEqualTo("tool");
        assertThat(verifiedTools.get(0).getMetadata().get("verified")).isEqualTo("true");
    }
    
    @Test
    void shouldFilterByCategory() {
        String testUser = generateUniqueUserId("filter-by-category");
        
        // Given
        memorix.store(testUser)
            .content("PostgreSQL connection string")
            .withType("DOCUMENTATION")
            .withMetadata("category", "config")
            .save();
        
        memorix.store(testUser)
            .content("Git commands cheatsheet")
            .withType("DOCUMENTATION")
            .withMetadata("category", "reference")
            .save();
        
        // When - Search only config items
        List<Memory> configs = memorix.query(testUser)
            .search("connection")
            .whereMetadata("category", "config")
            .limit(10)
            .execute();
        
        // Then
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).getMetadata().get("category")).isEqualTo("config");
    }
    
    @Test
    void shouldWorkWithoutFilters() {
        String testUser = generateUniqueUserId("no-filters");
        
        // Given
        memorix.store(testUser)
            .content("Some memory")
            .withType("CONVERSATION")
            .save();
        
        // When - Search without filters (should work as before)
        List<Memory> results = memorix.query(testUser)
            .search("memory")
            .limit(10)
            .execute();
        
        // Then
        assertThat(results).isNotEmpty();
    }
}

