package io.memorix.model;

import java.util.Objects;

/**
 * Configuration for memory deduplication.
 * 
 * <p>Supports two detection methods:
 * <ul>
 *   <li><b>Hash-based:</b> Exact duplicate detection via content hash</li>
 *   <li><b>Semantic:</b> Similar content detection via embedding similarity</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Hash-based only
 * DeduplicationConfig config = DeduplicationConfig.builder()
 *     .enabled(true)
 *     .strategy(DeduplicationStrategy.MERGE)
 *     .normalizeContent(true)
 *     .build();
 * 
 * // Semantic detection
 * DeduplicationConfig config = DeduplicationConfig.builder()
 *     .enabled(true)
 *     .strategy(DeduplicationStrategy.MERGE)
 *     .semanticEnabled(true)
 *     .semanticThreshold(0.85)  // 85% similarity = duplicate
 *     .build();
 * }</pre>
 */
public class DeduplicationConfig {
    
    private final boolean enabled;
    private final DeduplicationStrategy strategy;
    private final boolean normalizeContent;
    private final boolean semanticEnabled;
    private final double semanticThreshold;
    private final boolean reinforceOnMerge;
    
    private DeduplicationConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.strategy = builder.strategy;
        this.normalizeContent = builder.normalizeContent;
        this.semanticEnabled = builder.semanticEnabled;
        this.semanticThreshold = builder.semanticThreshold;
        this.reinforceOnMerge = builder.reinforceOnMerge;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Default configuration: disabled.
     * 
     * @return Default config with deduplication disabled
     */
    public static DeduplicationConfig disabled() {
        return builder().enabled(false).build();
    }
    
    public static class Builder {
        private boolean enabled = false;
        private DeduplicationStrategy strategy = DeduplicationStrategy.MERGE;
        private boolean normalizeContent = true;
        private boolean semanticEnabled = false;
        private double semanticThreshold = 0.85;
        private boolean reinforceOnMerge = true;
        
        /**
         * Enable or disable deduplication.
         * 
         * @param enabled True to enable deduplication
         * @return This builder
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        /**
         * Set strategy for handling duplicates.
         * 
         * @param strategy Deduplication strategy
         * @return This builder
         */
        public Builder strategy(DeduplicationStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
            return this;
        }
        
        /**
         * Enable content normalization before hashing.
         * 
         * <p>Normalization includes:
         * <ul>
         *   <li>Trim whitespace</li>
         *   <li>Convert to lowercase</li>
         *   <li>Collapse multiple spaces to single space</li>
         * </ul>
         * 
         * @param normalizeContent True to normalize content
         * @return This builder
         */
        public Builder normalizeContent(boolean normalizeContent) {
            this.normalizeContent = normalizeContent;
            return this;
        }
        
        /**
         * Enable semantic duplicate detection via embedding similarity.
         * 
         * <p>When enabled, uses vector similarity to detect semantically similar content:
         * <ul>
         *   <li>"User loves pizza" vs "User really likes pizza" → detected as duplicate</li>
         *   <li>"Prefers dark mode" vs "Likes dark theme" → detected as duplicate</li>
         * </ul>
         * 
         * @param semanticEnabled True to enable semantic detection
         * @return This builder
         */
        public Builder semanticEnabled(boolean semanticEnabled) {
            this.semanticEnabled = semanticEnabled;
            return this;
        }
        
        /**
         * Set similarity threshold for semantic duplicate detection.
         * 
         * <p>Threshold interpretation:
         * <ul>
         *   <li>0.95+ = Almost identical</li>
         *   <li>0.85-0.95 = Very similar (recommended default)</li>
         *   <li>0.70-0.85 = Similar topic</li>
         *   <li>&lt;0.70 = Different content</li>
         * </ul>
         * 
         * @param semanticThreshold Threshold (0.0 to 1.0), default 0.85
         * @return This builder
         */
        public Builder semanticThreshold(double semanticThreshold) {
            if (semanticThreshold < 0.0 || semanticThreshold > 1.0) {
                throw new IllegalArgumentException("semanticThreshold must be between 0.0 and 1.0");
            }
            this.semanticThreshold = semanticThreshold;
            return this;
        }
        
        /**
         * Control whether MERGE strategy reinforces decay.
         * 
         * <p>When true (default):
         * <ul>
         *   <li>Duplicate detected → decay increases (reinforcement)</li>
         *   <li>Use case: Frequent mentions = higher importance</li>
         *   <li>Example: User keeps saying "I love pizza" → signal it matters!</li>
         * </ul>
         * 
         * <p>When false:
         * <ul>
         *   <li>Duplicate detected → decay unchanged</li>
         *   <li>Use case: Duplicate is error, not signal of importance</li>
         *   <li>Example: Documentation shouldn't gain importance from duplicates</li>
         * </ul>
         * 
         * @param reinforceOnMerge True to reinforce decay on merge (default: true)
         * @return This builder
         */
        public Builder reinforceOnMerge(boolean reinforceOnMerge) {
            this.reinforceOnMerge = reinforceOnMerge;
            return this;
        }
        
        public DeduplicationConfig build() {
            return new DeduplicationConfig(this);
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public DeduplicationStrategy getStrategy() {
        return strategy;
    }
    
    public boolean isNormalizeContent() {
        return normalizeContent;
    }
    
    public boolean isSemanticEnabled() {
        return semanticEnabled;
    }
    
    public double getSemanticThreshold() {
        return semanticThreshold;
    }
    
    public boolean isReinforceOnMerge() {
        return reinforceOnMerge;
    }
    
    @Override
    public String toString() {
        if (!enabled) {
            return "DeduplicationConfig{disabled}";
        }
        StringBuilder sb = new StringBuilder("DeduplicationConfig{");
        sb.append("enabled=").append(enabled);
        sb.append(", strategy=").append(strategy);
        sb.append(", normalizeContent=").append(normalizeContent);
        if (semanticEnabled) {
            sb.append(", semanticEnabled=true");
            sb.append(", semanticThreshold=").append(semanticThreshold);
        }
        if (strategy == DeduplicationStrategy.MERGE) {
            sb.append(", reinforceOnMerge=").append(reinforceOnMerge);
        }
        sb.append('}');
        return sb.toString();
    }
}

