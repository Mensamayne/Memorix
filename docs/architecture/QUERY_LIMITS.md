# 🎯 Query Limits - LLM Optimization Guide

**Managing Context Windows with Multi-Dimensional Limits**

---

## 🚨 The Problem

LLMs like GPT have strict token limits. You can't just send "top 20 memories" if each is 500 tokens!

```java
// ❌ BAD: Can blow up context window
List<Memory> memories = memorix
    .query("user123")
    .search("pizza preferences")
    .limit(20)  // 20 × 500 tokens = 10,000 tokens! 💥
    .execute();

// GPT-3.5 context: 4096 tokens
// Your query: 100 tokens
// Response buffer: 1000 tokens
// Available for memories: ~2500 tokens
// You sent: 10,000 tokens → ERROR!
```

---

## ✅ The Solution: Multi-Dimensional Limits

Control results by **multiple criteria** simultaneously:

1. **maxCount** - Maximum number of memories
2. **maxTokens** - Maximum total tokens
3. **minSimilarity** - Minimum relevance threshold
4. **strategy** - How to combine limits

```java
// ✅ GOOD: Safe for LLM context
List<Memory> memories = memorix
    .query("user123")
    .search("pizza preferences")
    .limit(QueryLimit.builder()
        .maxCount(20)           // Max 20 memories
        .maxTokens(500)         // Max 500 tokens total
        .minSimilarity(0.5)     // Min 50% relevance
        .strategy(LimitStrategy.GREEDY)
        .build())
    .execute();

// Guaranteed: ≤ 20 memories, ≤ 500 tokens, all ≥ 50% similar
```

---

## 🏗️ QueryLimit Class

```java
public class QueryLimit {
    private final Integer maxCount;        // Max memories (null = unlimited)
    private final Integer maxTokens;       // Max total tokens (null = unlimited)
    private final Double minSimilarity;    // Min cosine similarity 0.0-1.0 (null = any)
    private final LimitStrategy strategy;  // How to combine limits
    
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
                throw new IllegalArgumentException("minSimilarity must be 0.0-1.0");
            }
            this.minSimilarity = similarity;
            return this;
        }
        
        public Builder strategy(LimitStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public QueryLimit build() {
            return new QueryLimit(maxCount, maxTokens, minSimilarity, strategy);
        }
    }
}
```

---

## 🎨 Limit Strategies

### 1. **ALL (Strict)** - All limits must be satisfied

```java
.strategy(LimitStrategy.ALL)

// Returns memories where:
// - count <= maxCount AND
// - tokens <= maxTokens AND
// - similarity >= minSimilarity
```

**Use Case**: "Must fit in context window"

**Example**:
```java
.maxCount(20)
.maxTokens(500)
.minSimilarity(0.5)
.strategy(LimitStrategy.ALL)

// Could return 15 memories, 480 tokens if #16 would exceed 500 tokens
// All returned memories have similarity >= 0.5
```

---

### 2. **ANY (Flexible)** - First limit stops

```java
.strategy(LimitStrategy.ANY)

// Returns memories until:
// - count == maxCount OR
// - tokens >= maxTokens OR
// - similarity < minSimilarity
```

**Use Case**: "Give me lots of context, but not too much"

**Example**:
```java
.maxCount(50)
.maxTokens(1000)
.strategy(LimitStrategy.ANY)

// Returns 50 memories (if small) OR stops at 1000 tokens (if large)
```

---

### 3. **GREEDY (Maximize)** - Pack as much as possible

```java
.strategy(LimitStrategy.GREEDY)

// Returns memories that fit:
// - Keep adding while count < maxCount
// - AND tokens + next.tokens <= maxTokens
// - AND similarity >= minSimilarity
// - Skip memories that would exceed limits
```

**Use Case**: "Fill to the brim, but don't overflow"

