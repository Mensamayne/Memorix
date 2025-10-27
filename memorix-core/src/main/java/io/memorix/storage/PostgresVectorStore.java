package io.memorix.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.memorix.api.MemoryStore;
import io.memorix.exception.ErrorCode;
import io.memorix.exception.StorageException;
import io.memorix.model.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL implementation of MemoryStore.
 * 
 * <p>Uses pgvector for vector similarity search and JDBC for operations.
 */
@Repository
public class PostgresVectorStore implements MemoryStore {
    
    private static final Logger log = LoggerFactory.getLogger(PostgresVectorStore.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final JdbcTemplate jdbcTemplate;
    private final MemoryRowMapper rowMapper;
    
    public PostgresVectorStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new MemoryRowMapper();
    }
    
    @Override
    @Transactional
    public Memory save(Memory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory cannot be null");
        }
        if (memory.getUserId() == null || memory.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("Memory must have userId");
        }
        if (memory.getContent() == null || memory.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Memory must have content");
        }
        
        try {
            String id = UUID.randomUUID().toString();
            memory.setId(id);
            
            // Convert metadata to JSON
            String metadataJson = metadataToJson(memory.getMetadata());
            
            String sql = "INSERT INTO memories " +
                        "(id, user_id, content, content_hash, embedding, decay, importance, metadata, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?::vector, ?, ?, ?::jsonb, ?, ?)";
            
            jdbcTemplate.update(sql,
                id,
                memory.getUserId(),
                memory.getContent(),
                memory.getContentHash(),
                vectorToString(memory.getEmbedding()),
                memory.getDecay(),
                memory.getImportance(),
                metadataJson,
                Timestamp.valueOf(memory.getCreatedAt()),
                Timestamp.valueOf(memory.getUpdatedAt())
            );
            
            log.debug("Saved memory: {} with metadata", id);
            return findById(id).orElseThrow();
            
        } catch (Exception e) {
            throw new StorageException(ErrorCode.SAVE_FAILED,
                "Failed to save memory: " + e.getMessage(), e)
                .withContext("userId", memory.getUserId());
        }
    }
    
    @Override
    @Transactional
    public Memory update(Memory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory cannot be null");
        }
        if (memory.getId() == null || memory.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Memory must have ID for update");
        }
        
        try {
            // Convert metadata to JSON
            String metadataJson = metadataToJson(memory.getMetadata());
            
            String sql = "UPDATE memories SET " +
                        "content = ?, content_hash = ?, embedding = ?::vector, decay = ?, " +
                        "importance = ?, metadata = ?::jsonb, updated_at = ? " +
                        "WHERE id = ?";
            
            int updated = jdbcTemplate.update(sql,
                memory.getContent(),
                memory.getContentHash(),
                vectorToString(memory.getEmbedding()),
                memory.getDecay(),
                memory.getImportance(),
                metadataJson,
                Timestamp.valueOf(LocalDateTime.now()),
                memory.getId()
            );
            
            if (updated == 0) {
                throw new StorageException(ErrorCode.UPDATE_FAILED,
                    "Memory not found: " + memory.getId());
            }

            log.debug("Updated memory: {} with metadata", memory.getId());
            // Return updated memory from database to get accurate state
            return findById(memory.getId()).orElseThrow();

        } catch (Exception e) {
            throw new StorageException(ErrorCode.UPDATE_FAILED,
                "Failed to update memory: " + e.getMessage(), e)
                .withContext("memoryId", memory.getId());
        }
    }
    
    @Override
    @Transactional
    public void delete(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
        
        try {
            String sql = "DELETE FROM memories WHERE id = ?";
            int deleted = jdbcTemplate.update(sql, memoryId);
            
            if (deleted == 0) {
                log.warn("Memory not found for deletion: {}", memoryId);
            } else {
                log.debug("Deleted memory: {}", memoryId);
            }
            
        } catch (Exception e) {
            throw new StorageException(ErrorCode.DELETE_FAILED,
                "Failed to delete memory: " + e.getMessage(), e)
                .withContext("memoryId", memoryId);
        }
    }
    
    @Override
    @Transactional
    public int deleteByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        try {
            String sql = "DELETE FROM memories WHERE user_id = ?";
            int deleted = jdbcTemplate.update(sql, userId);
            
            log.debug("Deleted {} memories for user: {}", deleted, userId);
            return deleted;
            
        } catch (Exception e) {
            throw new StorageException(ErrorCode.DELETE_FAILED,
                "Failed to delete memories for user: " + e.getMessage(), e)
                .withContext("userId", userId);
        }
    }
    
    @Override
    public Optional<Memory> findById(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            String sql = "SELECT * FROM memories WHERE id = ?";
            List<Memory> results = jdbcTemplate.query(sql, rowMapper, memoryId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            
        } catch (Exception e) {
            throw new StorageException(ErrorCode.QUERY_FAILED,
                "Failed to find memory by ID: " + e.getMessage(), e)
                .withContext("memoryId", memoryId);
        }
    }
    
    @Override
    public List<Memory> findByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        try {
            String sql = "SELECT * FROM memories WHERE user_id = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, rowMapper, userId);
            
        } catch (Exception e) {
            throw new StorageException(ErrorCode.QUERY_FAILED,
                "Failed to find memories by user ID: " + e.getMessage(), e)
                .withContext("userId", userId);
        }
    }
    
    @Override
    public List<Memory> findByUserIdAndDecayAbove(String userId, int minDecay) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        try {
            String sql = "SELECT * FROM memories WHERE user_id = ? AND decay > ? ORDER BY decay DESC";
            return jdbcTemplate.query(sql, rowMapper, userId, minDecay);
            
        } catch (Exception e) {
            throw new StorageException(ErrorCode.QUERY_FAILED,
                "Failed to find memories by user ID and decay: " + e.getMessage(), e)
                .withContext("userId", userId)
                .withContext("minDecay", minDecay);
        }
    }
    
    @Override
    public long countByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        try {
            String sql = "SELECT COUNT(*) FROM memories WHERE user_id = ?";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
            return count != null ? count : 0;
            
        } catch (Exception e) {
            throw new StorageException(ErrorCode.QUERY_FAILED,
                "Failed to count memories for user: " + e.getMessage(), e)
                .withContext("userId", userId);
        }
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
     * RowMapper for Memory objects.
     */
    private static class MemoryRowMapper implements RowMapper<Memory> {
        @Override
        public Memory mapRow(ResultSet rs, int rowNum) throws SQLException {
            Memory memory = new Memory();
            memory.setId(rs.getString("id"));
            memory.setUserId(rs.getString("user_id"));
            memory.setContent(rs.getString("content"));
            memory.setContentHash(rs.getString("content_hash"));
            memory.setEmbedding(getVectorFromResultSet(rs, "embedding"));
            memory.setDecay(rs.getInt("decay"));
            memory.setImportance(rs.getFloat("importance"));
            memory.setTokenCount(rs.getInt("token_count"));
            memory.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            memory.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            
            Timestamp lastAccessed = rs.getTimestamp("last_accessed_at");
            if (lastAccessed != null) {
                memory.setLastAccessedAt(lastAccessed.toLocalDateTime());
            }
            
            // Parse metadata from JSONB
            String metadataJson = rs.getString("metadata");
            memory.setMetadata(jsonToMetadata(metadataJson));
            
            return memory;
        }
        
        private float[] getVectorFromResultSet(ResultSet rs, String columnName) throws SQLException {
            String vectorString = rs.getString(columnName);
            if (vectorString == null) {
                return null;
            }
            
            // Parse pgvector format: "[0.1,0.2,0.3]"
            String trimmed = vectorString.substring(1, vectorString.length() - 1);  // Remove [ ]
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
    
    /**
     * Convert metadata Map to JSON string for JSONB storage.
     * 
     * @param metadata Metadata map
     * @return JSON string
     */
    private static String metadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata to JSON, using empty object: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Convert JSON string from JSONB to metadata Map.
     * 
     * @param json JSON string
     * @return Metadata map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonToMetadata(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("{}")) {
            return new HashMap<>();
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata JSON, using empty map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}

