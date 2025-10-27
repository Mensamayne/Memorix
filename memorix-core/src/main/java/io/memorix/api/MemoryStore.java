package io.memorix.api;

import io.memorix.model.Memory;
import java.util.List;
import java.util.Optional;

/**
 * Main interface for memory storage operations.
 * 
 * <p>Provides CRUD operations for memories:
 * <ul>
 *   <li>Save memory (with embedding generation)</li>
 *   <li>Update memory</li>
 *   <li>Delete memory</li>
 *   <li>Find memories by various criteria</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * Memory memory = Memory.builder()
 *     .userId("user123")
 *     .content("User loves pizza")
 *     .importance(0.8f)
 *     .build();
 * 
 * Memory saved = memoryStore.save(memory);
 * }</pre>
 */
public interface MemoryStore {
    
    /**
     * Save a new memory.
     * 
     * <p>Automatically generates embedding if not provided.
     * 
     * @param memory Memory to save (must have userId and content)
     * @return Saved memory with generated ID and embedding
     * @throws IllegalArgumentException if memory is null or invalid
     * @throws io.memorix.exception.StorageException if save fails
     * @throws io.memorix.exception.EmbeddingException if embedding generation fails
     */
    Memory save(Memory memory);
    
    /**
     * Update an existing memory.
     * 
     * <p>Updates content, metadata, and regenerates embedding if content changed.
     * 
     * @param memory Memory to update (must have ID)
     * @return Updated memory
     * @throws IllegalArgumentException if memory is null or has no ID
     * @throws io.memorix.exception.StorageException if update fails
     */
    Memory update(Memory memory);
    
    /**
     * Delete a memory by ID.
     * 
     * @param memoryId Memory ID to delete
     * @throws IllegalArgumentException if memoryId is null or empty
     * @throws io.memorix.exception.StorageException if delete fails
     */
    void delete(String memoryId);
    
    /**
     * Delete all memories for a user.
     * 
     * @param userId User ID
     * @return Number of memories deleted
     * @throws IllegalArgumentException if userId is null or empty
     * @throws io.memorix.exception.StorageException if delete fails
     */
    int deleteByUserId(String userId);
    
    /**
     * Find memory by ID.
     * 
     * @param memoryId Memory ID
     * @return Memory if found
     */
    Optional<Memory> findById(String memoryId);
    
    /**
     * Find all memories for a user.
     * 
     * @param userId User ID
     * @return List of memories (may be empty)
     */
    List<Memory> findByUserId(String userId);
    
    /**
     * Find memories for a user with decay above threshold.
     * 
     * @param userId User ID
     * @param minDecay Minimum decay value
     * @return List of memories (may be empty)
     */
    List<Memory> findByUserIdAndDecayAbove(String userId, int minDecay);
    
    /**
     * Count memories for a user.
     * 
     * @param userId User ID
     * @return Number of memories
     */
    long countByUserId(String userId);
}

