# 📖 Memorix - Usage Guide

**Complete tutorial for using Memorix in your projects**

---

## 🚀 Quick Start (5 minutes)

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.memorix</groupId>
    <artifactId>memorix-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Database

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/memorix
    username: postgres
    password: postgres

memorix:
  # 🆕 Auto-create database if it doesn't exist (PostgreSQL only)
  auto-create-database: true
  
  embedding:
    provider: openai
    api-key: ${OPENAI_API_KEY}
```

**💡 What happens with `auto-create-database: true`?**

1. Memorix checks if database `memorix` exists
2. If not - creates it automatically
3. Runs Flyway migrations to create tables
4. Ready to use! 🎉

**Note:** This only works for PostgreSQL. For other databases, create manually.

### 3. Initialize PostgreSQL (Optional - auto-created if enabled)

```sql
-- Install pgvector extension (required, but Flyway does this automatically)
CREATE EXTENSION IF NOT EXISTS vector;

-- Database and tables are created automatically if auto-create-database: true
-- Or create manually:
createdb memorix
```

### 4. Inject and Use

```java
@Service
public class MyChatbot {
    
    @Autowired
    private Memorix memorix;
    
    public void rememberPreference(String userId, String preference) {
        memorix.store(userId)
            .content(preference)
            .withType("USER_PREFERENCE")
            .save();
    }
    
    public List<Memory> getContext(String userId, String query) {
        return memorix.query(userId)
            .search(query)
            .withType("USER_PREFERENCE")
            .limit(20)
            .execute();
    }
}
```

**That's it! You're using Memorix.**

---

## 📚 Core Operations

### Saving Memories

#### Basic Save
```java
Memory memory = memorix.store("user123")
    .content("User loves pizza margherita")
    .withType("USER_PREFERENCE")
    .save();

// Auto-generated:
// ✅ UUID
// ✅ Vector embedding (1536 dims)
// ✅ Token count
// ✅ Initial decay (100)
// ✅ Timestamps
```

#### Save with Importance
```java
Memory important = memorix.store("user123")
    .content("User is allergic to peanuts")
    .withType("USER_PREFERENCE")
    .withImportance(1.0f)  // Maximum importance!
    .save();
```

#### Save with Properties
```java
Memory memory = memorix.store("user123")
    .content("User speaks Polish and English")
    .withType("USER_PREFERENCE")
    .withProperties(Map.of(
        "category", "language",
        "confidence", 0.95
    ))
    .save();
```

---

### Searching Memories

#### Basic Search
```java
List<Memory> results = memorix.query("user123")
    .search("What languages does user speak?")
    .withType("USER_PREFERENCE")
    .execute();

// Returns semantically similar memories
// Sorted by relevance
```

#### Search with Limits
```java
List<Memory> results = memorix.query("user123")
    .search("food preferences")
    .withType("USER_PREFERENCE")
    .limit(QueryLimit.builder()
        .maxCount(20)           // Max 20 memories
        .maxTokens(500)         // Max 500 tokens total
        .minSimilarity(0.6)     // Min 60% relevant
        .strategy(LimitStrategy.GREEDY)
        .build())
    .execute();

// Guaranteed: ≤ 20 memories, ≤ 500 tokens
```

#### Search with Metadata
```java
QueryResult result = memorix.query("user123")
    .search("pizza preferences")
    .withType("USER_PREFERENCE")
    .limit(20)
    .executeWithMetadata();

// Check what happened
System.out.printf("Found %d/%d memories, %d tokens, limited by: %s%n",
    result.getMetadata().getReturned(),
    result.getMetadata().getTotalFound(),
    result.getMetadata().getTotalTokens(),
    result.getMetadata().getLimitReason()
);
```

---

### Lifecycle Management

#### Apply Decay After Conversation
```java
// User had conversation
List<Memory> usedMemories = ... // Memories shown in conversation

// Extract IDs
List<String> usedIds = usedMemories.stream()
    .map(Memory::getId)
    .toList();

