# 🔄 Decay Strategies - Complete Guide

**Understanding Memory Lifecycle in Memorix**

---

## 🎯 What is Decay?

Decay is a numeric value (typically 0-128) that represents the "strength" or "relevance" of a memory. Higher decay = stronger/more important memory.

### Core Concepts

1. **Initial Decay**: Starting value when memory is created (default: 100)
2. **Decay Reduction**: How much decay decreases per cycle (default: -4)
3. **Decay Reinforcement**: How much decay increases when used (default: +6)
4. **Min/Max Bounds**: Decay is clamped to [minDecay, maxDecay]
5. **Auto-Delete**: Memory is deleted when decay <= minDecay (if enabled)

---

## 🔧 Decay Configuration (Per Plugin)

```java
public class DecayConfig {
    private Class<? extends DecayStrategy> strategy;  // Which strategy to use
    private int initialDecay;                         // Starting value (default: 100)
    private int minDecay;                             // Lower bound (default: 0)
    private int maxDecay;                             // Upper bound (default: 128)
    private int decayReduction;                       // -X per unused cycle (default: 4)
    private int decayReinforcement;                   // +X when used (default: 6)
    private boolean autoDelete;                       // Delete when decay <= minDecay
    private boolean affectsSearchRanking;             // Does decay boost search results?
    
    // Optional: For time-based strategies
    private Duration decayInterval;                   // How often to apply decay
    
    // Optional: For hybrid strategies
    private Map<String, Object> strategyParams;       // Custom params per strategy
}
```

---

## 🎨 Built-in Strategies

### 1. Usage-Based Decay Strategy

**Philosophy**: Memories decay based on usage, NOT time.

**Key Behavior**: 
- User's break from app = **freeze decay** (no change)
- Only active sessions trigger decay
- Used memories get reinforced
- Unused memories (during active sessions) decay

**Perfect For**:
- User preferences ("Likes pizza")
- Permanent facts ("Lives in Warsaw")
- User profile data
- Long-lasting knowledge

#### Configuration Example

```java
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin<UserPreference> {
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategy(UsageBasedDecayStrategy.class)
            .initialDecay(100)
            .minDecay(0)
            .maxDecay(200)              // Higher max for important preferences
            .decayReduction(3)          // Slow decay
            .decayReinforcement(8)      // Strong reinforcement
            .autoDelete(true)
            .affectsSearchRanking(true) // More used = higher in search
            .build();
    }
}
```

#### Implementation

```java
@Component
public class UsageBasedDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        int current = memory.getDecay();
        
        // Was this memory used in current session?
        if (context.wasUsedInSession()) {
            // REINFORCE: Memory was accessed!
            return Math.min(
                current + context.getDecayReinforcement(), 
                context.getMaxDecay()
            );
        }
        
        // Is this an active session (user using app)?
        if (context.isActiveSession()) {
            // DECAY: Memory not used during active session
            return Math.max(
                current - context.getDecayReduction(), 
                context.getMinDecay()
            );
        }
        
        // User not using app = FREEZE (no change)
        return current;
    }
}
```

#### Timeline Example

```
Day 1: "User likes pizza" saved → decay = 100
Day 2: Used in conversation → decay = 108 (+8)
Day 3: Not used, but app active → decay = 105 (-3)
Day 4-30: User doesn't use app → decay = 105 (FROZEN!)
Day 31: User returns, memory used → decay = 113 (+8)
```

---

### 2. Time-Based Decay Strategy

**Philosophy**: Memories decay based on calendar time.

**Key Behavior**:
- Real-time clock drives decay
- User's activity doesn't matter
- Gradual, predictable degradation
- Auto-cleanup after expiration

**Perfect For**:
- News articles ("Bitcoin hits 40K")
- Event announcements ("Conference on June 15")
- Time-sensitive data
- Prices, statuses

#### Configuration Example

```java
@MemoryType("NEWS_ARTICLE")
public class NewsArticlePlugin implements MemoryPlugin<NewsArticle> {
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategy(TimeBasedDecayStrategy.class)
            .initialDecay(100)
            .minDecay(0)
            .maxDecay(100)              // No reinforcement needed
            .decayReduction(5)          // -5 per week
            .decayInterval(Duration.ofDays(7))  // Weekly decay
            .autoDelete(true)
            .affectsSearchRanking(false)  // Age doesn't affect ranking
            .build();
    }
}
```

