# 🧠 MEMORIX - Project Vision & Architecture

**The High-Quality AI Memory Framework for Java**

> *"A memory system so elegant, it becomes the foundation for every AI agent you build."*

---

## 🎯 Project Mission

**Memorix** is a production-grade, open-source memory management framework for AI applications, built on PostgreSQL + pgvector. It provides intelligent lifecycle management, semantic search, and pluggable architecture - all with ACID guarantees.

### Core Philosophy

1. **Quality Over Speed** - We write code we're proud of
2. **Simplicity Over Complexity** - Elegant solutions, not clever hacks
3. **Completeness Over Features** - Finish what we start, no half-measures
4. **PostgreSQL-First** - Leverage battle-tested technology, not reinvent it

---

## 🏗️ System Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  MEMORIX FRAMEWORK                          │
│              AI Memory Management System                    │
└─────────────────────────────────────────────────────────────┘

╔═══════════════════════════════════════════════════════════╗
║  Layer 1: Public API (What Users Interact With)          ║
╠═══════════════════════════════════════════════════════════╣
║  • MemoryStore<T>      - Main interface for storage      ║
║  • MemoryQuery<T>      - Fluent query builder            ║
║  • MemoryPlugin<T>     - Plugin interface                ║
║  • DecayStrategy       - Lifecycle strategy              ║
║  • EmbeddingProvider   - Embedding abstraction           ║
╚═══════════════════════════════════════════════════════════╝
                            ↓
╔═══════════════════════════════════════════════════════════╗
║  Layer 2: Core Engine (Business Logic)                   ║
╠═══════════════════════════════════════════════════════════╣
║  • MemoryManager       - Orchestrates all operations     ║
║  • LifecycleEngine     - Decay, reinforcement, cleanup   ║
║  • VectorService       - Embedding generation            ║
║  • PluginRegistry      - Plugin management               ║
║  • TransactionManager  - ACID transaction handling       ║
╚═══════════════════════════════════════════════════════════╝
                            ↓
╔═══════════════════════════════════════════════════════════╗
║  Layer 3: Storage Layer (Data Persistence)               ║
╠═══════════════════════════════════════════════════════════╣
║  PostgreSQL + pgvector                                    ║
║  • Metadata storage    (decay, importance, tags)         ║
║  • Vector storage      (embeddings via pgvector)         ║
║  • Semantic search     (cosine similarity)               ║
║  • ACID transactions   (PostgreSQL native)               ║
╚═══════════════════════════════════════════════════════════╝
                            ↓