// Apply decay (reinforce used, decay unused)
memorix.lifecycle()
    .forUser("user123")
    .withPluginType("USER_PREFERENCE")
    .markUsed(usedIds)
    .activeSession(true)
    .applyDecay()
    .cleanupExpired()
    .execute();
```

#### Scheduled Cleanup
```java
@Scheduled(cron = "0 0 2 * * ?")  // Daily at 2 AM
public void dailyCleanup() {
    // Cleanup expired memories for all users
    lifecycleManager.forUser("*")  // All users
        .withPluginType("CONVERSATION")
        .cleanupExpired()
        .execute();
}
```

---

## 🔌 Plugin Types

Memorix comes with 3 built-in plugin types:

### 1. USER_PREFERENCE
```java
// For: Lasting user preferences and facts
// Strategy: Usage-Based (freeze during breaks)
// Decay: Slow (-3), Strong reinforcement (+8)

memorix.store("user123")
    .content("Prefers dark mode")
    .withType("USER_PREFERENCE")
    .save();
```

### 2. DOCUMENTATION
```java
// For: Permanent documentation and knowledge
// Strategy: Permanent (never decays)
// Decay: Disabled

memorix.store("system")
    .content("API endpoint /users returns user list")
    .withType("DOCUMENTATION")
    .save();
```

### 3. CONVERSATION
```java
// For: Conversation context and history
// Strategy: Hybrid (usage + time)
// Decay: Mixed (30% time, 70% usage)

memorix.store("user123")
    .content("Discussed vacation plans for June")
    .withType("CONVERSATION")
    .save();
```

---

## 🎯 Real-World Examples

### Example 1: AI Chatbot

```java
@Service
public class ChatbotService {
    
    @Autowired
    private Memorix memorix;
    
    public String chat(String userId, String userMessage) {
        // 1. Get relevant context
        List<Memory> context = memorix
            .query(userId)
            .search(userMessage)
            .withType("USER_PREFERENCE")
            .limit(QueryLimit.builder()
                .maxTokens(2000)  // Leave room for GPT
                .minSimilarity(0.5)
                .strategy(LimitStrategy.GREEDY)
                .build())
            .execute();
        
        // 2. Send to LLM
        String response = callLLM(userMessage, context);
        
        // 3. Save new memory from conversation
        if (containsImportantInfo(response)) {
            memorix.store(userId)
                .content(extractMemory(response))
                .withType("USER_PREFERENCE")
                .save();
        }
        
        // 4. Apply decay
        memorix.lifecycle()
            .forUser(userId)
            .withPluginType("USER_PREFERENCE")
            .markUsed(context.stream().map(Memory::getId).toList())
            .applyDecay()
            .execute();
        
        return response;
    }
}
```

---

### Example 2: Knowledge Base Search

```java
@RestController
public class KnowledgeBaseController {
    
    @Autowired
    private Memorix memorix;
    
    @GetMapping("/search")
    public List<Memory> search(@RequestParam String query) {
        return memorix
            .query("system")
            .search(query)
            .withType("DOCUMENTATION")
            .limit(QueryLimit.builder()
                .maxCount(10)
                .minSimilarity(0.7)  // High precision
                .build())
            .execute();
    }
    
    @PostMapping("/docs")
    public Memory addDoc(@RequestBody String content) {
        return memorix
            .store("system")
            .content(content)
            .withType("DOCUMENTATION")
            .save();
    }
}
```

---

### Example 3: User Profile Memory

```java
@Service
public class UserProfileService {
    
    @Autowired
    private Memorix memorix;
    
    public void onUserAction(String userId, String action, String details) {
        // Save action as memory
        memorix.store(userId)
            .content(String.format("User %s: %s", action, details))
            .withType("USER_PREFERENCE")
            .withImportance(calculateImportance(action))
            .save();
        
        // Apply decay to rest
        memorix.lifecycle()
            .forUser(userId)
            .withPluginType("USER_PREFERENCE")
            .applyDecay()
            .execute();
    }
    
    public String getUserProfile(String userId) {
        List<Memory> memories = memorix
            .query(userId)
            .search("user profile and preferences")
            .withType("USER_PREFERENCE")
            .limit(50)
            .execute();
        
        return memories.stream()
            .map(Memory::getContent)
            .collect(Collectors.joining("; "));
    }
}
```

---

## 🎨 Creating Custom Plugins

### Step 1: Create Plugin Class

```java
package com.myapp.plugins;

