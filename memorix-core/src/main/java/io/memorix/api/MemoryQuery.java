package io.memorix.api;

import io.memorix.model.Memory;
import io.memorix.model.QueryLimit;
import io.memorix.model.QueryResult;
import java.util.List;

/**
 * Interface for querying memories with semantic search.
 * 
 * <p>Provides fluent API for complex queries:
 * <ul>
 *   <li>Semantic search by text</li>
 *   <li>Vector similarity search</li>
 *   <li>Multi-dimensional limits</li>
 *   <li>Metadata filtering</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * List<Memory> results = memoryQuery
 *     .forUser("user123")
 *     .search("pizza preferences")
 *     .limit(QueryLimit.builder()
 *         .maxCount(20)
 *         .maxTokens(500)
 *         .minSimilarity(0.6)
 *         .strategy(LimitStrategy.GREEDY)
 *         .build())
 *     .execute();
 * }</pre>
 */
public interface MemoryQuery {
    
    /**
     * Set user ID for query.
     * 
     * @param userId User ID
     * @return This query builder
     */
    MemoryQuery forUser(String userId);
    
    /**
     * Search by text (converts to embedding).
     * 
     * @param query Search query text
     * @return This query builder
     */
    MemoryQuery search(String query);
    
    /**
     * Search by vector embedding directly.
     * 
     * @param embedding Query vector
     * @return This query builder
     */
    MemoryQuery searchByVector(float[] embedding);
    
    /**
     * Set query limits.
     * 
     * @param limit Query limits (maxCount, maxTokens, minSimilarity, strategy)
     * @return This query builder
     */
    MemoryQuery limit(QueryLimit limit);
    
    /**
     * Filter by minimum decay.
     * 
     * @param minDecay Minimum decay value
     * @return This query builder
     */
    MemoryQuery minDecay(int minDecay);
    
    /**
     * Execute query and return memories.
     * 
     * @return List of matching memories
     * @throws IllegalStateException if required parameters not set
     * @throws io.memorix.exception.StorageException if query fails
     */
    List<Memory> execute();
    
    /**
     * Execute query and return memories with metadata.
     * 
     * @return Query result with memories and execution metadata
     * @throws IllegalStateException if required parameters not set
     * @throws io.memorix.exception.StorageException if query fails
     */
    QueryResult executeWithMetadata();
}

