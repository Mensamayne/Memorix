package io.memorix.deduplication;

import io.memorix.embedding.EmbeddingProvider;
import io.memorix.model.DeduplicationConfig;
import io.memorix.model.Memory;
import io.memorix.model.MemoryWithSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Semantic deduplication detector using embedding similarity.
 * 
 * <p>Detects duplicates by comparing vector embeddings:
 * <ul>
   *   <li>Generates embedding for new content</li>
 *   <li>Queries PostgreSQL+pgvector for most similar existing memory</li>
 *   <li>Returns match if similarity exceeds threshold</li>
 * </ul>
 * 
 * <p>Benefits over hash-based:
 * <ul>
 *   <li>Detects paraphrases: "loves pizza" = "really likes pizza"</li>
 *   <li>Semantic understanding: "prefers dark mode" = "likes dark theme"</li>
 *   <li>Language variations: "speaks Polish" = "Polish language speaker"</li>
 * </ul>
 * 
 * <p>Uses HNSW index for fast similarity search (~20ms for 1000 memories).
 */
@Component
public class SemanticDeduplicationDetector implements DeduplicationDetector {
    
    private static final Logger log = LoggerFactory.getLogger(SemanticDeduplicationDetector.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingProvider embeddingProvider;
    
    public SemanticDeduplicationDetector(JdbcTemplate jdbcTemplate, 
                                        EmbeddingProvider embeddingProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingProvider = embeddingProvider;
    }
    
    @Override
    public Optional<Memory> findDuplicate(String userId, String content, DeduplicationConfig config) {
        if (content == null || content.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Generate embedding for new content
        float[] newEmbedding = embeddingProvider.embed(content);
        
        log.debug("Checking semantic duplicate: userId={}, threshold={}", 
            userId, config.getSemanticThreshold());
        
        // Find most similar memory using pgvector
        String sql = """
            SELECT id, user_id, content, embedding, decay, importance, token_count,
                   created_at, updated_at, last_accessed_at,
                   (1 - (embedding <=> ?::vector)) AS similarity
            FROM memories
            WHERE user_id = ?
              AND decay > 0
            ORDER BY embedding <=> ?::vector ASC
            LIMIT 1
            """;
        
        String vectorString = vectorToString(newEmbedding);
        
        List<MemoryWithSimilarity> results = jdbcTemplate.query(
            sql,
            new MemoryWithSimilarityRowMapper(),
            vectorString, userId, vectorString
        );
        
        if (results.isEmpty()) {
            log.debug("No semantic duplicate found (no memories exist)");
            return Optional.empty();
        }
        
        MemoryWithSimilarity closest = results.get(0);
        double threshold = config.getSemanticThreshold();
        
        if (closest.getSimilarity() >= threshold) {
            log.debug("Semantic duplicate found: id={}, similarity={:.3f} >= {:.3f}", 
                closest.getMemory().getId(), closest.getSimilarity(), threshold);
            return Optional.of(closest.getMemory());
        }
        
        log.debug("No semantic duplicate found: maxSimilarity={:.3f} < threshold={:.3f}", 
            closest.getSimilarity(), threshold);
        return Optional.empty();
    }
    
    /**
     * Convert float array to PostgreSQL vector string format.
     */
    private String vectorToString(float[] vector) {
        if (vector == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * RowMapper for Memory with similarity score.
     */
    private static class MemoryWithSimilarityRowMapper implements RowMapper<MemoryWithSimilarity> {
        @Override
        public MemoryWithSimilarity mapRow(ResultSet rs, int rowNum) throws SQLException {
            Memory memory = new Memory();
            memory.setId(rs.getString("id"));
            memory.setUserId(rs.getString("user_id"));
            memory.setContent(rs.getString("content"));
            memory.setEmbedding(getVectorFromResultSet(rs, "embedding"));
            memory.setDecay(rs.getInt("decay"));
            memory.setImportance(rs.getFloat("importance"));
            memory.setTokenCount(rs.getInt("token_count"));
            memory.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            memory.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            
            var lastAccessed = rs.getTimestamp("last_accessed_at");
            if (lastAccessed != null) {
                memory.setLastAccessedAt(lastAccessed.toLocalDateTime());
            }
            
            double similarity = rs.getDouble("similarity");
            
            return new MemoryWithSimilarity(memory, similarity);
        }
        
        private float[] getVectorFromResultSet(ResultSet rs, String columnName) throws SQLException {
            String vectorString = rs.getString(columnName);
            if (vectorString == null) {
                return null;
            }
            
            String trimmed = vectorString.substring(1, vectorString.length() - 1);
            if (trimmed.isEmpty()) {
                return new float[0];
            }
            
            String[] parts = trimmed.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        }
    }
}