╔═══════════════════════════════════════════════════════════╗
║  Layer 4: Provider Layer (External Integrations)         ║
╠═══════════════════════════════════════════════════════════╣
║  • OpenAI Provider     - text-embedding-3-small          ║
║  • Ollama Provider     - Local embeddings                ║
║  • Custom Provider     - User-defined embeddings         ║
╚═══════════════════════════════════════════════════════════╝
```

---

## 📦 Module Structure

### Multi-Module Maven Project

```
memorix/
├── memorix-core/                    ← Core library
│   ├── api/                         ← Public interfaces
│   │   ├── MemoryStore.java
│   │   ├── MemoryQuery.java
│   │   ├── MemoryPlugin.java
│   │   ├── DecayStrategy.java
│   │   └── EmbeddingProvider.java
│   │
│   ├── engine/                      ← Core business logic
│   │   ├── MemoryManager.java
│   │   ├── LifecycleEngine.java
│   │   ├── VectorService.java
│   │   ├── PluginRegistry.java
│   │   └── TransactionManager.java
│   │
│   ├── storage/                     ← Storage implementation
│   │   ├── PostgresVectorStore.java
│   │   ├── SchemaManager.java
│   │   └── MigrationManager.java
│   │
│   ├── lifecycle/                   ← Decay & cleanup
│   │   ├── DecayEngine.java
│   │   ├── ReinforcementEngine.java
│   │   ├── CleanupScheduler.java
│   │   └── strategies/
│   │       ├── LinearDecayStrategy.java
│   │       ├── ExponentialDecayStrategy.java
│   │       └── TimeBasedDecayStrategy.java
│   │
│   └── exception/                   ← Exception hierarchy
│       ├── MemorixException.java
│       ├── StorageException.java
│       ├── PluginException.java
│       └── EmbeddingException.java
│
├── memorix-spring-boot-starter/     ← Spring Boot integration
│   ├── autoconfigure/
│   │   ├── MemorixAutoConfiguration.java
│   │   ├── MemorixProperties.java
│   │   └── MemorixHealthIndicator.java
│   └── starter/
│       └── pom.xml
│
├── memorix-embeddings/              ← Embedding providers
│   ├── openai/
│   │   └── OpenAIEmbeddingProvider.java
│   ├── ollama/
│   │   └── OllamaEmbeddingProvider.java
│   └── custom/
│       └── CustomEmbeddingProvider.java
│
├── memorix-examples/                ← Example applications
│   ├── chatbot-memory/
│   ├── recipe-search/
│   ├── knowledge-base/
│   └── user-preferences/
│
├── memorix-docs/                    ← Documentation
│   ├── quick-start.md
│   ├── architecture.md
│   ├── plugin-development.md
│   ├── decay-strategies.md
│   └── migration-guide.md
│
├── pgvector/                        ← pgvector source (reference)
├── PROJECT_VISION.md                ← This file
├── CODE_QUALITY.md                  ← Code quality standards
├── UNFINISHED_BRIDGES.md            ← TODO tracker
├── README.md                        ← Main README
├── CONTRIBUTING.md                  ← Contribution guidelines
├── LICENSE                          ← MIT License
└── pom.xml                          ← Root POM
```

---

## 🎯 Core Features

### 1. Intelligent Decay System

**Problem**: AI agents accumulate memories but don't know what to forget.

**Solution**: Fully configurable decay strategies with automatic cleanup.

#### **Decay Configuration (Per Plugin)**

```java
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin<UserPreference> {
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategy(UsageBasedDecayStrategy.class)
            .initialDecay(100)          // Starting value
            .minDecay(0)                // Minimum (auto-delete threshold)
            .maxDecay(128)              // Maximum (can't reinforce above)
            .decayReduction(4)          // -4 per unused cycle
            .decayReinforcement(6)      // +6 when used
            .autoDelete(true)           // Delete when decay <= minDecay
            .affectsSearchRanking(true) // Higher decay = higher in results
            .build();
    }
}
```

#### **Decay Flow**

```java
// Initial state
memory.decay = 100;  // Fresh memory

// Cycle 1: Memory was used
memory.decay += 6;   // → 106 (reinforcement!)

// Cycle 2: Memory not used
memory.decay -= 4;   // → 102 (decay)

// ...many cycles later
memory.decay = 0;    // → AUTO-DELETE (if enabled)
```

#### **Decay Strategies**

**1. Usage-Based (Activity-Driven)**
```java
// Decay ONLY during active sessions
// User's break from app = freeze decay
// Perfect for: Preferences, lasting facts
```

**2. Time-Based (Calendar-Driven)**
```java
// Decay by real time elapsed
// User's break doesn't matter
// Perfect for: News, prices, events
```

**3. Hybrid (Mixed)**
```java
// Combines usage + time
// Configurable weighting
// Perfect for: Interests, hobbies
```

**4. Permanent (No Decay)**
```java
// Never decays, never deleted
// Perfect for: Documentation, definitions
```

#### **Context-Aware Decay**

```java
public class DecayContext {
    private LocalDateTime now;
    private boolean wasUsedInSession;
    private int sessionsSinceLastUse;
    private Duration timeSinceLastUse;
    private Duration timeSinceCreated;
    private int totalUsageCount;
    
    // Strategies can use ANY of these factors
}
```

**Key Insight**: User's break from app doesn't always mean memory should decay!
- Preferences persist (Usage-Based)
- News expires (Time-Based)
- Hobbies fade gradually (Hybrid)

### 2. Semantic Search with PostgreSQL

**Problem**: Traditional keyword search doesn't understand context.

**Solution**: Vector embeddings + pgvector for semantic similarity.

```sql
-- Find similar memories (single query!)
SELECT id, content, decay,
       embedding <=> $1 AS similarity
FROM memories
WHERE user_id = $2 
  AND decay > 0
ORDER BY similarity
LIMIT 20;
```

**Benefits**:
- Fast (HNSW index)
- Accurate (cosine similarity)
- Simple (pure SQL)
- Scalable (PostgreSQL clustering)

### 3. Plugin Architecture

**Problem**: Every project needs different memory types.

**Solution**: Generic plugin system for custom memory types.

```java
@MemoryType("USER_PREFERENCE")
public class UserPreference implements Storable {
    private String category;
    private String value;
    private float confidence;
    
