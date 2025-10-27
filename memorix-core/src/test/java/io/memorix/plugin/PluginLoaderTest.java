package io.memorix.plugin;

import io.memorix.TestApplication;
import io.memorix.api.MemoryPlugin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PluginLoader.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class PluginLoaderTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_plugin_test")
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
    private PluginLoader pluginLoader;
    
    @Autowired
    private PluginRegistry registry;
    
    @Test
    void shouldLoadPluginsOnStartup() {
        // Then - Plugins should be auto-loaded by @PostConstruct
        assertThat(registry.getPluginCount()).isGreaterThan(0);
    }
    
    @Test
    void shouldLoadExamplePlugins() {
        // Then - Example plugins should be registered
        assertThat(registry.isRegistered("USER_PREFERENCE")).isTrue();
        assertThat(registry.isRegistered("DOCUMENTATION")).isTrue();
        assertThat(registry.isRegistered("CONVERSATION")).isTrue();
    }
    
    @Test
    void shouldReloadPlugins() {
        // Given
        int initialCount = registry.getPluginCount();
        
        // When
        pluginLoader.reload();
        
        // Then - Same count after reload
        assertThat(registry.getPluginCount()).isEqualTo(initialCount);
    }
    
    @Test
    void shouldGetUserPreferencePlugin() {
        // When
        MemoryPlugin plugin = registry.getByType("USER_PREFERENCE");
        
        // Then
        assertThat(plugin).isNotNull();
        assertThat(plugin.getType()).isEqualTo("USER_PREFERENCE");
        
        // Check decay config
        var decayConfig = plugin.getDecayConfig();
        assertThat(decayConfig.getMaxDecay()).isEqualTo(200);
        assertThat(decayConfig.getDecayReduction()).isEqualTo(3);
        assertThat(decayConfig.getDecayReinforcement()).isEqualTo(8);
        assertThat(decayConfig.isAutoDelete()).isTrue();
        assertThat(decayConfig.affectsSearchRanking()).isTrue();
    }
    
    @Test
    void shouldGetDocumentationPlugin() {
        // When
        MemoryPlugin plugin = registry.getByType("DOCUMENTATION");
        
        // Then
        assertThat(plugin).isNotNull();
        assertThat(plugin.getType()).isEqualTo("DOCUMENTATION");
        
        // Check decay config (permanent)
        var decayConfig = plugin.getDecayConfig();
        assertThat(decayConfig.getMinDecay()).isEqualTo(100);
        assertThat(decayConfig.getMaxDecay()).isEqualTo(100);
        assertThat(decayConfig.getDecayReduction()).isEqualTo(0);
        assertThat(decayConfig.isAutoDelete()).isFalse();
        assertThat(decayConfig.affectsSearchRanking()).isFalse();
    }
    
    @Test
    void shouldGetConversationPlugin() {
        // When
        MemoryPlugin plugin = registry.getByType("CONVERSATION");
        
        // Then
        assertThat(plugin).isNotNull();
        assertThat(plugin.getType()).isEqualTo("CONVERSATION");
        
        // Check decay config (hybrid)
        var decayConfig = plugin.getDecayConfig();
        assertThat(decayConfig.getMaxDecay()).isEqualTo(150);
        assertThat(decayConfig.getStrategyParams())
                .containsEntry("timeFactor", 0.3)
                .containsEntry("usageFactor", 0.7)
                .containsEntry("inactivityThreshold", 30);
    }
    
    @Test
    void shouldExtractPropertiesFromPlugins() {
        // When
        var prefPlugin = registry.getByType("USER_PREFERENCE");
        var docPlugin = registry.getByType("DOCUMENTATION");
        var convPlugin = registry.getByType("CONVERSATION");
        
        // Then
        assertThat(prefPlugin.extractProperties("test"))
                .containsEntry("category", "preference");
        
        assertThat(docPlugin.extractProperties("test"))
                .containsEntry("permanent", true);
        
        assertThat(convPlugin.extractProperties("test"))
                .containsEntry("temporal", true);
    }
}

