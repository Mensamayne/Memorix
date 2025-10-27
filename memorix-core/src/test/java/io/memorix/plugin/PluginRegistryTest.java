package io.memorix.plugin;

import io.memorix.api.MemoryPlugin;
import io.memorix.exception.PluginException;
import io.memorix.model.DecayConfig;
import io.memorix.model.QueryLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PluginRegistry.
 */
class PluginRegistryTest {
    
    private PluginRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new PluginRegistry();
    }
    
    @Test
    void shouldRegisterPlugin() {
        // Given
        TestPlugin plugin = new TestPlugin("TEST_TYPE");
        
        // When
        registry.register(plugin);
        
        // Then
        assertThat(registry.isRegistered("TEST_TYPE")).isTrue();
        assertThat(registry.getPluginCount()).isEqualTo(1);
    }
    
    @Test
    void shouldFindPluginByType() {
        // Given
        TestPlugin plugin = new TestPlugin("TEST_TYPE");
        registry.register(plugin);
        
        // When
        Optional<MemoryPlugin> found = registry.findByType("TEST_TYPE");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo("TEST_TYPE");
    }
    
    @Test
    void shouldReturnEmptyWhenPluginNotFound() {
        // When
        Optional<MemoryPlugin> found = registry.findByType("NON_EXISTENT");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldGetPluginByType() {
        // Given
        TestPlugin plugin = new TestPlugin("TEST_TYPE");
        registry.register(plugin);
        
        // When
        MemoryPlugin found = registry.getByType("TEST_TYPE");
        
        // Then
        assertThat(found).isNotNull();
        assertThat(found.getType()).isEqualTo("TEST_TYPE");
    }
    
    @Test
    void shouldThrowExceptionWhenPluginNotFound() {
        // When/Then
        assertThatThrownBy(() -> registry.getByType("NON_EXISTENT"))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Plugin not found for type: NON_EXISTENT");
    }
    
    @Test
    void shouldThrowExceptionWhenRegisteringNullPlugin() {
        // When/Then
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin cannot be null");
    }
    
    @Test
    void shouldThrowExceptionWhenPluginTypeNull() {
        // Given
        TestPlugin plugin = new TestPlugin(null);
        
        // When/Then
        assertThatThrownBy(() -> registry.register(plugin))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Plugin type cannot be null or empty");
    }
    
    @Test
    void shouldThrowExceptionWhenPluginTypeEmpty() {
        // Given
        TestPlugin plugin = new TestPlugin("  ");
        
        // When/Then
        assertThatThrownBy(() -> registry.register(plugin))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Plugin type cannot be null or empty");
    }
    
    @Test
    void shouldThrowExceptionWhenDuplicateType() {
        // Given
        TestPlugin plugin1 = new TestPlugin("TEST_TYPE");
        TestPlugin plugin2 = new TestPlugin("TEST_TYPE");
        registry.register(plugin1);
        
        // When/Then
        assertThatThrownBy(() -> registry.register(plugin2))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Plugin type already registered: TEST_TYPE");
    }
    
    @Test
    void shouldUnregisterPlugin() {
        // Given
        TestPlugin plugin = new TestPlugin("TEST_TYPE");
        registry.register(plugin);
        
        // When
        boolean result = registry.unregister("TEST_TYPE");
        
        // Then
        assertThat(result).isTrue();
        assertThat(registry.isRegistered("TEST_TYPE")).isFalse();
        assertThat(registry.getPluginCount()).isEqualTo(0);
    }
    
    @Test
    void shouldReturnFalseWhenUnregisteringNonExistent() {
        // When
        boolean result = registry.unregister("NON_EXISTENT");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void shouldGetRegisteredTypes() {
        // Given
        registry.register(new TestPlugin("TYPE1"));
        registry.register(new TestPlugin("TYPE2"));
        registry.register(new TestPlugin("TYPE3"));
        
        // When
        Set<String> types = registry.getRegisteredTypes();
        
        // Then
        assertThat(types).containsExactlyInAnyOrder("TYPE1", "TYPE2", "TYPE3");
    }
    
    @Test
    void shouldReturnUnmodifiableSet() {
        // Given
        registry.register(new TestPlugin("TEST_TYPE"));
        
        // When
        Set<String> types = registry.getRegisteredTypes();
        
        // Then
        assertThatThrownBy(() -> types.add("NEW_TYPE"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
    
    @Test
    void shouldClearAllPlugins() {
        // Given
        registry.register(new TestPlugin("TYPE1"));
        registry.register(new TestPlugin("TYPE2"));
        
        // When
        registry.clear();
        
        // Then
        assertThat(registry.getPluginCount()).isEqualTo(0);
        assertThat(registry.getRegisteredTypes()).isEmpty();
    }
    
    /**
     * Test plugin implementation.
     */
    private static class TestPlugin implements MemoryPlugin {
        private final String type;
        
        public TestPlugin(String type) {
            this.type = type;
        }
        
        @Override
        public String getType() {
            return type;
        }
        
        @Override
        public DecayConfig getDecayConfig() {
            return DecayConfig.builder().build();
        }
        
        @Override
        public QueryLimit getDefaultQueryLimit() {
            return QueryLimit.builder().build();
        }
        
        @Override
        public Map<String, Object> extractProperties(String memory) {
            return Map.of();
        }
    }
}

