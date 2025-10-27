package io.memorix;

import io.memorix.api.MemoryStore;
import io.memorix.model.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for BUG #1: IMPORTANCE NOT PERSISTED TO DATABASE
 * 
 * This test replicates the critical bug where:
 * - API accepts importance values (0.0, 0.5, 0.8, 0.9, 1.0)
 * - API response returns the sent importance value
 * - BUT database stores default value 0.5 for ALL records
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
public class ImportancePersistenceTest {
    
    @Autowired
    private Memorix memorix;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private MemoryStore memoryStore;
    
    @BeforeEach
    public void setUp() {
        // Clean database before each test to avoid deduplication issues
        jdbcTemplate.execute("DELETE FROM memories");
    }
    
    @Test
    public void testImportancePersistence_ShouldSaveToDatabase() {
        String userId = "importance-test-user";
        
        // Test different importance values
        testImportanceValue(userId, 0.0f, "Zero importance");
        testImportanceValue(userId, 0.3f, "Low importance");
        testImportanceValue(userId, 0.5f, "Medium importance");
        testImportanceValue(userId, 0.8f, "High importance");
        testImportanceValue(userId, 1.0f, "Critical importance");
    }
    
    private void testImportanceValue(String userId, float expectedImportance, String content) {
        // Save memory with specific importance
        Memory saved = memorix.store(userId)
                .content(content)
                .withType("USER_PREFERENCE")
                .withImportance(expectedImportance)
                .save();
        
        // Verify API response has correct importance
        assertEquals(expectedImportance, saved.getImportance(), 0.001f,
                "API response should return importance=" + expectedImportance);
        
        // Verify database has correct importance (THIS IS THE BUG!)
        Float dbImportance = jdbcTemplate.queryForObject(
                "SELECT importance FROM memories WHERE id = ?",
                Float.class,
                saved.getId()
        );
        
        assertNotNull(dbImportance, "Database should have importance value");
        assertEquals(expectedImportance, dbImportance, 0.001f,
                String.format("Database should store importance=%.1f but got %.1f", 
                        expectedImportance, dbImportance));
        
        // Verify reading from database returns correct value
        Memory reloaded = memoryStore.findById(saved.getId()).orElseThrow();
        assertEquals(expectedImportance, reloaded.getImportance(), 0.001f,
                "Reloaded memory should have importance=" + expectedImportance);
    }
    
    @Test
    public void testImportanceDefaultValue_ShouldBe0_5() {
        String userId = "importance-default-test";
        
        // Save without specifying importance
        Memory saved = memorix.store(userId)
                .content("Memory without importance specified")
                .withType("USER_PREFERENCE")
                .save();
        
        // Should default to 0.5
        assertEquals(0.5f, saved.getImportance(), 0.001f,
                "Default importance should be 0.5");
        
        Float dbImportance = jdbcTemplate.queryForObject(
                "SELECT importance FROM memories WHERE id = ?",
                Float.class,
                saved.getId()
        );
        
        assertEquals(0.5f, dbImportance, 0.001f,
                "Database should store default importance=0.5");
    }
    
    @Test
    public void testImportanceUpdate_ShouldPersist() {
        String userId = "importance-update-test";
        
        // Save with importance 0.3
        Memory saved = memorix.store(userId)
                .content("Memory to update")
                .withType("USER_PREFERENCE")
                .withImportance(0.3f)
                .save();
        
        assertEquals(0.3f, saved.getImportance(), 0.001f);
        
        // Update importance to 0.9
        saved.setImportance(0.9f);
        Memory updated = memoryStore.update(saved);
        
        assertEquals(0.9f, updated.getImportance(), 0.001f,
                "Updated memory should have new importance");
        
        // Verify in database
        Float dbImportance = jdbcTemplate.queryForObject(
                "SELECT importance FROM memories WHERE id = ?",
                Float.class,
                saved.getId()
        );
        
        assertEquals(0.9f, dbImportance, 0.001f,
                "Database should store updated importance=0.9");
    }
    
    @Test
    public void testImportanceBoundaryValues() {
        String userId = "importance-boundary-test";
        
        // Test minimum value
        Memory minMemory = memorix.store(userId)
                .content("Minimum importance")
                .withType("USER_PREFERENCE")
                .withImportance(0.0f)
                .save();
        
        Float minDbValue = jdbcTemplate.queryForObject(
                "SELECT importance FROM memories WHERE id = ?",
                Float.class,
                minMemory.getId()
        );
        assertEquals(0.0f, minDbValue, 0.001f, "Should store importance=0.0");
        
        // Test maximum value
        Memory maxMemory = memorix.store(userId)
                .content("Maximum importance")
                .withType("USER_PREFERENCE")
                .withImportance(1.0f)
                .save();
        
        Float maxDbValue = jdbcTemplate.queryForObject(
                "SELECT importance FROM memories WHERE id = ?",
                Float.class,
                maxMemory.getId()
        );
        assertEquals(1.0f, maxDbValue, 0.001f, "Should store importance=1.0");
    }
}