    // Custom fields, custom logic
}

// Framework handles the rest
memorix.register(new UserPreferencePlugin());
```

**Plugin defines**:
- Custom fields
- Decay configuration
- Auto-deletion rules
- Vector properties
- 🆕 **DataSource & Table Schema** (NEW)

#### **🆕 Multi-DataSource Support**

**Problem**: Different memory types need physical isolation.

**Solution**: Plugins can declare their own datasource and table schema.

```java
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "documentation";  // Use separate database
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("doc_memories")
            .vectorDimension(1536)
            .addCustomColumn("doc_version VARCHAR(50)")
            .addCustomColumn("doc_category VARCHAR(100)")
            .addCustomIndex("CREATE INDEX idx_version ON doc_memories(doc_version)")
            .build();
    }
}
```

**Benefits:**
- **Physical Isolation** - Documentation DB separated from user preferences
- **Independent Scaling** - Scale docs DB separately based on load
- **Custom Schemas** - Each type can have different table structure
- **Security Policies** - Different retention/backup per database
- **Team Autonomy** - Docs team manages docs DB, data team manages preferences DB

**Configuration:**
```yaml
memorix:
  datasources:
    default:
      url: jdbc:postgresql://localhost:5432/memorix
    documentation:
      url: jdbc:postgresql://docs-server:5432/memorix_docs
    recipes:
      url: jdbc:postgresql://recipes-server:5432/memorix_recipes
```

**Routing is Automatic:**
```java
// Automatically routed to 'documentation' database
memorix.store("user123")
    .content("API docs")
    .withType("DOCUMENTATION")
    .save();
```

### 4. ACID Transactions

**Problem**: Memory operations must be atomic.

**Solution**: PostgreSQL native transactions.

```java
memorix.transaction(tx -> {
    Memory m1 = tx.save("First memory");
    Memory m2 = tx.save("Second memory");
    tx.commit();  // Both or neither
});
```

**Guarantees**:
- Atomicity: All or nothing
- Consistency: Valid state always
- Isolation: No dirty reads
- Durability: Persisted to disk

### 5. Batch Operations

**Problem**: Updating thousands of memories individually is slow.

**Solution**: SQL-based batch updates.

```java
// Update 10,000 memories in milliseconds
memorix.batch()
    .forUser("user123")
    .exclude(usedMemoryIds)
    .applyDecay(-4)
    .execute();
```

### 6. Advanced Query Limits (LLM-Optimized)

**Problem**: LLMs have context limits. Can't just send "top 20 memories" if they're huge!

**Solution**: Multi-dimensional query limits with smart cutoff.

#### **Query Limit Types**

```java
List<Memory> results = memorix
    .query("user123")
    .search("pizza preferences")
    .limit(QueryLimit.builder()
        .maxCount(20)              // Max 20 memories
        .maxTokens(500)            // Max 500 tokens TOTAL
        .minSimilarity(0.5)        // Min 50% relevance
        .strategy(LimitStrategy.GREEDY)  // Strategy
        .build())
    .execute();
```

#### **Limit Strategies**

**1. ALL (Strict) - All limits must be met**
```java
// Use case: "Must fit in GPT context window"
.strategy(LimitStrategy.ALL)
// Returns: count <= 20 AND tokens <= 500 AND similarity >= 0.5
```

**2. ANY (Flexible) - First limit stops**
```java
// Use case: "Give me 50 memories OR 1000 tokens, whichever first"
.strategy(LimitStrategy.ANY)
// Returns: Stops at first limit hit
```

**3. GREEDY (Maximize) - Fill to the brim**
```java
// Use case: "Pack as much as possible"
.strategy(LimitStrategy.GREEDY)
// Example:
// - 19 memories, 499 tokens
// - Next memory: 16 tokens (would be 515)
// - SKIP IT! Would exceed maxTokens
// - Returns: 19 memories, 499 tokens
```

**4. FIRST_MET (Alternative) - First satisfied condition wins**
```java
// Use case: "Either very relevant OR fill count"
.strategy(LimitStrategy.FIRST_MET)
// Returns: When any single limit is met
```

#### **Smart Token Cutoff**

```sql
-- Database stores pre-calculated token counts
CREATE TABLE memories (
    id SERIAL PRIMARY KEY,
    content TEXT,
    token_count INTEGER,  -- Cached! (approx length/3)
    embedding vector(1536),
    decay INTEGER
);
```

```java
// Query executor knows total before sending
int totalTokens = 0;
for (Memory m : candidates) {
    if (totalTokens + m.getTokenCount() > limit.getMaxTokens()) {
        break;  // Don't add this one - would exceed!
    }
    results.add(m);
    totalTokens += m.getTokenCount();
}
```

#### **Token Counting**

```java
public interface TokenCounter {
    int count(String text);
}