import io.memorix.api.MemoryPlugin;
import io.memorix.model.*;
import io.memorix.plugin.MemoryType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@MemoryType("RECIPE")
public class RecipePlugin implements MemoryPlugin {
    
    @Override
    public String getType() {
        return "RECIPE";
    }
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategyClassName("io.memorix.lifecycle.PermanentDecayStrategy")
            .initialDecay(100)
            .autoDelete(false)  // Recipes don't expire
            .affectsSearchRanking(false)
            .build();
    }
    
    @Override
    public QueryLimit getDefaultQueryLimit() {
        return QueryLimit.builder()
            .maxCount(10)
            .maxTokens(1000)  // Recipes can be longer
            .minSimilarity(0.6)
            .strategy(LimitStrategy.GREEDY)
            .build();
    }
    
    @Override
    public Map<String, Object> extractProperties(String memory) {
        return Map.of(
            "category", "recipe",
            "permanent", true
        );
    }
}
```

### Step 2: Use Your Plugin

```java
// Save recipe
Memory recipe = memorix.store("user123")
    .content("Pizza Margherita: dough, tomato sauce, mozzarella, basil")
    .withType("RECIPE")
    .save();

// Search recipes
List<Memory> recipes = memorix.query("user123")
    .search("italian food recipes")
    .withType("RECIPE")
    .execute();

// Recipes never decay!
```

---

## 🗄️ Multi-DataSource Support (Advanced)

**🆕 NEW:** Plugins can now use physically separate databases for complete data isolation.

### Use Case: When You Need Multiple Databases

✅ **Use separate databases when:**
- Different retention policies (documentation = permanent, conversations = 30 days)
- Different security requirements (docs = restricted, preferences = open)
- Different teams manage different types (docs team, data team)
- Need independent scaling (heavy doc queries vs light preference lookups)
- Regulatory requirements (personal data in separate DB)

❌ **Don't use separate databases when:**
- Small project with similar requirements across types
- Simplicity is more important than isolation
- All types have similar access patterns

### Step 1: Configure Multiple DataSources

```yaml
# application.yml
memorix:
  datasources:
    default:
      url: jdbc:postgresql://localhost:5432/memorix
      username: postgres
      password: postgres
    documentation:
      url: jdbc:postgresql://docs-server:5432/memorix_docs
      username: docs_user
      password: docs_pass
    recipes:
      url: jdbc:postgresql://recipes-server:5432/memorix_recipes
      username: recipes_user
      password: recipes_pass
```

### Step 2: Plugin Declares DataSource

```java
@Component
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin {
    
    // ... existing configuration ...
    
    /**
     * Use separate 'documentation' database.
     * Falls back to 'default' if not configured.
     */
    @Override
    public String getDataSourceName() {
        return "documentation";
    }
    
    /**
     * Optional: Define custom table schema with additional columns.
     */
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("doc_memories")     // Custom table name
            .vectorDimension(1536)
            .addCustomColumn("doc_version VARCHAR(50)")
            .addCustomColumn("doc_category VARCHAR(100)")
            .addCustomIndex("CREATE INDEX idx_doc_version ON doc_memories(doc_version)")
            .addCustomIndex("CREATE INDEX idx_doc_category ON doc_memories(doc_category)")
            .build();
    }
}
```

### Step 3: Use Normally - Routing is Automatic!

```java
// Save to documentation database (automatic routing)
Memory doc = memorix.store("user123")
    .content("API endpoint /users returns user list")
    .withType("DOCUMENTATION")
    .save();

// Save to recipes database (automatic routing)
Memory recipe = memorix.store("user123")
    .content("Pizza Margherita recipe")
    .withType("RECIPE")
    .save();

