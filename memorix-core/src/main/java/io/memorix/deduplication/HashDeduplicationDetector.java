package io.memorix.deduplication;

import io.memorix.api.MemoryStore;
import io.memorix.model.DeduplicationConfig;
import io.memorix.model.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Hash-based deduplication detector.
 * 
 * <p>Detects exact duplicates by comparing content hashes.
 * Fast and deterministic - same content always produces same hash.
 * 
 * <p>Process:
 * <ol>
 *   <li>Generate hash from new content (with optional normalization)</li>
 *   <li>Query database for memories with matching hash</li>
 *   <li>Return first match (exact duplicate)</li>
 * </ol>
 */
@Component
public class HashDeduplicationDetector implements DeduplicationDetector {
    
    private static final Logger log = LoggerFactory.getLogger(HashDeduplicationDetector.class);
    
    private final MemoryStore memoryStore;
    
    public HashDeduplicationDetector(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }
    
    @Override
    public Optional<Memory> findDuplicate(String userId, String content, DeduplicationConfig config) {
        if (content == null || content.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Generate hash
        String contentHash = ContentHashGenerator.generateHash(content, config.isNormalizeContent());
        
        log.debug("Checking for duplicate: userId={}, hash={}", userId, contentHash);
        
        // Find memories with same hash
        List<Memory> allMemories = memoryStore.findByUserId(userId);
        
        for (Memory memory : allMemories) {
            String existingHash = ContentHashGenerator.generateHash(
                memory.getContent(), 
                config.isNormalizeContent()
            );
            
            if (contentHash.equals(existingHash)) {
                log.debug("Duplicate found: existingId={}, content={}", 
                    memory.getId(), memory.getContent());
                return Optional.of(memory);
            }
        }
        
        log.debug("No duplicate found");
        return Optional.empty();
    }
}

