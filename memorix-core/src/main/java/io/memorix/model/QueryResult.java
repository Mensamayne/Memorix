package io.memorix.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a query with metadata.
 * 
 * <p>Contains:
 * <ul>
 *   <li>Retrieved memories</li>
 *   <li>Metadata about execution (total found, tokens, why stopped, etc)</li>
 * </ul>
 */
public class QueryResult {
    
    private final List<Memory> memories;
    private final QueryMetadata metadata;
    
    public QueryResult(List<Memory> memories, QueryMetadata metadata) {
        this.memories = new ArrayList<>(memories);
        this.metadata = metadata;
    }
    
    public List<Memory> getMemories() {
        return new ArrayList<>(memories);
    }
    
    public QueryMetadata getMetadata() {
        return metadata;
    }
    
    /**
     * Metadata about query execution.
     */
    public static class QueryMetadata {
        private final int totalFound;
        private final int returned;
        private final int totalTokens;
        private final double avgSimilarity;
        private final String limitReason;
        private final long executionTimeMs;
        
        public QueryMetadata(int totalFound, int returned, int totalTokens, 
                           double avgSimilarity, String limitReason, long executionTimeMs) {
            this.totalFound = totalFound;
            this.returned = returned;
            this.totalTokens = totalTokens;
            this.avgSimilarity = avgSimilarity;
            this.limitReason = limitReason;
            this.executionTimeMs = executionTimeMs;
        }
        
        public int getTotalFound() {
            return totalFound;
        }
        
        public int getReturned() {
            return returned;
        }
        
        public int getTotalTokens() {
            return totalTokens;
        }
        
        public double getAvgSimilarity() {
            return avgSimilarity;
        }
        
        public String getLimitReason() {
            return limitReason;
        }
        
        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
        
        @Override
        public String toString() {
            return "QueryMetadata{" +
                    "totalFound=" + totalFound +
                    ", returned=" + returned +
                    ", totalTokens=" + totalTokens +
                    ", avgSimilarity=" + String.format("%.2f", avgSimilarity) +
                    ", limitReason='" + limitReason + '\'' +
                    ", executionTimeMs=" + executionTimeMs +
                    '}';
        }
    }
}

