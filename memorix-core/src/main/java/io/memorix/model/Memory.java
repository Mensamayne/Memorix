package io.memorix.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Core memory object.
 * 
 * <p>Represents a single memory with:
 * <ul>
 *   <li>Content (text)</li>
 *   <li>Embedding (vector)</li>
 *   <li>Decay (strength)</li>
 *   <li>Metadata (custom fields)</li>
 *   <li>Timestamps</li>
 * </ul>
 */
public class Memory {
    
    private String id;
    private String userId;
    private String content;
    private String contentHash; // SHA-256 hash for deduplication
    private float[] embedding;
    private int decay;
    private float importance;
    private int tokenCount;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessedAt;
    
    public Memory() {
        this.metadata = new HashMap<>();
        this.decay = 100;
        this.importance = 0.5f;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final Memory memory = new Memory();
        
        public Builder id(String id) {
            memory.id = id;
            return this;
        }
        
        public Builder userId(String userId) {
            memory.userId = userId;
            return this;
        }
        
        public Builder content(String content) {
            memory.content = content;
            return this;
        }
        
        public Builder contentHash(String contentHash) {
            memory.contentHash = contentHash;
            return this;
        }
        
        public Builder embedding(float[] embedding) {
            memory.embedding = embedding;
            return this;
        }
        
        public Builder decay(int decay) {
            memory.decay = decay;
            return this;
        }
        
        public Builder importance(float importance) {
            memory.importance = importance;
            return this;
        }
        
        public Builder tokenCount(int tokenCount) {
            memory.tokenCount = tokenCount;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            memory.metadata = new HashMap<>(metadata);
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            memory.metadata.put(key, value);
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            memory.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(LocalDateTime updatedAt) {
            memory.updatedAt = updatedAt;
            return this;
        }
        
        public Builder lastAccessedAt(LocalDateTime lastAccessedAt) {
            memory.lastAccessedAt = lastAccessedAt;
            return this;
        }
        
        public Memory build() {
            Objects.requireNonNull(memory.userId, "userId cannot be null");
            Objects.requireNonNull(memory.content, "content cannot be null");
            return memory;
        }
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContentHash() {
        return contentHash;
    }
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public float[] getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
    
    public int getDecay() {
        return decay;
    }
    
    public void setDecay(int decay) {
        this.decay = decay;
    }
    
    public float getImportance() {
        return importance;
    }
    
    public void setImportance(float importance) {
        this.importance = importance;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Memory memory = (Memory) o;
        return Objects.equals(id, memory.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Memory{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", decay=" + decay +
                ", importance=" + importance +
                ", tokenCount=" + tokenCount +
                '}';
    }
}

