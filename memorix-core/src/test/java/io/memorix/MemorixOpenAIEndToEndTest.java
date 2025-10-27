package io.memorix;

import io.memorix.api.rest.MemorixController;
import io.memorix.model.Memory;
import io.memorix.model.QueryResult;
import io.memorix.model.QueryResult.QueryMetadata;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End test with OpenAI API and Testcontainers.
 * 
 * <p>This test uses:
 * - Real Spring Boot test context
 * - Testcontainers PostgreSQL (automatic)
 * - Real OpenAI API for embeddings
 * - Real semantic search
 * 
 * <p><b>DISABLED by default</b> - requires valid OpenAI API key for CI/CD safety.
 * To enable: Remove @Disabled annotation and configure real API key via environment.
 */
@SpringBootTest(classes = io.memorix.TestApplication.class)
@Testcontainers
@Disabled("Requires valid OpenAI API key - enable manually for real API testing")
public class MemorixOpenAIEndToEndTest {
    
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
    public void testRealSemanticSearchWithOpenAI() {
        String userId = "test-user-" + System.currentTimeMillis();
        
        // 1. Save a memory about user preferences
        MemorixController.SaveMemoryRequest saveRequest = new MemorixController.SaveMemoryRequest();
        saveRequest.userId = userId;
        saveRequest.content = "User prefers Italian food, especially pizza and pasta";
        saveRequest.pluginType = "USER_PREFERENCE";
        saveRequest.importance = 0.9f;
        
        var saveResponse = controller.saveMemory(saveRequest);
        assertThat(saveResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        Object responseBody = saveResponse.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).isInstanceOf(Memory.class);
        
        Memory savedMemory = (Memory) responseBody;
        assertThat(savedMemory.getUserId()).isEqualTo(userId);
        assertThat(savedMemory.getContent()).contains("Italian food");
        System.out.println("✅ Saved memory: " + savedMemory.getId());
        
        // 2. Search with related but different wording
        MemorixController.SearchRequest searchRequest = new MemorixController.SearchRequest();
        searchRequest.userId = userId;
        searchRequest.query = "What does this user like to eat?";  // Different wording!
        searchRequest.pluginType = "USER_PREFERENCE";
        searchRequest.maxCount = 10;
        searchRequest.maxTokens = 1000;
        searchRequest.minSimilarity = 0.3;  // Lower threshold for testing
        searchRequest.strategy = io.memorix.model.LimitStrategy.GREEDY;
        
        var searchResponse = controller.searchMemories(searchRequest);
        assertThat(searchResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        QueryResult result = searchResponse.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getMemories()).isNotEmpty();
        
        Memory foundMemory = result.getMemories().get(0);
        assertThat(foundMemory.getContent()).contains("Italian food");
        
        QueryMetadata metadata = result.getMetadata();
        System.out.println("✅ Search results:");
        System.out.println("   Total found: " + metadata.getTotalFound());
        System.out.println("   Returned: " + metadata.getReturned());
        System.out.println("   Execution time: " + metadata.getExecutionTimeMs() + "ms");
        System.out.println("   Limited by: " + metadata.getLimitReason());
        
        // 3. Verify semantic similarity worked
        assertThat(metadata.getTotalFound()).isGreaterThan(0);
        assertThat(metadata.getReturned()).isGreaterThan(0);
        assertThat(foundMemory.getContent()).contains("Italian");
    }
    