// Approximate (fast, cached in DB)
public class ApproximateTokenCounter implements TokenCounter {
    public int count(String text) {
        return text.length() / 3;  // ~3 chars per token
    }
}

// Exact (slower, uses tiktoken)
public class TiktokenCounter implements TokenCounter {
    public int count(String text) {
        return encoding.countTokens(text);  // Exact OpenAI count
    }
}
```

#### **Query Result Metadata**

```java
QueryResult result = memorix
    .query("user123")
    .search("pizza")
    .limit(limit)
    .executeWithMetadata();

// Check what happened
result.getMetadata().getTotalFound();      // 47 total
result.getMetadata().getReturned();        // 18 returned
result.getMetadata().getTotalTokens();     // 496 tokens
result.getMetadata().getLimitReason();     // "maxTokens"
result.getMetadata().getAvgSimilarity();   // 0.78
```

**Key Insight**: Don't cut memories in half! Return 499 tokens, not 515.

### 7. Fluent Query API

**Problem**: Complex queries are hard to read.

**Solution**: Fluent builder pattern.

```java
List<Memory> results = memorix
    .query("user123")
    .search("favorite food")
    .withTags("preference", "food")
    .minSimilarity(0.7)
    .minDecay(10)
    .limit(QueryLimit.builder()
        .maxCount(20)
        .maxTokens(500)
        .strategy(LimitStrategy.GREEDY)
        .build())
    .execute();
```

---

## 🚀 Development Phases

### Phase 1: Foundation (Week 1) ✅

**Goal**: Core infrastructure and basic CRUD.

- [x] Maven multi-module setup
- [x] PostgreSQL + pgvector connection
- [x] Core interfaces (MemoryStore, MemoryQuery, MemoryPlugin)
- [x] Basic CRUD operations
- [x] Exception hierarchy
- [ ] Unit tests (80%+ coverage)

**Deliverable**: Can save and retrieve memories.

---

### Phase 2: Plugin System (Week 2)

**Goal**: Dynamic plugin loading and registration.

- [ ] Plugin registry implementation
- [ ] Plugin lifecycle management
- [ ] Example plugins (UserMemory, Document, Preference)
- [ ] Plugin validation
- [ ] Plugin documentation

**Deliverable**: Users can define custom memory types.

---

### Phase 3: Decay & Lifecycle (Week 3)

**Goal**: Intelligent memory lifecycle management.

- [ ] Decay engine
- [ ] Decay strategies (Linear, Exponential, Time-based)
- [ ] Reinforcement engine
- [ ] Auto-deletion
- [ ] Cleanup scheduler
- [ ] Lifecycle metrics

**Deliverable**: Memories decay and cleanup automatically.

---

### Phase 4: Advanced Search (Week 4)

**Goal**: Powerful semantic search capabilities.

- [ ] Vector service
- [ ] Embedding providers (OpenAI, Ollama)
- [ ] Query builder (fluent API)
- [ ] Batch operations
- [ ] Search filters (tags, decay, similarity)
- [ ] Search optimization

**Deliverable**: Fast, accurate semantic search.

---

### Phase 5: Spring Integration (Week 5)

**Goal**: First-class Spring Boot support.

- [ ] Auto-configuration
- [ ] Configuration properties
- [ ] Health indicators
- [ ] Metrics (Micrometer)
- [ ] Actuator endpoints
- [ ] Transaction management

**Deliverable**: Spring Boot starter package.

---

### Phase 6: Documentation & Polish (Week 6)

**Goal**: Production-ready release.

- [ ] README with examples
- [ ] Architecture documentation
- [ ] Plugin development guide
- [ ] Migration guide
- [ ] Example projects
- [ ] Contributing guidelines
- [ ] Performance benchmarks

**Deliverable**: v1.0.0 release on GitHub.

---

## 💡 Key Design Decisions

### 1. PostgreSQL + pgvector Over Weaviate

**Why?**
- Single system (no sync issues)
- ACID transactions
- Battle-tested reliability
- Lower operational complexity
- SQL is universal
- Perfect for 90% of use cases

**When Weaviate wins?**
- Billions of vectors
- Multi-datacenter replication
- Advanced graph queries

**Our choice**: PostgreSQL for simplicity and reliability.

---

### 2. No Mocks, Real TODOs

**Why?**
- Mocks hide incomplete features
- TODOs are honest about progress
- Forces us to finish or document

**How?**
```java
// ❌ BAD: Mock hides incompleteness
@Override
public List<Memory> search(String query) {
    return Collections.emptyList(); // Pretends to work
}

