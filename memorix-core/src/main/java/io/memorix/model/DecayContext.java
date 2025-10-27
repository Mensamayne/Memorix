package io.memorix.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Context information for decay calculation.
 * 
 * <p>Provides runtime information to decay strategies:
 * <ul>
 *   <li>Current time</li>
 *   <li>Memory usage status</li>
 *   <li>Session information</li>
 *   <li>Time since last use/creation</li>
 *   <li>Decay configuration</li>
 * </ul>
 */
public class DecayContext {
    
    private final LocalDateTime now;
    private final boolean wasUsedInSession;
    private final boolean isActiveSession;
    private final int sessionsSinceLastUse;
    private final Duration timeSinceLastUse;
    private final Duration timeSinceCreated;
    private final int totalUsageCount;
    private final DecayConfig decayConfig;
    private final Map<String, Object> customParams;
    
    private DecayContext(Builder builder) {
        this.now = builder.now;
        this.wasUsedInSession = builder.wasUsedInSession;
        this.isActiveSession = builder.isActiveSession;
        this.sessionsSinceLastUse = builder.sessionsSinceLastUse;
        this.timeSinceLastUse = builder.timeSinceLastUse;
        this.timeSinceCreated = builder.timeSinceCreated;
        this.totalUsageCount = builder.totalUsageCount;
        this.decayConfig = builder.decayConfig;
        this.customParams = new HashMap<>(builder.customParams);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create context for a memory.
     */
    public static DecayContext forMemory(Memory memory, DecayConfig config) {
        LocalDateTime now = LocalDateTime.now();
        
        return builder()
            .now(now)
            .timeSinceCreated(Duration.between(memory.getCreatedAt(), now))
            .timeSinceLastUse(memory.getLastAccessedAt() != null 
                ? Duration.between(memory.getLastAccessedAt(), now)
                : Duration.between(memory.getCreatedAt(), now))
            .decayConfig(config)
            .build();
    }
    
    public static class Builder {
        private LocalDateTime now = LocalDateTime.now();
        private boolean wasUsedInSession = false;
        private boolean isActiveSession = true;
        private int sessionsSinceLastUse = 0;
        private Duration timeSinceLastUse = Duration.ZERO;
        private Duration timeSinceCreated = Duration.ZERO;
        private int totalUsageCount = 0;
        private DecayConfig decayConfig;
        private Map<String, Object> customParams = new HashMap<>();
        
        public Builder now(LocalDateTime now) {
            this.now = now;
            return this;
        }
        
        public Builder wasUsedInSession(boolean wasUsed) {
            this.wasUsedInSession = wasUsed;
            return this;
        }
        
        public Builder isActiveSession(boolean isActive) {
            this.isActiveSession = isActive;
            return this;
        }
        
        public Builder sessionsSinceLastUse(int sessions) {
            this.sessionsSinceLastUse = sessions;
            return this;
        }
        
        public Builder timeSinceLastUse(Duration duration) {
            this.timeSinceLastUse = duration;
            return this;
        }
        
        public Builder timeSinceCreated(Duration duration) {
            this.timeSinceCreated = duration;
            return this;
        }
        
        public Builder totalUsageCount(int count) {
            this.totalUsageCount = count;
            return this;
        }
        
        public Builder decayConfig(DecayConfig config) {
            this.decayConfig = config;
            return this;
        }
        
        public Builder customParam(String key, Object value) {
            this.customParams.put(key, value);
            return this;
        }
        
        public DecayContext build() {
            return new DecayContext(this);
        }
    }
    
    // Getters
    public LocalDateTime getNow() {
        return now;
    }
    
    public boolean wasUsedInSession() {
        return wasUsedInSession;
    }
    
    public boolean isActiveSession() {
        return isActiveSession;
    }
    
    public int getSessionsSinceLastUse() {
        return sessionsSinceLastUse;
    }
    
    public Duration getTimeSinceLastUse() {
        return timeSinceLastUse;
    }
    
    public Duration getTimeSinceCreated() {
        return timeSinceCreated;
    }
    
    public int getTotalUsageCount() {
        return totalUsageCount;
    }
    
    public DecayConfig getDecayConfig() {
        return decayConfig;
    }
    
    public Map<String, Object> getCustomParams() {
        return new HashMap<>(customParams);
    }
    
    public Object getCustomParam(String key, Object defaultValue) {
        return customParams.getOrDefault(key, defaultValue);
    }
    
    // Convenience methods
    public int getInitialDecay() {
        return decayConfig != null ? decayConfig.getInitialDecay() : 100;
    }
    
    public int getMinDecay() {
        return decayConfig != null ? decayConfig.getMinDecay() : 0;
    }
    
    public int getMaxDecay() {
        return decayConfig != null ? decayConfig.getMaxDecay() : 128;
    }
    
    public int getDecayReduction() {
        return decayConfig != null ? decayConfig.getDecayReduction() : 4;
    }
    
    public int getDecayReinforcement() {
        return decayConfig != null ? decayConfig.getDecayReinforcement() : 6;
    }
    
    public Duration getDecayInterval() {
        return decayConfig != null ? decayConfig.getDecayInterval() : Duration.ofDays(7);
    }
    
    public Object getStrategyParam(String key, Object defaultValue) {
        if (decayConfig == null) {
            return defaultValue;
        }
        return decayConfig.getStrategyParams().getOrDefault(key, defaultValue);
    }
}

