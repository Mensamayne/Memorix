package io.memorix.exception;

import io.memorix.model.Memory;

/**
 * Exception thrown when a duplicate memory is detected.
 * 
 * <p>Contains reference to the existing memory that triggered the duplicate detection.
 * This allows the caller to decide how to handle the situation (e.g., merge, update, or reject).
 * 
 * <p>Thrown only when deduplication is enabled and strategy is {@code REJECT}.
 * For {@code MERGE} and {@code UPDATE} strategies, the system handles duplicates automatically.
 * 
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     memorix.store(userId).content("User loves pizza").save();
 * } catch (DuplicateMemoryException e) {
 *     Memory existing = e.getExistingMemory();
 *     System.out.println("Duplicate of: " + existing.getContent());
 *     // Handle accordingly
 * }
 * }</pre>
 */
public class DuplicateMemoryException extends StorageException {
    
    private static final long serialVersionUID = 1L;
    
    private final transient Memory existingMemory;
    
    /**
     * Create exception with existing memory reference.
     * 
     * @param message Error message
     * @param existingMemory The existing memory that matches
     */
    public DuplicateMemoryException(String message, Memory existingMemory) {
        super(ErrorCode.DUPLICATE_MEMORY, message);
        this.existingMemory = existingMemory;
    }
    
    /**
     * Add context about existing memory.
     * 
     * @return This exception with context
     */
    public DuplicateMemoryException withExistingMemoryContext() {
        if (existingMemory != null) {
            withContext("existingMemoryId", existingMemory.getId());
            withContext("existingContent", existingMemory.getContent());
        }
        return this;
    }
    
    /**
     * Get the existing memory that triggered duplicate detection.
     * 
     * @return Existing memory
     */
    public Memory getExistingMemory() {
        return existingMemory;
    }
}

