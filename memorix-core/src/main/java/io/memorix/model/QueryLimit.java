package io.memorix.model;

/**
 * Multi-dimensional query limits.
 * 
 * <p>Controls query results by:
 * <ul>
 *   <li>maxCount - Maximum number of memories</li>
 *   <li>maxTokens - Maximum total tokens</li>
 *   <li>minSimilarity - Minimum relevance threshold</li>
 *   <li>strategy - How to combine limits</li>
 * </ul>
 */
public class QueryLimit {
    
    private final Integer maxCount;
    private final Integer maxTokens;
    private final Double minSimilarity;
    private final LimitStrategy strategy;
    
    private QueryLimit(Builder builder) {
        this.maxCount = builder.maxCount;
        this.maxTokens = builder.maxTokens;
        this.minSimilarity = builder.minSimilarity;
        this.strategy = builder.strategy;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Integer maxCount;
        private Integer maxTokens;
        private Double minSimilarity;
        private LimitStrategy strategy = LimitStrategy.GREEDY;
        
        public Builder maxCount(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("maxCount must be > 0");
            }
            this.maxCount = count;
            return this;
        }
        
        public Builder maxTokens(int tokens) {
            if (tokens <= 0) {
                throw new IllegalArgumentException("maxTokens must be > 0");
            }
            this.maxTokens = tokens;
            return this;
        }
        
        public Builder minSimilarity(double similarity) {
            if (similarity < 0.0 || similarity > 1.0) {
                throw new IllegalArgumentException("minSimilarity must be between 0.0 and 1.0");
            }
            this.minSimilarity = similarity;
            return this;
        }
        
        public Builder strategy(LimitStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public QueryLimit build() {
            return new QueryLimit(this);
        }
    }
    
    // Getters
    public Integer getMaxCount() {
        return maxCount;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public Double getMinSimilarity() {
        return minSimilarity;
    }
    
    public LimitStrategy getStrategy() {
        return strategy;
    }
    
    @Override
    public String toString() {
        return "QueryLimit{" +
                "maxCount=" + maxCount +
                ", maxTokens=" + maxTokens +
                ", minSimilarity=" + minSimilarity +
                ", strategy=" + strategy +
                '}';
    }
}

