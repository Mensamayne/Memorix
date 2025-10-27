package io.memorix.storage;

import io.memorix.TestApplication;
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
 * Test database migrations.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class DatabaseMigrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("memorix_migration_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        
        // Disable multi-datasource routing for tests
        registry.add("memorix.multi-datasource.enabled", () -> "false");
    }
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    void shouldApplyMigrationsSuccessfully() {
        // Given - Flyway should have run migrations
        
        // Then - Table should exist
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'memories'",
            Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldCreatePgvectorExtension() {
        // Then - pgvector extension should be installed
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
            Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldCreateMemoriesTableWithCorrectSchema() {
        // Then - Table should have correct columns
        Integer columnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns " +
            "WHERE table_name = 'memories' AND column_name IN " +
            "('id', 'user_id', 'content', 'embedding', 'decay', 'importance', 'token_count', " +
            "'created_at', 'updated_at', 'last_accessed_at')",
            Integer.class
        );
        assertThat(columnCount).isEqualTo(10);
    }
    
    @Test
    void shouldCreateIndexes() {
        // Then - Indexes should exist
        Integer indexCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'memories'",
            Integer.class
        );
        assertThat(indexCount).isGreaterThanOrEqualTo(4);  // Primary key + 3 indexes
    }
    
    @Test
    void shouldCreateTriggers() {
        // Then - Triggers should exist
        Integer triggerCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_trigger WHERE tgrelid = 'memories'::regclass",
            Integer.class
        );
        assertThat(triggerCount).isGreaterThanOrEqualTo(2);  // token_count + updated_at
    }
}