    @Test
    public void testMultipleMemoriesAndComplexSearch() {
        String userId = "complex-user-" + System.currentTimeMillis();
        
        // Save multiple related memories
        String[] memories = {
            "User works as a software engineer at Google",
            "User lives in San Francisco and loves the tech scene",
            "User enjoys hiking in the mountains on weekends",
            "User prefers vegetarian food and sustainable living"
        };
        
        for (String content : memories) {
            MemorixController.SaveMemoryRequest saveRequest = new MemorixController.SaveMemoryRequest();
            saveRequest.userId = userId;
            saveRequest.content = content;
            saveRequest.pluginType = "USER_PREFERENCE";
            saveRequest.importance = 0.8f;
            
            var response = controller.saveMemory(saveRequest);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
        
        System.out.println("✅ Saved " + memories.length + " memories for user: " + userId);
        
        // Search for work-related info
        MemorixController.SearchRequest workSearch = new MemorixController.SearchRequest();
        workSearch.userId = userId;
        workSearch.query = "What is the user's profession and workplace?";
        workSearch.pluginType = "USER_PREFERENCE";
        workSearch.maxCount = 5;
        workSearch.maxTokens = 1000;
        workSearch.minSimilarity = 0.2;
        
        var workResponse = controller.searchMemories(workSearch);
        assertThat(workResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        QueryResult workResult = workResponse.getBody();
        assertThat(workResult.getMemories()).isNotEmpty();
        
        // Should find the Google engineer memory
        boolean foundWorkMemory = workResult.getMemories().stream()
            .anyMatch(m -> m.getContent().contains("Google") || m.getContent().contains("software engineer"));
        assertThat(foundWorkMemory).isTrue();
        
        System.out.println("✅ Work search found " + workResult.getMemories().size() + " relevant memories");
        
        // Search for lifestyle info
        MemorixController.SearchRequest lifestyleSearch = new MemorixController.SearchRequest();
        lifestyleSearch.userId = userId;
        lifestyleSearch.query = "What are the user's hobbies and lifestyle preferences?";
        lifestyleSearch.pluginType = "USER_PREFERENCE";
        lifestyleSearch.maxCount = 5;
        lifestyleSearch.maxTokens = 1000;
        lifestyleSearch.minSimilarity = 0.2;
        
        var lifestyleResponse = controller.searchMemories(lifestyleSearch);
        assertThat(lifestyleResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        QueryResult lifestyleResult = lifestyleResponse.getBody();
        assertThat(lifestyleResult.getMemories()).isNotEmpty();
        
        // Should find hiking or food preferences
        boolean foundLifestyleMemory = lifestyleResult.getMemories().stream()
            .anyMatch(m -> m.getContent().contains("hiking") || 
                          m.getContent().contains("vegetarian") || 
                          m.getContent().contains("sustainable"));
        assertThat(foundLifestyleMemory).isTrue();
        
        System.out.println("✅ Lifestyle search found " + lifestyleResult.getMemories().size() + " relevant memories");
        
        // Verify different searches return different results
        assertThat(workResult.getMemories()).isNotEqualTo(lifestyleResult.getMemories());
    }
    
    @Test
    public void testStatsEndpoint() {
        String userId = "stats-user-" + System.currentTimeMillis();
        
        // Save memories with DIVERSE content to avoid semantic deduplication
        String[] diverseContents = {
            "User prefers Italian food like pizza and pasta",
            "User works as software engineer in San Francisco",
            "User enjoys mountain hiking on sunny weekends"
        };
        
        for (String content : diverseContents) {
            MemorixController.SaveMemoryRequest saveRequest = new MemorixController.SaveMemoryRequest();
            saveRequest.userId = userId;
            saveRequest.content = content;
            saveRequest.pluginType = "USER_PREFERENCE";
            saveRequest.importance = 0.7f;
            
            controller.saveMemory(saveRequest);
        }
        
        // Get stats
        var statsResponse = controller.getStats(userId);
        assertThat(statsResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        var stats = statsResponse.getBody();
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalMemories()).isEqualTo(3);
        
        System.out.println("✅ Stats test passed - found " + stats.getTotalMemories() + " memories");
    }
    
    @Test
    public void testPluginInfoEndpoint() {
        // Get available plugins
        var pluginsResponse = controller.getPlugins();
        assertThat(pluginsResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        var plugins = pluginsResponse.getBody();
        assertThat(plugins).isNotEmpty();
        assertThat(plugins).contains("USER_PREFERENCE");
        
        System.out.println("✅ Available plugins: " + plugins);
        
        // Get plugin info
        var pluginInfoResponse = controller.getPluginInfo("USER_PREFERENCE");
        assertThat(pluginInfoResponse.getStatusCode().is2xxSuccessful()).isTrue();
        
        var pluginInfo = pluginInfoResponse.getBody();
        assertThat(pluginInfo).isNotNull();
        assertThat(pluginInfo).containsKey("type");
        assertThat(pluginInfo).containsKey("decayConfig");
        assertThat(pluginInfo).containsKey("defaultQueryLimit");
        
        System.out.println("✅ Plugin info for USER_PREFERENCE: " + pluginInfo);
    }
}
