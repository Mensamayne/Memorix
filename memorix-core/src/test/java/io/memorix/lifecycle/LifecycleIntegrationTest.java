package io.memorix.lifecycle;

import io.memorix.TestApplication;
import io.memorix.api.MemoryStore;
import io.memorix.model.Memory;
import io.memorix.plugin.PluginRegistry;
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
 * Integration tests for Lifecycle system.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class LifecycleIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_lifecycle_test")
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
    private LifecycleManager lifecycleManager;
    
    @Autowired
    private DecayEngine decayEngine;
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE memories CASCADE");
    }
    
    @Test
    void shouldApplyDecayToMemories() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .decay(100)
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .decay(100)
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        
        // When
        LifecycleManager.LifecycleResult result = lifecycleManager
                .forUser("user123")
                .withPluginType("USER_PREFERENCE")
                .markUsed(List.of(memory1.getId()))  // Only memory1 used
                .applyDecay()
                .execute();
        
        // Then
        assertThat(result.getDecayApplied()).isEqualTo(2);
        
        // Memory1 should be reinforced
        Memory updated1 = memoryStore.findById(memory1.getId()).orElseThrow();
        assertThat(updated1.getDecay()).isGreaterThan(100);
        
        // Memory2 should decay
        Memory updated2 = memoryStore.findById(memory2.getId()).orElseThrow();
        assertThat(updated2.getDecay()).isLessThan(100);
    }
    
    @Test
    void shouldCleanupExpiredMemories() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Expired memory")
                .decay(0)  // Below threshold
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Alive memory")
                .decay(50)
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        
        // When
        LifecycleManager.LifecycleResult result = lifecycleManager
                .forUser("user123")
                .withPluginType("USER_PREFERENCE")
                .cleanupExpired()
                .execute();
        
        // Then
        assertThat(result.getMemoriesDeleted()).isEqualTo(1);
        assertThat(memoryStore.findById(memory1.getId())).isEmpty();  // Deleted
        assertThat(memoryStore.findById(memory2.getId())).isPresent();  // Alive
    }
    
    @Test
    void shouldCombineDecayAndCleanup() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .decay(10)  // Low decay
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .decay(100)
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        
        // When - Apply decay AND cleanup
        LifecycleManager.LifecycleResult result = lifecycleManager
                .forUser("user123")
                .withPluginType("USER_PREFERENCE")
                .markUsed(List.of())  // Nothing used
                .activeSession(true)
                .applyDecay()
                .cleanupExpired()
                .execute();
        
        // Then
        assertThat(result.getDecayApplied()).isGreaterThan(0);
        
        // memory1 might be deleted after decay
        Memory updated1 = memoryStore.findById(memory1.getId()).orElse(null);
        if (updated1 != null) {
            assertThat(updated1.getDecay()).isLessThan(10);
        }
    }
    
    @Test
    void shouldThrowExceptionWhenPluginTypeNotSpecified() {
        // When/Then
        assertThatThrownBy(() -> 
            lifecycleManager.forUser("user123")
                    .applyDecay()
                    .execute()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Plugin type must be specified");
    }
    
    @Test
    void shouldWorkWithDifferentPluginTypes() {
        // Given - DOCUMENTATION plugin (permanent)
        Memory doc = Memory.builder()
                .userId("user123")
                .content("API documentation")
                .decay(100)
                .build();
        memoryStore.save(doc);
        
        // When - Try to cleanup (should not delete permanent)
        LifecycleManager.LifecycleResult result = lifecycleManager
                .forUser("user123")
                .withPluginType("DOCUMENTATION")
                .cleanupExpired()
                .execute();
        
        // Then
        assertThat(result.getMemoriesDeleted()).isEqualTo(0);  // Nothing deleted
        assertThat(memoryStore.findById(doc.getId())).isPresent();
    }
    
    @Test
    void shouldReinforceMultipleMemories() {
        // Given
        Memory memory1 = Memory.builder()
                .userId("user123")
                .content("Memory 1")
                .decay(90)
                .build();
        
        Memory memory2 = Memory.builder()
                .userId("user123")
                .content("Memory 2")
                .decay(80)
                .build();
        
        Memory memory3 = Memory.builder()
                .userId("user123")
                .content("Memory 3")
                .decay(70)
                .build();
        
        memoryStore.save(memory1);
        memoryStore.save(memory2);
        memoryStore.save(memory3);
        
        // When - Mark 2 as used
        lifecycleManager.forUser("user123")
                .withPluginType("USER_PREFERENCE")
                .markUsed(List.of(memory1.getId(), memory2.getId()))
                .applyDecay()
                .execute();
        
        // Then
        Memory updated1 = memoryStore.findById(memory1.getId()).orElseThrow();
        Memory updated2 = memoryStore.findById(memory2.getId()).orElseThrow();
        Memory updated3 = memoryStore.findById(memory3.getId()).orElseThrow();
        
        assertThat(updated1.getDecay()).isGreaterThan(90);  // Reinforced
        assertThat(updated2.getDecay()).isGreaterThan(80);  // Reinforced
        assertThat(updated3.getDecay()).isLessThan(70);     // Decayed
    }
    
    // Helper method
    private Memory createMemory(int decay) {
        Memory memory = Memory.builder()
                .userId("user123")
                .content("Test content")
                .build();
        memory.setDecay(decay);
        return memory;
    }
}