// ✅ GOOD: TODO makes it explicit
@Override
public List<Memory> search(String query) {
    throw new UnsupportedOperationException(
        "TODO: Implement semantic search - see UNFINISHED_BRIDGES.md #12"
    );
}
```

---

### 3. Backwards Compatibility: Not Our Problem (Yet)

**Why?**
- v1.0.0 is not released yet
- Breaking changes are free
- Focus on getting it right, not maintaining legacy

**When it matters**:
- After v1.0.0 release
- When users depend on it
- Semantic versioning applies

**Our approach**: Move fast, break things, document changes.

---

### 4. Small Classes, Clear Responsibilities

**Why?**
- Easy to understand
- Easy to test
- Easy to refactor
- Single Responsibility Principle

**Rules**:
- Max 200 lines per class
- Max 20 lines per method
- One responsibility per class
- Extract until it hurts, then extract more

---

### 5. Centralized Error Handling

**Why?**
- Consistent error messages
- Easy to debug
- Clear exception hierarchy

**Structure**:
```java
MemorixException
├── StorageException
│   ├── ConnectionException
│   ├── QueryException
│   └── TransactionException
├── PluginException
│   ├── PluginNotFoundException
│   └── PluginValidationException
└── EmbeddingException
    ├── EmbeddingGenerationException
    └── ProviderException
```

---

## 🎯 Success Criteria

### Before v1.0.0 Release

1. **Zero TODO exceptions in main code path**
2. **80%+ test coverage**
3. **All core features implemented**
4. **Documentation complete**
5. **Example projects work**
6. **Performance benchmarks done**
7. **Code review passed**
8. **We're proud to show it**

### Quality Gates

- ✅ All tests pass
- ✅ No compiler warnings
- ✅ No SonarQube critical issues
- ✅ JavaDoc complete
- ✅ README examples work
- ✅ Performance acceptable (benchmarks)
- ✅ Code review approved
- ✅ UNFINISHED_BRIDGES.md is empty

---

## 🌟 What Makes Memorix Special

### Not Just Another Library

1. **Built on PostgreSQL** - Solid foundation, not another NoSQL experiment
2. **ACID Guarantees** - Transactions matter
3. **Context-Aware Decay** - Usage vs Time vs Hybrid strategies
4. **LLM-Optimized Queries** - Token limits, smart cutoff, metadata
5. **Fully Configurable** - Decay params, limits, strategies per plugin
6. **Plugin Architecture** - Truly extensible, not hardcoded
7. **Production Quality** - No shortcuts, no mocks
8. **Beautiful API** - Fluent, intuitive, documented
9. **Spring-First** - Seamless integration
10. **Open Source** - MIT license, community-driven

### The "Hibernate of AI Memory"

Just like Hibernate abstracts SQL databases, **Memorix abstracts AI memory management**.

- You don't think about SQL
- You don't think about vectors
- You don't think about decay
- **You just remember and recall**

---

## 📝 Final Notes

This is not a side project. This is not a proof of concept. This is not "good enough for now."

**This is our magnum opus.**

Every line of code should make us proud. Every design decision should be defensible. Every feature should be complete.

We're building something that will be used in production, referenced in tutorials, and copied by others.

**Let's make it worthy of that responsibility.**

---

*"Perfect is the enemy of good. But 'good enough' is the enemy of great. We're building great."*

**— Memorix Team**

