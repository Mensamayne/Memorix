package io.memorix.model;

/**
 * Strategy for applying query limits.
 * 
 * <p>Defines how multiple limits are combined:
 * <ul>
 *   <li>ALL - All limits must be satisfied (AND)</li>
 *   <li>ANY - First limit stops (OR)</li>
 *   <li>GREEDY - Pack as much as possible without exceeding</li>
 *   <li>FIRST_MET - First satisfied condition wins</li>
 * </ul>
 */
public enum LimitStrategy {
    
    /**
     * ALL: All limits must be satisfied (AND).
     * 
     * <p>Returns memories where:
     * count <= maxCount AND tokens <= maxTokens AND similarity >= minSimilarity
     */
    ALL,
    
    /**
     * ANY: First limit stops (OR).
     * 
     * <p>Returns memories until:
     * count == maxCount OR tokens >= maxTokens OR similarity < minSimilarity
     */
    ANY,
    
    /**
     * GREEDY: Pack as much as possible.
     * 
     * <p>Returns memories that fit:
     * - Keep adding while count < maxCount
     * - AND tokens + next.tokens <= maxTokens
     * - AND similarity >= minSimilarity
     * - Skip memories that would exceed limits
     */
    GREEDY,
    
    /**
     * FIRST_MET: First satisfied condition wins.
     * 
     * <p>Returns memories until first limit is met.
     */
    FIRST_MET
}

