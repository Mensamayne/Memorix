package io.memorix;

import io.memorix.api.MemoryPlugin;
import io.memorix.model.Memory;
import io.memorix.model.TableSchema;
import io.memorix.plugin.PluginRegistry;
import io.memorix.service.MemoryService;
import io.memorix.storage.PluginDataSourceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.memorix.model.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for multi-datasource plugin support.
 * 
 * <p>Tests that plugins can declare their own datasource and table schema.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class MultiDataSourceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_multi_ds_test")
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
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private MemoryService memoryService;
    
    private TestCustomDataSourcePlugin testPlugin;
    
    @BeforeEach
    void setUp() {
        // Clear any existing context
        PluginDataSourceContext.clear();
        
        // Create and register test plugin programmatically
        if (!pluginRegistry.isRegistered("TEST_CUSTOM_DS")) {
            testPlugin = new TestCustomDataSourcePlugin();
            pluginRegistry.register(testPlugin);
        }
    }
    
    @AfterEach
    void tearDown() {
        PluginDataSourceContext.clear();
        
        // Cleanup: unregister test plugin
        if (pluginRegistry.isRegistered("TEST_CUSTOM_DS")) {
            pluginRegistry.unregister("TEST_CUSTOM_DS");
        }
    }
    
    @Test
    void testPluginCanDeclareDataSource() {
        // Given: A plugin that declares a custom datasource
        MemoryPlugin plugin = pluginRegistry.getByType("TEST_CUSTOM_DS");
        
        // When: Get datasource name
        String dataSourceName = plugin.getDataSourceName();
        
        // Then: Returns custom datasource
        assertEquals("custom", dataSourceName);
    }
    
    @Test
    void testPluginCanDeclareTableSchema() {
        // Given: A plugin that declares custom table schema
        MemoryPlugin plugin = pluginRegistry.getByType("TEST_CUSTOM_DS");
        
        // When: Get table schema
        TableSchema schema = plugin.getTableSchema();
        
        // Then: Returns custom schema
        assertNotNull(schema);
        assertEquals("custom_memories", schema.getTableName());
        assertEquals(1536, schema.getVectorDimension());
    }
    
    @Test
    void testDataSourceContextIsSet() {
        // Given: A custom datasource plugin
        MemoryPlugin plugin = pluginRegistry.getByType("TEST_CUSTOM_DS");
        
        // When: Save memory with that plugin
        try {
            PluginDataSourceContext.setCurrentDataSource(plugin.getDataSourceName());
            
            // Then: Context should be set
            assertEquals("custom", PluginDataSourceContext.getCurrentDataSource());
            
        } finally {
            PluginDataSourceContext.clear();
        }
    }
    
    @Test
    void testDataSourceContextIsCleared() {
        // Given: Context is set
        PluginDataSourceContext.setCurrentDataSource("custom");
        assertEquals("custom", PluginDataSourceContext.getCurrentDataSource());
        
        // When: Clear context
        PluginDataSourceContext.clear();
        
        // Then: Falls back to default
        assertEquals("default", PluginDataSourceContext.getCurrentDataSource());
    }
    
    @Test
    void testDefaultPluginUsesDefaultDataSource() {
        // Given: A standard plugin (USER_PREFERENCE)
        MemoryPlugin plugin = pluginRegistry.getByType("USER_PREFERENCE");
        
        // When: Get datasource
        String dataSourceName = plugin.getDataSourceName();
        
        // Then: Uses default
        assertEquals("default", dataSourceName);
    }
    
    @Test
    void testSaveMemoryWithCustomDataSource() {
        // Given: A plugin with custom datasource (but using default for now)
        String userId = "test-user-" + System.currentTimeMillis();
        String content = "Test memory for custom datasource";
        
        // When: Save memory
        Memory saved = memoryService.save(userId, content, "TEST_CUSTOM_DS");
        
        // Then: Memory is saved successfully
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(content, saved.getContent());
        assertEquals(userId, saved.getUserId());
    }
    
    /**
     * Test plugin with custom datasource.
     * 
     * NOTE: Registered programmatically in @BeforeEach, not via Spring
     */
    static class TestCustomDataSourcePlugin implements MemoryPlugin {
        
        @Override
        public String getType() {
            return "TEST_CUSTOM_DS";
        }
        
        @Override
        public DecayConfig getDecayConfig() {
            return DecayConfig.builder()
                    .strategyClassName("io.memorix.lifecycle.UsageBasedDecayStrategy")
                    .initialDecay(100)
                    .minDecay(0)
                    .maxDecay(150)
                    .decayReduction(5)
                    .decayReinforcement(8)
                    .autoDelete(true)
                    .affectsSearchRanking(true)
                    .build();
        }
        
        @Override
        public QueryLimit getDefaultQueryLimit() {
            return QueryLimit.builder()
                    .maxCount(20)
                    .maxTokens(500)
                    .minSimilarity(0.5)
                    .strategy(LimitStrategy.GREEDY)
                    .build();
        }
        
        @Override
        public DeduplicationConfig getDeduplicationConfig() {
            return DeduplicationConfig.disabled();
        }
        
        @Override
        public Map<String, Object> extractProperties(String memory) {
            return Map.of("test", true);
        }
        
        @Override
        public String getDataSourceName() {
            // Declare custom datasource (but will fall back to default if not configured)
            return "custom";
        }
        
        @Override
        public TableSchema getTableSchema() {
            return TableSchema.builder()
                    .tableName("custom_memories")
                    .vectorDimension(1536)
                    .addCustomColumn("custom_field VARCHAR(100)")
                    .build();
        }
    }
}