// Each goes to its own database!
```

### Architecture Benefits

```
┌─────────────────────┐
│   Your Application  │
└──────────┬──────────┘
           │
    ┌──────▼───────┐
    │ MemoryService│ (Routes based on plugin)
    └──────┬───────┘
           │
    ┌──────▼────────────────────────┐
    │  DataSource Router            │
    └──┬──────────┬─────────────┬───┘
       │          │             │
   ┌───▼──┐  ┌───▼────┐  ┌────▼────┐
   │ DB 1 │  │  DB 2  │  │  DB 3   │
   │(def) │  │ (docs) │  │(recipes)│
   └──────┘  └────────┘  └─────────┘
```

**Key Points:**
- ✅ Automatic routing based on plugin type
- ✅ Transparent to application code
- ✅ Falls back to 'default' if datasource not configured
- ✅ Each plugin = independent database & schema
- ✅ Full ACID transactions per database

---

## 💡 Best Practices

### 1. Always Specify Plugin Type
```java
// ✅ GOOD
.withType("USER_PREFERENCE")

// ❌ BAD: Will fail
// (no default type)
```

### 2. Use Token Limits for LLM Context
```java
// ✅ GOOD: Safe for GPT-3.5
.limit(QueryLimit.builder()
    .maxTokens(2000)
    .build())

// ❌ BAD: Might overflow
.limit(50)  // Could be 5000 tokens!
```

### 3. Apply Decay Regularly
```java
// ✅ GOOD: After each session
memorix.lifecycle()
    .forUser(userId)
    .markUsed(usedIds)
    .applyDecay()
    .execute();

// ❌ BAD: Never applying decay
// Memories accumulate forever!
```

### 4. Use Appropriate Strategies
```java
// Preferences → Usage-Based
.withType("USER_PREFERENCE")

// News → Time-Based
.withType("NEWS_ARTICLE")

// Interests → Hybrid
.withType("USER_INTEREST")

// Docs → Permanent
.withType("DOCUMENTATION")
```

---

## 🔧 Configuration

### Database
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/memorix
    username: postgres
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
```

### Flyway
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

### Logging
```yaml
logging:
  level:
    io.memorix: INFO
    io.memorix.query: DEBUG  # Debug queries
    io.memorix.lifecycle: DEBUG  # Debug decay
```

---

## 🐛 Troubleshooting

### "Plugin not found for type: X"
```java
// Cause: Plugin not registered
// Fix: Ensure plugin class is @Component and in scan path

@Component
@MemoryType("MY_TYPE")
public class MyPlugin implements MemoryPlugin { }
```

### "Query cannot be null or empty"
```java
// Cause: Missing search() call
// Fix: Always specify query

memorix.query("user123")
    .search("your query here")  // Required!
    .execute();
```

### "Plugin type must be specified"
```java
// Cause: Missing withType() call
// Fix: Always specify plugin type

memorix.store("user123")
    .content("text")
    .withType("USER_PREFERENCE")  // Required!
    .save();
```

---

## 📊 Monitoring

### Get Statistics
```java
MemoryService.MemoryStats stats = memorix.stats("user123");
System.out.println("Total memories: " + stats.getTotalMemories());
```

### Query Metadata
```java
QueryResult result = memorix.query("user123")
    .search("query")
    .withType("USER_PREFERENCE")
    .executeWithMetadata();

// Log insights
log.info("Query stats: {}/{} memories, {} tokens, limited by: {}, avg similarity: {:.2f}, took {}ms",
    result.getMetadata().getReturned(),
    result.getMetadata().getTotalFound(),
    result.getMetadata().getTotalTokens(),
    result.getMetadata().getLimitReason(),
    result.getMetadata().getAvgSimilarity(),
    result.getMetadata().getExecutionTimeMs()
);
```

---

## 🎓 Advanced Usage

### Custom Decay Strategy
```java
@Component
public class CustomDecayStrategy implements DecayStrategy {
    
    @Override
    public int calculateDecay(Memory memory, DecayContext context) {
        // Your custom logic
        int current = memory.getDecay();
        
        if (context.wasUsedInSession()) {
            return current + 10;  // Strong reinforcement
        }
        
        // Exponential decay
        return (int) (current * 0.9);
    }
    
    @Override
    public String getStrategyName() {
        return "CUSTOM_EXPONENTIAL";
    }
}

// Use in plugin
DecayConfig.builder()
    .strategyClassName("com.myapp.CustomDecayStrategy")
    .build()
```