#### Implementation

```java
@Component
public class TimeBasedDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        Duration age = context.getTimeSinceCreated();
        Duration interval = context.getDecayInterval();
        
        // How many intervals have passed?
        long intervalsElapsed = age.toMillis() / interval.toMillis();
        
        // Calculate decay reduction
        int totalReduction = (int) (intervalsElapsed * context.getDecayReduction());
        
        // Return initial - reduction (clamped to min)
        return Math.max(
            context.getInitialDecay() - totalReduction,
            context.getMinDecay()
        );
    }
}
```

#### Timeline Example

```
Week 0: "Bitcoin at 40K" saved → decay = 100
Week 1: 7 days passed → decay = 95 (-5)
Week 2: 14 days passed → decay = 90 (-5)
Week 5: 35 days passed → decay = 75 (-5 × 5)
Week 20: 140 days passed → decay = 0 → DELETED
```

---

### 3. Hybrid Decay Strategy

**Philosophy**: Combine usage AND time for balanced decay.

**Key Behavior**:
- Usage is PRIMARY factor
- Time is SECONDARY factor
- Long inactivity adds gentle decay
- Recent usage overrides time

**Perfect For**:
- User interests ("Plays Baldur's Gate")
- Hobbies and activities
- Context that changes over time
- Social connections

#### Configuration Example

```java
@MemoryType("USER_INTEREST")
public class UserInterestPlugin implements MemoryPlugin<UserInterest> {
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategy(HybridDecayStrategy.class)
            .initialDecay(100)
            .minDecay(0)
            .maxDecay(150)
            .decayReduction(4)
            .decayReinforcement(6)
            .autoDelete(true)
            .affectsSearchRanking(true)
            .strategyParams(Map.of(
                "timeFactor", 0.3,           // 30% weight to time
                "usageFactor", 0.7,          // 70% weight to usage
                "inactivityThreshold", 90    // Days before time matters
            ))
            .build();
    }
}
```

#### Implementation

```java
@Component
public class HybridDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        int current = memory.getDecay();
        
        // Component 1: USAGE (dominant)
        if (context.wasUsedInSession()) {
            current += context.getDecayReinforcement();  // +6
        } else if (context.isActiveSession()) {
            current -= (context.getDecayReduction() / 2);  // -2 (gentle)
        }
        
        // Component 2: TIME (gentle, kicks in after inactivity)
        long daysSinceLastUse = context.getTimeSinceLastUse().toDays();
        int inactivityThreshold = (int) context.getStrategyParam("inactivityThreshold", 90);
        
        if (daysSinceLastUse > inactivityThreshold) {
            // User inactive for 90+ days? Gentle time decay
            current -= 2;  // Slow fade
        }
        
        // Component 3: IMPORTANCE (multiplier)
        float importance = memory.getImportance();
        if (importance > 0.8f) {
            current += 1;  // Bonus for important memories
        }
        
        // Clamp to bounds
        return Math.max(context.getMinDecay(), 
                   Math.min(current, context.getMaxDecay()));
    }
}
```

#### Timeline Example

```
Month 0: "User plays Baldur's Gate" → decay = 100
Month 1: Frequently used → decay = 130 (reinforced)
Month 2: Not used, app active → decay = 124 (-6)
Month 3-6: User doesn't use app → decay = 124 (frozen)
Month 7: 90+ days inactive → decay = 116 (-8 time decay)
Month 8: User returns, uses memory → decay = 122 (+6)
```

---

### 4. Permanent Decay Strategy

**Philosophy**: Memories never decay.

**Key Behavior**:
- Decay value never changes
- Auto-delete disabled
- Search ranking unaffected by decay

**Perfect For**:
- Documentation
- API definitions
- System knowledge
- Permanent facts

#### Configuration Example

```java
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin<Documentation> {
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategy(PermanentDecayStrategy.class)
            .initialDecay(100)
            .autoDelete(false)          // Never delete
            .affectsSearchRanking(false) // Decay irrelevant
            .build();
    }
}
```

#### Implementation

```java
@Component
public class PermanentDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        // Never change
        return memory.getDecay();
    }
    
    @Override
    public boolean shouldAutoDelete(Memory memory) {
        // Never delete
        return false;
    }
}
```

---

## 📊 Decay Affects Search Ranking

