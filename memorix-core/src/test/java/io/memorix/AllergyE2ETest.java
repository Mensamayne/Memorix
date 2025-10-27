package io.memorix;

import io.memorix.api.rest.MemorixController;
import io.memorix.model.Memory;
import io.memorix.model.QueryResult;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Test: Save 10 allergy-related memories and search for "allergy"
 */
@SpringBootTest(classes = io.memorix.TestApplication.class)
@Testcontainers
public class AllergyE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_allergy_test")
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
    private MemorixController controller;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE memories CASCADE");
    }
    
    @Test
    public void testAllergyMemoriesSearch() {
        String userId = "allergy-test-user-" + System.currentTimeMillis();
        
        // 1. Save 10 allergy-related memories
        String[] allergyMemories = {
            "User is allergic to peanuts and tree nuts",
            "Patient has severe shellfish allergy requiring EpiPen",
            "Allergic reaction to penicillin - use alternatives",
            "Latex allergy noted during medical examination",
            "Seasonal allergies worse in spring - uses antihistamines",
            "Dairy intolerance causes digestive issues",
            "Bee sting allergy documented - carries emergency medication",
            "Gluten sensitivity confirmed by medical tests",
            "Cat and dog dander cause respiratory symptoms",
            "Allergic to dust mites and pollen - uses air purifier"
        };
        
        System.out.println("💾 Saving 10 allergy-related memories...");
        for (String content : allergyMemories) {
            MemorixController.SaveMemoryRequest saveRequest = new MemorixController.SaveMemoryRequest();
            saveRequest.userId = userId;
            saveRequest.content = content;
            saveRequest.pluginType = "USER_PREFERENCE";
            saveRequest.importance = 0.9f;
            
            var saveResponse = controller.saveMemory(saveRequest);
            assertThat(saveResponse.getStatusCode().is2xxSuccessful()).isTrue();
            System.out.println("  ✓ Saved: " + content.substring(0, Math.min(50, content.length())) + "...");
        }
        
        // 2. Search for "allergy"
        System.out.println("\n🔍 Searching for 'allergy'...");
        MemorixController.SearchRequest searchRequest = new MemorixController.SearchRequest();
        searchRequest.userId = userId;
        searchRequest.query = "allergy";
        searchRequest.pluginType = "USER_PREFERENCE";
        searchRequest.maxCount = 10;
        searchRequest.maxTokens = 2000;
        searchRequest.minSimilarity = 0.3;
        
        var searchResponse = controller.searchMemories(searchRequest);
        assertThat(searchResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        QueryResult result = searchResponse.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getMemories()).isNotEmpty();
        
        // 3. Display results
        System.out.println("\n✅ Search results:");
        System.out.println("   Total found: " + result.getMetadata().getTotalFound());
        System.out.println("   Returned: " + result.getMetadata().getReturned());
        System.out.println("   Execution time: " + result.getMetadata().getExecutionTimeMs() + "ms");
        System.out.println("   Limit reason: " + result.getMetadata().getLimitReason());
        
        System.out.println("\n📋 Found memories:");
        for (Memory memory : result.getMemories()) {
            System.out.println("   - " + memory.getContent());
        }
        
        // 4. Verify we found relevant memories
        assertThat(result.getMemories().size()).isGreaterThan(0);
        
        // Verify at least some memories contain allergy-related terms
        long allergyCount = result.getMemories().stream()
            .filter(m -> m.getContent().toLowerCase().contains("allerg") 
                      || m.getContent().toLowerCase().contains("peanut")
                      || m.getContent().toLowerCase().contains("shellfish")
                      || m.getContent().toLowerCase().contains("penicillin"))
            .count();
        
        assertThat(allergyCount).isGreaterThan(0);
        
        System.out.println("\n🎉 TEST PASSED! Found " + allergyCount + " allergy-related memories");
    }
}

