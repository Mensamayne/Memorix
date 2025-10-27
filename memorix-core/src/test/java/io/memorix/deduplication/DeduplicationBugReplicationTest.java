package io.memorix.deduplication;

import io.memorix.Memorix;
import io.memorix.TestApplication;
import io.memorix.model.Memory;
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

import static org.assertj.core.api.Assertions.*;

/**
 * Test replikujący bug deduplikacji znaleziony podczas testowania curl.
 * 
 * <p>Bug: Dwukrotny zapis identycznej pamięci tworzy dwa osobne rekordy
 * zamiast wykryć duplikat i zastosować strategię MERGE.
 * 
 * <p>Test case:
 * <pre>{@code
 * POST /api/memorix/memories
 * {
 *   "userId": "testuser",
 *   "content": "User loves pizza",
 *   "pluginType": "USER_PREFERENCE",
 *   "importance": 0.8
 * }
 * 
 * // Drugi identyczny request
 * POST /api/memorix/memories  // <-- Powinien zmergować, a tworzy nowy!
 * {
 *   "userId": "testuser",
 *   "content": "User loves pizza",
 *   "pluginType": "USER_PREFERENCE",
 *   "importance": 0.8
 * }
 * }</pre>
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class DeduplicationBugReplicationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_bug_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
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
    void shouldDetectDuplicateAndMerge_notCreateSecondMemory() {
        // Given - Pierwszy zapis pamięci (dokładnie jak w curl)
        Memory first = memorix.store("testuser")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .withImportance(0.8f)
                .save();
        
        assertThat(first).isNotNull();
        assertThat(first.getId()).isNotNull();
        assertThat(first.getDecay()).isEqualTo(130); // Początkowy decay (importance 0.8 scales to 130)
        
        System.out.println("✅ First memory saved: id=" + first.getId() + ", decay=" + first.getDecay());
        
        // When - Drugi IDENTYCZNY zapis (ten sam content, userId, pluginType)
        Memory second = memorix.store("testuser")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .withImportance(0.8f)
                .save();
        
        System.out.println("✅ Second memory saved: id=" + second.getId() + ", decay=" + second.getDecay());
        
        // Then - Powinien zwrócić TEN SAM rekord (merge)
        assertThat(second.getId())
                .as("Deduplikacja powinna zwrócić ten sam ID, nie tworzyć nowego rekordu")
                .isEqualTo(first.getId());
        
        assertThat(second.getDecay())
                .as("Decay powinien być wzmocniony (reinforced) po merge")
                .isGreaterThan(first.getDecay())
                .isEqualTo(138); // 130 + 8 (reinforcement z USER_PREFERENCE config)
        
        // Weryfikacja w bazie danych - powinien być TYLKO 1 rekord
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memories WHERE user_id = 'testuser' AND content = 'User loves pizza'",
                Long.class);
        
        assertThat(count)
                .as("W bazie powinien być tylko 1 rekord, nie 2!")
                .isEqualTo(1);
        
        // Sprawdzenie czy content_hash jest zapisany
        String contentHash = jdbcTemplate.queryForObject(
                "SELECT content_hash FROM memories WHERE id = ?",
                String.class,
                first.getId());
        
        assertThat(contentHash)
                .as("content_hash powinien być zapisany w bazie danych")
                .isNotNull()
                .isNotEmpty();
        
        System.out.println("✅ BUG NAPRAWIONY! Tylko 1 rekord w bazie, content_hash=" + contentHash);
    }
    
    @Test
    void shouldStoreContentHashInDatabase() {
        // Given - Zapisz pamięć
        Memory memory = memorix.store("testuser")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        // When - Pobierz content_hash z bazy
        String contentHash = jdbcTemplate.queryForObject(
                "SELECT content_hash FROM memories WHERE id = ?",
                String.class,
                memory.getId());
        
        // Then - content_hash powinien być zapisany
        assertThat(contentHash)
                .as("content_hash musi być zapisany podczas save()")
                .isNotNull()
                .isNotEmpty()
                .hasSize(64); // SHA-256 = 64 hex chars
        
        // Drugi zapis tej samej treści powinien mieć ten sam hash
        Memory duplicate = memorix.store("testuser")
                .content("User loves pizza")
                .withType("USER_PREFERENCE")
                .save();
        
        String duplicateHash = jdbcTemplate.queryForObject(
                "SELECT content_hash FROM memories WHERE id = ?",
                String.class,
                duplicate.getId());
        
        assertThat(duplicateHash)
                .as("Duplikat powinien mieć ten sam content_hash")
                .isEqualTo(contentHash);
    }
}

