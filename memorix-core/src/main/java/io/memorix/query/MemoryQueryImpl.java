package io.memorix.query;

import io.memorix.api.MemoryQuery;
import io.memorix.embedding.EmbeddingProvider;
import io.memorix.model.Memory;
import io.memorix.model.QueryLimit;
import io.memorix.model.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of fluent MemoryQuery API.
 * 
 * <p>Prototype-scoped to allow multiple concurrent queries.
 */
@Component
@Scope("prototype")
public class MemoryQueryImpl implements MemoryQuery {
    
    private final QueryExecutor queryExecutor;
    private final EmbeddingProvider embeddingProvider;
    
    private String userId;
    private String queryText;
    private float[] queryVector;
    private QueryLimit limit = QueryLimit.builder().build();  // Default
    
    @Autowired
    public MemoryQueryImpl(QueryExecutor queryExecutor, 
                           EmbeddingProvider embeddingProvider) {
        this.queryExecutor = queryExecutor;
        this.embeddingProvider = embeddingProvider;
    }
    
    @Override
    public MemoryQuery forUser(String userId) {
        this.userId = userId;
        return this;
    }
    
    @Override
    public MemoryQuery search(String query) {
        this.queryText = query;
        return this;
    }
    
    @Override
    public MemoryQuery searchByVector(float[] embedding) {
        this.queryVector = embedding;
        return this;
    }
    
    @Override
    public MemoryQuery limit(QueryLimit limit) {
        this.limit = limit;
        return this;
    }
    
    @Override
    public MemoryQuery minDecay(int minDecay) {
        // Minimum decay filtering - planned for v1.1.0
        return this;
    }
    
    @Override
    public List<Memory> execute() {
        return executeWithMetadata().getMemories();
    }
    
    @Override
    public QueryResult executeWithMetadata() {
        // Validate required parameters
        validateParameters();
        
        // Get query vector
        float[] vector = getQueryVector();
        
        // Execute query
        return queryExecutor.executeQuery(userId, vector, limit);
    }
    
    private void validateParameters() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("User ID must be set. Call forUser() first.");
        }
        
        if (queryText == null && queryVector == null) {
            throw new IllegalStateException("Query must be set. Call search() or searchByVector() first.");
        }
    }
    
    private float[] getQueryVector() {
        if (queryVector != null) {
            return queryVector;
        }
        
        // Generate embedding from text using configured provider
        return embeddingProvider.embed(queryText);
    }
}