**Example**:
```java
.maxCount(30)
.maxTokens(800)
.minSimilarity(0.6)
.strategy(LimitStrategy.GREEDY)

// Execution:
// Memory 1: 50 tokens, 0.95 sim → ADD (total: 50)
// Memory 2: 60 tokens, 0.92 sim → ADD (total: 110)
// ...
// Memory 25: 40 tokens, 0.65 sim → ADD (total: 760)
// Memory 26: 50 tokens, 0.63 sim → SKIP (would be 810 > 800)
// Memory 27: 30 tokens, 0.62 sim → ADD (total: 790)
// Memory 28: 20 tokens, 0.61 sim → SKIP (would be 810 > 800)
// ...
// Returns: 26 memories, 790 tokens, all >= 0.6 similarity
```

**Key**: Doesn't cut memory in half! Returns 790 tokens, not 810.

---

### 4. **FIRST_MET (Alternative)** - First satisfied wins

```java
.strategy(LimitStrategy.FIRST_MET)

// Returns memories until FIRST limit is satisfied
```

**Use Case**: "Either very relevant OR fill quota"

**Example**:
```java
.maxCount(20)
.minSimilarity(0.9)
.strategy(LimitStrategy.FIRST_MET)

// Stops when: 20 memories found OR found 5 with 0.9+ similarity
```

---

## 🔢 Token Counting

### Why Cache Token Count?

```java
// ❌ BAD: Count tokens on every query
int tokens = 0;
for (Memory m : memories) {
    tokens += tokenCounter.count(m.getContent());  // SLOW!
}

// ✅ GOOD: Count once, cache in database
CREATE TABLE memories (
    id SERIAL PRIMARY KEY,
    content TEXT,
    token_count INTEGER,  -- Calculated at insert time
    embedding vector(1536)
);
```

### Token Counter Interface

```java
public interface TokenCounter {
    /**
     * Count tokens in text.
     * 
     * @param text Text to count
     * @return Number of tokens
     */
    int count(String text);
}
```

### Implementation 1: Approximate (Fast)

```java
@Component
public class ApproximateTokenCounter implements TokenCounter {
    
    @Override
    public int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Heuristic: ~3 characters per token
        // English: ~4 chars/token
        // Polish: ~2 chars/token (more multi-char letters)
        // Average: ~3 chars/token
        return text.length() / 3;
    }
}
```

**Pros**: 
- ⚡ Instant
- 🔢 Good enough for limits
- 💾 Can cache in DB

**Cons**:
- ⚠️ Not exact
- 📊 Varies by language

---

### Implementation 2: Exact (Slow)

```java
@Component
public class TiktokenCounter implements TokenCounter {
    
    private final Encoding encoding;
    
    public TiktokenCounter() {
        // Use OpenAI's tiktoken
        this.encoding = Encodings.newDefaultEncodingRegistry()
            .getEncoding(EncodingType.CL100K_BASE);  // GPT-3.5/4
    }
    
    @Override
    public int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return encoding.countTokens(text);
    }
}
```

**Pros**:
- ✅ Exact OpenAI count
- 🎯 Matches LLM tokens perfectly

**Cons**:
- 🐌 Slower (native code)
- 📦 External dependency

---

### Hybrid Approach (Recommended)

```java
@Service
public class HybridTokenCounter implements TokenCounter {
    
    @Autowired
    private ApproximateTokenCounter approximate;
    
    @Autowired
    private TiktokenCounter exact;
    
    /**
     * Use approximate for filtering, exact for final count.
     */
    public int count(String text) {
        // Fast approximate for most cases
        return approximate.count(text);
    }
    
    /**
     * Get exact count when precision matters.
     */
    public int countExact(String text) {
        return exact.count(text);
    }
}
```

**Strategy**:
1. Store approximate count in DB (fast)
2. Use approximate for query limits (good enough)
3. Use exact for final LLM submission (precision)

---

## 📊 Query Execution (Smart Cutoff)

### How It Works

