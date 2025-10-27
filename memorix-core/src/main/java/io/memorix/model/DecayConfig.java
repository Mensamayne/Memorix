package io.memorix.model;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for decay behavior.
 * 
 * <p>Defines how memories decay over time/usage:
 * <ul>
 *   <li>Strategy class</li>
 *   <li>Initial/min/max decay values</li>
 *   <li>Reduction/reinforcement amounts</li>
 *   <li>Auto-deletion settings</li>
 *   <li>Search ranking behavior</li>
 * </ul>
 */
public class DecayConfig {
    
    private final String strategyClassName;
    private final int initialDecay;
    private final int minDecay;
    private final int maxDecay;
    private final int decayReduction;
    private final int decayReinforcement;
    private final boolean autoDelete;
    private final boolean affectsSearchRanking;
    private final Duration decayInterval;
    private final Map<String, Object> strategyParams;
    
    private DecayConfig(Builder builder) {
        this.strategyClassName = builder.strategyClassName;
        this.initialDecay = builder.initialDecay;
        this.minDecay = builder.minDecay;
        this.maxDecay = builder.maxDecay;
        this.decayReduction = builder.decayReduction;
        this.decayReinforcement = builder.decayReinforcement;
        this.autoDelete = builder.autoDelete;
        this.affectsSearchRanking = builder.affectsSearchRanking;
        this.decayInterval = builder.decayInterval;
        this.strategyParams = new HashMap<>(builder.strategyParams);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String strategyClassName = "io.memorix.lifecycle.UsageBasedDecayStrategy";
        private int initialDecay = 100;
        private int minDecay = 0;
        private int maxDecay = 128;
        private int decayReduction = 4;
        private int decayReinforcement = 6;
        private boolean autoDelete = true;
        private boolean affectsSearchRanking = true;
        private Duration decayInterval = Duration.ofDays(7);
        private Map<String, Object> strategyParams = new HashMap<>();
        
        public Builder strategyClassName(String strategyClassName) {
            this.strategyClassName = Objects.requireNonNull(strategyClassName);
            return this;
        }
        
        public Builder initialDecay(int initialDecay) {
            if (initialDecay < 0) {
                throw new IllegalArgumentException("initialDecay must be >= 0");
            }
            this.initialDecay = initialDecay;
            return this;
        }
        
        public Builder minDecay(int minDecay) {
            if (minDecay < 0) {
                throw new IllegalArgumentException("minDecay must be >= 0");
            }
            this.minDecay = minDecay;
            return this;
        }
        
        public Builder maxDecay(int maxDecay) {
            if (maxDecay < 0) {
                throw new IllegalArgumentException("maxDecay must be >= 0");
            }
            this.maxDecay = maxDecay;
            return this;
        }
        
        public Builder decayReduction(int decayReduction) {
            if (decayReduction < 0) {
                throw new IllegalArgumentException("decayReduction must be >= 0");
            }
            this.decayReduction = decayReduction;
            return this;
        }
        
        public Builder decayReinforcement(int decayReinforcement) {
            if (decayReinforcement < 0) {
                throw new IllegalArgumentException("decayReinforcement must be >= 0");
            }
            this.decayReinforcement = decayReinforcement;
            return this;
        }
        
        public Builder autoDelete(boolean autoDelete) {
            this.autoDelete = autoDelete;
            return this;
        }
        
        public Builder affectsSearchRanking(boolean affectsSearchRanking) {
            this.affectsSearchRanking = affectsSearchRanking;
            return this;
        }
        
        public Builder decayInterval(Duration decayInterval) {
            this.decayInterval = Objects.requireNonNull(decayInterval);
            return this;
        }
        
        public Builder strategyParam(String key, Object value) {
            this.strategyParams.put(key, value);
            return this;
        }
        
        public Builder strategyParams(Map<String, Object> params) {
            this.strategyParams = new HashMap<>(params);
            return this;
        }
        
        public DecayConfig build() {
            if (minDecay > maxDecay) {
                throw new IllegalArgumentException("minDecay cannot be greater than maxDecay");
            }
            if (initialDecay < minDecay || initialDecay > maxDecay) {
                throw new IllegalArgumentException("initialDecay must be between minDecay and maxDecay");
            }
            return new DecayConfig(this);
        }
    }
    
    // Getters
    public String getStrategyClassName() {
        return strategyClassName;
    }
    
    public int getInitialDecay() {
        return initialDecay;
    }
    
    public int getMinDecay() {
        return minDecay;
    }
    
    public int getMaxDecay() {
        return maxDecay;
    }
    
    public int getDecayReduction() {
        return decayReduction;
    }
    
    public int getDecayReinforcement() {
        return decayReinforcement;
    }
    
    public boolean isAutoDelete() {
        return autoDelete;
    }
    
    public boolean affectsSearchRanking() {
        return affectsSearchRanking;
    }
    
    public Duration getDecayInterval() {
        return decayInterval;
    }
    
    public Map<String, Object> getStrategyParams() {
        return new HashMap<>(strategyParams);
    }
}