### Batch Operations
```java
// Save multiple memories efficiently
List<Memory> memories = users.stream()
    .map(user -> memorix.store(user.getId())
        .content(user.getPreference())
        .withType("USER_PREFERENCE")
        .save())
    .toList();

// Apply decay to multiple users
users.forEach(user -> 
    memorix.lifecycle()
        .forUser(user.getId())
        .withPluginType("USER_PREFERENCE")
        .applyDecay()
        .execute()
);
```

---

## 📦 Complete Example Project

```java
@SpringBootApplication
public class MemorixExampleApp {
    
    public static void main(String[] args) {
        SpringApplication.run(MemorixExampleApp.class, args);
    }
}

@RestController
@RequestMapping("/api/memories")
public class MemoryController {
    
    @Autowired
    private Memorix memorix;
    
    @PostMapping
    public Memory save(@RequestParam String userId, 
                      @RequestParam String content) {
        return memorix.store(userId)
            .content(content)
            .withType("USER_PREFERENCE")
            .save();
    }
    
    @GetMapping("/search")
    public List<Memory> search(@RequestParam String userId,
                              @RequestParam String query) {
        return memorix.query(userId)
            .search(query)
            .withType("USER_PREFERENCE")
            .limit(20)
            .execute();
    }
    
    @PostMapping("/decay")
    public String decay(@RequestParam String userId,
                       @RequestBody List<String> usedIds) {
        var result = memorix.lifecycle()
            .forUser(userId)
            .withPluginType("USER_PREFERENCE")
            .markUsed(usedIds)
            .applyDecay()
            .cleanupExpired()
            .execute();
        
        return String.format("Processed %d, deleted %d",
            result.getDecayApplied(),
            result.getMemoriesDeleted());
    }
}
```

---

## 🎯 Performance Tips

### 1. Use Token Limits
```java
// Prevents huge result sets
.maxTokens(2000)
```

### 2. Set minSimilarity
```java
// Filters low-relevance results early
.minSimilarity(0.5)
```

### 3. Apply Decay in Batches
```java
// Once per session, not per message
memorix.lifecycle()
    .forUser(userId)
    .applyDecay()
    .execute();
```

### 4. Use GREEDY Strategy
```java
// Maximizes context usage
.strategy(LimitStrategy.GREEDY)
```

---

## 🚦 Migration from Old System

### Before (hybridVectorStorage)
```java
HybridStorageManager<MemoryEntry> manager = 
    HybridStorageManagerFactory.createMemoryManager(config);

manager.saveObject(userId, entry);
List<MemoryEntry> results = manager.searchSimilarObjects(userId, query, 20);
manager.updateDecayBatch(userId, usedIds, unusedIds);
```

### After (Memorix)
```java
@Autowired
private Memorix memorix;

memorix.store(userId).content(text).withType("USER_PREFERENCE").save();
List<Memory> results = memorix.query(userId).search(query).execute();
memorix.lifecycle().forUser(userId).markUsed(usedIds).applyDecay().execute();
```

**Benefits**:
- Cleaner API
- Better type safety
- Plugin system
- Auto-embedding
- Multi-dimensional limits

---

## 📞 Getting Help

- **Documentation**: See all `*.md` files in project root
- **Examples**: `memorix-core/src/test/java/io/memorix/MemorixEndToEndTest.java`
- **Issues**: GitHub Issues (when published)
- **Discussions**: GitHub Discussions (when published)

---

## 🎁 Next Steps

1. Read [PROJECT_VISION.md](PROJECT_VISION.md) - Understand architecture
2. Read [DECAY_STRATEGIES.md](DECAY_STRATEGIES.md) - Master lifecycle
3. Read [QUERY_LIMITS.md](QUERY_LIMITS.md) - Optimize for LLMs
4. Create your own plugins!
5. Build something awesome!

---

**Happy memory management! 🧠✨**

*Last Updated: 2024-10-14*  
*Version: 1.0.0*