```java
public List<Memory> executeWithLimits(String userId, 
                                      float[] queryVector, 
                                      QueryLimit limit) {
    
    // 1. Fetch candidates (2x buffer for safety)
    int fetchLimit = (limit.getMaxCount() != null) 
        ? limit.getMaxCount() * 2 
        : 1000;
    
    List<MemoryWithSimilarity> candidates = repository.findBySimilarity(
        userId, 
        queryVector, 
        fetchLimit
    );
    
    // 2. Apply limits with strategy
    return applyLimits(candidates, limit);
}

private List<Memory> applyLimits(List<MemoryWithSimilarity> candidates, 
                                 QueryLimit limit) {
    
    List<Memory> results = new ArrayList<>();
    int currentCount = 0;
    int currentTokens = 0;
    
    for (MemoryWithSimilarity candidate : candidates) {
        Memory memory = candidate.getMemory();
        double similarity = candidate.getSimilarity();
        
        // Check similarity threshold
        if (limit.getMinSimilarity() != null && 
            similarity < limit.getMinSimilarity()) {
            
            if (limit.getStrategy() == LimitStrategy.ALL) {
                break;  // Rest will be worse (sorted by similarity)
            } else {
                continue;  // Skip this one
            }
        }
        
        // Check count limit
        if (limit.getMaxCount() != null && 
            currentCount >= limit.getMaxCount()) {
            
            if (shouldStopForCount(limit.getStrategy())) {
                break;
            }
        }
        
        // Check token limit (SMART CUTOFF!)
        int memoryTokens = memory.getTokenCount();
        if (limit.getMaxTokens() != null && 
            currentTokens + memoryTokens > limit.getMaxTokens()) {
            
            if (shouldStopForTokens(limit.getStrategy())) {
                break;  // Don't add - would exceed
            } else {
                continue;  // Try next (might be smaller)
            }
        }
        
        // Add memory
        results.add(memory);
        currentCount++;
        currentTokens += memoryTokens;
    }
    
    return results;
}
```

**Key**: Never cut memory in half! If adding it would exceed limit, skip it.

---

## 📈 Query Result Metadata

### Why Metadata?

Track **what happened** during query execution:
- How many found vs returned?
- Why did it stop?
- How many tokens used?

### QueryResult Class

```java
public class QueryResult {
    private final List<Memory> memories;
    private final QueryMetadata metadata;
    
    public static class QueryMetadata {
        private int totalFound;        // Total matches in DB
        private int returned;          // Actually returned
        private int totalTokens;       // Sum of tokens
        private double avgSimilarity;  // Average similarity
        private String limitReason;    // Why stopped: maxCount/maxTokens/minSimilarity
        private long executionTimeMs;  // Query time
    }
}
```

### Usage

```java
QueryResult result = memorix
    .query("user123")
    .search("pizza")
    .limit(limit)
    .executeWithMetadata();

// Log details
log.info("Query: returned {}/{} memories, {} tokens, limited by: {}, avg similarity: {:.2f}, took {}ms",
    result.getMetadata().getReturned(),
    result.getMetadata().getTotalFound(),
    result.getMetadata().getTotalTokens(),
    result.getMetadata().getLimitReason(),
    result.getMetadata().getAvgSimilarity(),
    result.getMetadata().getExecutionTimeMs()
);

// Example output:
// "Query: returned 18/47 memories, 496 tokens, limited by: maxTokens, avg similarity: 0.78, took 45ms"
```

---

## 🎯 Real-World Examples

### Example 1: GPT-3.5 Context Management

```java
// GPT-3.5: 4096 token context
// Query: ~100 tokens
// System prompt: ~200 tokens
// Response buffer: ~1500 tokens
// Available for memories: ~2200 tokens

List<Memory> context = memorix
    .query(userId)
    .search(userQuery)
    .limit(QueryLimit.builder()
        .maxTokens(2000)         // Safe buffer
        .minSimilarity(0.6)      // Only relevant
        .strategy(LimitStrategy.ALL)
        .build())
    .execute();
```

