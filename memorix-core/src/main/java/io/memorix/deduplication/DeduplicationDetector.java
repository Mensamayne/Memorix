package io.memorix.deduplication;

import io.memorix.model.DeduplicationConfig;
import io.memorix.model.Memory;

import java.util.Optional;

/**
 * Interface for detecting duplicate memories.
 * 
 * <p>Implementations can use different strategies:
 * <ul>
 *   <li>Content hash - exact duplicate detection</li>
 *   <li>Semantic similarity - AI-based duplicate detection (future)</li>
 *   <li>Hybrid - combination of both (future)</li>
 * </ul>
 */
public interface DeduplicationDetector {
    
    /**
     * Check if a memory is a duplicate.
     * 
     * @param userId User ID to check within
     * @param content Content to check for duplicates
     * @param config Deduplication configuration
     * @return Existing memory if duplicate found, empty otherwise
     */
    Optional<Memory> findDuplicate(String userId, String content, DeduplicationConfig config);
}

