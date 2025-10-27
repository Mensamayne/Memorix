package io.memorix.model;

/**
 * Strategy for handling duplicate memory detection.
 * 
 * <p>Defines how the system responds when a duplicate memory is detected:
 * <ul>
 *   <li><b>REJECT</b> - Throw exception, require user to modify content</li>
 *   <li><b>MERGE</b> - Update existing memory (reinforce decay, update importance)</li>
 *   <li><b>UPDATE</b> - Replace existing memory completely with new values</li>
 * </ul>
 * 
 * @see DeduplicationConfig
 */
public enum DeduplicationStrategy {
    
    /**
     * Reject duplicate and throw exception.
     * 
     * <p>Use when memories must be unique (e.g., documentation, recipes).
     */
    REJECT,
    
    /**
     * Merge with existing memory by reinforcing it.
     * 
     * <p>Updates:
     * <ul>
     *   <li>Decay increased (reinforcement)</li>
     *   <li>Importance = max(existing, new)</li>
     *   <li>updatedAt = now</li>
     *   <li>Content unchanged (keeps original)</li>
     * </ul>
     * 
     * <p>Use for user preferences, facts that don't change but get reinforced.
     */
    MERGE,
    
    /**
     * Update existing memory with new values.
     * 
     * <p>Replaces:
     * <ul>
     *   <li>Content (new version)</li>
     *   <li>Embedding (regenerated)</li>
     *   <li>Importance (new value)</li>
     *   <li>Decay (reset to initial)</li>
     * </ul>
     * 
     * <p>Use when memories evolve over time (e.g., user interests, status updates).
     */
    UPDATE
}