### Option 1: Decay Boosts Results

```java
// Higher decay = higher in search results
DecayConfig.builder()
    .affectsSearchRanking(true)
    .build();

// SQL query
SELECT 
    content,
    (1 - (embedding <=> $1)) * 0.7 +     -- 70% semantic similarity
    (decay / 128.0) * 0.3                 -- 30% decay boost
    AS final_score
FROM memories
ORDER BY final_score DESC;
```

**Use Case**: Frequently used memories should be "more accessible"

---

### Option 2: Decay Only for Deletion

```java
// Decay doesn't affect ranking, only cleanup
DecayConfig.builder()
    .affectsSearchRanking(false)
    .build();

// SQL query
SELECT content,
    (1 - (embedding <=> $1)) AS similarity
FROM memories
WHERE decay > 0  -- Only alive memories
ORDER BY similarity DESC;
```

**Use Case**: All "alive" memories equal, decay just cleans up dead ones

---

## 🔄 Lifecycle API

### Usage-Based Example

```java
// At end of user session
List<Memory> retrieved = memorix
    .query("user123")
    .search("pizza")
    .execute();

// Mark which were used
memorix.lifecycle()
    .forUser("user123")
    .markUsed(retrieved.stream().map(Memory::getId).toList())
    .applyDecay()     // +6 for used, -4 for unused (in this session)
    .cleanupExpired()
    .execute();
```

---

### Time-Based Example

```java
// Cron job (weekly)
@Scheduled(cron = "0 0 0 * * SUN")
public void weeklyDecay() {
    memorix.lifecycle()
        .forAllUsers()
        .recalculateAll()  // Recalc based on time
        .cleanupExpired()
        .execute();
}
```

---

### Hybrid Example

```java
// Session end + weekly check
@Scheduled(cron = "0 0 2 * * ?")
public void dailyMaintenance() {
    memorix.lifecycle()
        .forAllUsers()
        .withContext(DecayContext.builder()
            .isActiveSession(false)
            .build())
        .applyDecay()      // Usage component
        .recalculateAll()  // Time component
        .cleanupExpired()
        .execute();
}
```

---

## 🎯 Choosing the Right Strategy

| Memory Type | Strategy | Why |
|-------------|----------|-----|
| User Preferences | Usage-Based | Doesn't change with time, only with usage |
| News Articles | Time-Based | Expires naturally over time |
| User Interests | Hybrid | Can change, but gradually |
| Documentation | Permanent | Never expires |
| Conversation History | Usage-Based | Recent conversation matters |
| Events | Time-Based | Date-driven expiration |
| User Skills | Hybrid | Can rust but not quickly |
| Product Prices | Time-Based | Outdated quickly |

---

## 💡 Best Practices

### 1. **Don't Over-Configure**
```java
// ✅ GOOD: Use sensible defaults
DecayConfig.builder()
    .strategy(UsageBasedDecayStrategy.class)
    .build();  // Uses defaults: 100, 0-128, -4, +6

// ❌ BAD: Over-tuning
DecayConfig.builder()
    .initialDecay(87)
    .decayReduction(3.5)  // Can't use float!
    .build();
```

### 2. **Test Your Strategy**
```java
// Simulate 100 cycles
for (int i = 0; i < 100; i++) {
    memory.setDecay(strategy.calculateDecay(memory, context));
    System.out.println("Cycle " + i + ": decay = " + memory.getDecay());
}
```

### 3. **Monitor Deletion Rate**
```java
// Too aggressive?
if (deletionRate > 0.5) {
    // 50% of memories deleted? Maybe reduce decayReduction
}
```

### 4. **Match Strategy to Data**
- Preferences? → Usage-Based
- Events? → Time-Based
- Interests? → Hybrid
- Docs? → Permanent

---

## 🔬 Advanced: Custom Strategy

```java
@Component
public class ExponentialDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        int current = memory.getDecay();
        
        if (context.wasUsedInSession()) {
            return Math.min(current + 10, context.getMaxDecay());
        }
        
        // Exponential: decay = decay * 0.95
        return Math.max((int) (current * 0.95), context.getMinDecay());
    }
}

// Use it
DecayConfig.builder()
    .strategy(ExponentialDecayStrategy.class)
    .build();
```

---

*Last Updated: 2024-10-14*  
*Version: 1.0*  
*Maintainers: Memorix Team*