---

### Example 2: Bulk Context ("Give me everything relevant")

```java
List<Memory> context = memorix
    .query(userId)
    .search("user dietary restrictions and food preferences")
    .limit(QueryLimit.builder()
        .maxCount(100)           // Lots of context
        .maxTokens(5000)         // GPT-4 Turbo has room
        .minSimilarity(0.4)      // Cast wide net
        .strategy(LimitStrategy.ANY)  // Either limit
        .build())
    .execute();
```

---

### Example 3: High-Precision Search

```java
List<Memory> criticalInfo = memorix
    .query(userId)
    .search("medical allergies and conditions")
    .limit(QueryLimit.builder()
        .maxCount(10)            // Not many needed
        .minSimilarity(0.85)     // VERY relevant only
        .strategy(LimitStrategy.ALL)
        .build())
    .execute();

// Might return only 3 memories if others < 0.85 similarity
// Safety > quantity for medical data
```

---

### Example 4: Greedy Packing

```java
List<Memory> context = memorix
    .query(userId)
    .search("vacation planning")
    .limit(QueryLimit.builder()
        .maxCount(50)
        .maxTokens(3000)
        .minSimilarity(0.5)
        .strategy(LimitStrategy.GREEDY)  // Pack it full!
        .build())
    .execute();

// Returns: as many memories as fit in 3000 tokens
// Example: 42 memories, 2987 tokens
```

---

## 🔧 Plugin Configuration

### Default Query Limits Per Plugin

```java
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin<UserPreference> {
    
    @Override
    public QueryConfig getDefaultQueryConfig() {
        return QueryConfig.builder()
            .defaultLimit(QueryLimit.builder()
                .maxCount(20)
                .maxTokens(400)
                .minSimilarity(0.5)
                .strategy(LimitStrategy.GREEDY)
                .build())
            .build();
    }
}

// User can always override:
memorix.query("user123")
    .search("pizza")
    .limit(customLimit)  // Override plugin default
    .execute();
```

---

## 💡 Best Practices

### 1. **Always Set maxTokens for LLM Context**

```java
// ✅ GOOD: Safe for any LLM
.maxTokens(calculateAvailableTokens(llmContextSize))

// ❌ BAD: Might overflow
.maxCount(50)  // Could be 5000 tokens!
```

### 2. **Use GREEDY for Context Packing**

```java
// ✅ GOOD: Maximize context usage
.strategy(LimitStrategy.GREEDY)

// Returns 2987 tokens instead of 2500
```

### 3. **Set minSimilarity for Quality**

```java
// ✅ GOOD: Only relevant memories
.minSimilarity(0.6)

// ❌ BAD: Garbage in results
// (no minSimilarity check)
```

### 4. **Monitor Metadata**

```java
QueryResult result = memorix.query(...).executeWithMetadata();

if (result.getMetadata().getLimitReason().equals("maxTokens")) {
    log.warn("Hitting token limit! Consider increasing or adjusting query");
}

if (result.getMetadata().getAvgSimilarity() < 0.5) {
    log.warn("Low relevance results - query might be too broad");
}
```

---

## 🎓 Summary

| Feature | Purpose | Example |
|---------|---------|---------|
| **maxCount** | Limit number of memories | `.maxCount(20)` |
| **maxTokens** | Fit in LLM context | `.maxTokens(500)` |
| **minSimilarity** | Quality threshold | `.minSimilarity(0.6)` |
| **ALL** | Strict limits | All conditions must be met |
| **ANY** | Flexible limits | First limit stops |
| **GREEDY** | Maximize packing | Fill without overflow |
| **Metadata** | Track execution | Why stopped, how many, etc |

**Key Principle**: Don't cut memories in half! Smart cutoff ensures clean boundaries.

---

*Last Updated: 2024-10-14*  
*Version: 1.0*  
*Maintainers: Memorix Team*

