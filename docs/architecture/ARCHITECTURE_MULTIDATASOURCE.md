# Multi-DataSource Architecture - Deep Dive

**Status**: ✅ Fully Implemented  
**Version**: 1.1.0-SNAPSHOT  
**Date**: 2025-10-21

---

## 🎯 **Problem Statement**

**Question**: *"Co z pluginem jak ma w chuj inne ustawienia a chce korzystać z tej samej bazy co inny plugin z innymi ustawieniami?"*

**Translation**: How does a plugin use different settings while sharing the same database with other plugins that have different settings?

---

## ✅ **Solution: Multi-Level Configuration**

Memorix separates **physical storage** from **logical configuration**:

```java
public interface MemoryPlugin {
    // LEVEL 1: Physical - Which DATABASE?
    String getDataSourceName();      // "default", "documentation", "recipes"
    
    // LEVEL 2: Physical - Which TABLE in that database?
    TableSchema getTableSchema();    // Custom table name, columns, indexes
    
    // LEVEL 3: Logical - How to manage memories?
    DecayConfig getDecayConfig();    // Decay strategy, initial decay, etc.
    QueryLimit getDefaultQueryLimit(); // Max tokens, count, similarity
    DeduplicationConfig getDeduplicationConfig(); // Duplicate handling
}
```

---

## 🏗️ **Architecture Layers**

### **Layer 1: DataSource (Physical Database)**

Plugins declare which **physical database** they want to use:

```java
@Component
@MemoryType("RECIPES")
public class RecipePlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "default"; // ✅ Uses same physical DB as other plugins
    }
}

@Component
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "documentation"; // ✅ Uses separate physical DB
    }
}
```

**Configuration** (`application.yml`):
```yaml
memorix:
  multi-datasource:
    enabled: true  # Enable routing
    
  datasources:
    default:
      url: jdbc:postgresql://localhost:5432/memorix
      username: postgres
      password: postgres
      
    documentation:
      url: jdbc:postgresql://localhost:5432/memorix_docs
      username: postgres
      password: postgres
```

**Benefits**:
- ✅ Physical isolation (separate servers)
- ✅ Independent scaling
- ✅ Different backup policies
- ✅ Team autonomy

---

### **Layer 2: TableSchema (Physical Table)**

Plugins can use **different tables** in the same database:

```java
@Component
@MemoryType("RECIPES")
public class RecipePlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "default"; // Same DB as UserPreferencePlugin
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("recipe_memories") // ✅ But different TABLE!
            .vectorDimension(1536)
            .addCustomColumn("cuisine VARCHAR(100)")
            .addCustomColumn("difficulty VARCHAR(50)")
            .addCustomIndex("CREATE INDEX idx_cuisine ON recipe_memories(cuisine)")
            .build();
    }
}

@Component
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "default"; // Same DB as RecipePlugin
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.DEFAULT; // Uses 'memories' table (default)
    }
}
```

**Result**: Both plugins use **same database**, but **different tables**:
- RecipePlugin → `default` database → `recipe_memories` table
- UserPreferencePlugin → `default` database → `memories` table

**Benefits**:
- ✅ Shared database (easier deployment)
- ✅ Custom schema per plugin
- ✅ Optimized indexes per use case
- ✅ Clear data separation

---

### **Layer 3: Logical Configuration (Settings)**

Each plugin has **independent logical settings**, regardless of physical location:

```java
@Component
@MemoryType("RECIPES")
public class RecipePlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "default"; // Same DB
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("recipe_memories") // Different table
            .build();
    }
    
    // ✅ DIFFERENT SETTINGS - Recipes never decay
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategyClassName("io.memorix.lifecycle.PermanentDecayStrategy")
            .initialDecay(100)
            .autoDelete(false) // Never delete recipes!
            .build();
    }
    
    @Override
    public QueryLimit getDefaultQueryLimit() {
        return QueryLimit.builder()
            .maxCount(10)
            .maxTokens(2000) // Recipes can be long
            .minSimilarity(0.7)
            .build();
    }
}

@Component
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "default"; // Same DB
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.DEFAULT; // Same table structure (different table name)
    }
    
    // ✅ DIFFERENT SETTINGS - Preferences decay with usage
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategyClassName("io.memorix.lifecycle.UsageBasedDecayStrategy")
            .initialDecay(100)
            .decayReduction(3) // Lose 3 when unused
            .decayReinforcement(8) // Gain 8 when used
            .autoDelete(true) // Delete when decay reaches 0
            .build();
    }
    
    @Override
    public QueryLimit getDefaultQueryLimit() {
        return QueryLimit.builder()
            .maxCount(20)
            .maxTokens(500) // Shorter context
            .minSimilarity(0.6)
            .build();
    }
}
```

**Result**: Both plugins:
- ✅ Use same database (`default`)
- ✅ Can use same OR different tables
- ✅ Have completely independent decay settings
- ✅ Have completely independent query limits
- ✅ Have completely independent deduplication settings

---

## 📊 **All Possible Combinations**

| Plugin A | Plugin B | Same DB? | Same Table? | Same Settings? | Supported? |
|----------|----------|----------|-------------|----------------|------------|
| Recipes  | User Pref | ✅ Yes | ❌ No | ❌ No | ✅ **YES** |
| Recipes  | User Pref | ✅ Yes | ✅ Yes | ❌ No | ✅ **YES** |
| Recipes  | Docs | ❌ No | ❌ No | ❌ No | ✅ **YES** |
| Recipes  | Docs | ❌ No | ❌ No | ✅ Yes | ✅ **YES** |

**Answer**: ✅ **TAK, można mieć różne pluginy z różnymi ustawieniami korzystające z tej samej bazy!**

---

## 🔧 **How It Works Internally**

### 1. **PluginDataSourceRouter** (ThreadLocal Routing)

```java
public class PluginDataSourceRouter extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceName = PluginDataSourceContext.getCurrentDataSource();
        return dataSourceName; // Routes to correct DB
    }
}
```

### 2. **PluginDataSourceContext** (Thread-Local Context)

```java
public class PluginDataSourceContext {
    
    private static final ThreadLocal<String> currentDataSource = new ThreadLocal<>();
    
    public static void setCurrentDataSource(String dataSourceName) {
        currentDataSource.set(dataSourceName);
    }
    
    public static String getCurrentDataSource() {
        String ds = currentDataSource.get();
        return ds != null ? ds : "default";
    }
}
```

### 3. **MemoryService** (Auto-Routing)

```java
@Service
public class MemoryService {
    
    public Memory save(String userId, String content, String pluginType) {
        MemoryPlugin plugin = pluginRegistry.getByType(pluginType);
        
        // Set datasource context for this thread
        String dataSourceName = plugin.getDataSourceName();
        PluginDataSourceContext.setCurrentDataSource(dataSourceName);
        
        try {
            // All database operations here use the correct datasource
            return memoryStore.save(memory);
        } finally {
            PluginDataSourceContext.clear(); // Clean up
        }
    }
}
```

---

## 🎯 **Example Use Cases**

### **Use Case 1: Same DB, Different Tables**

```yaml
# Config
memorix:
  multi-datasource:
    enabled: false  # Disable routing, use Spring Boot's default
    
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/memorix
```

```java
// Plugin 1: Uses 'memories' table
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin {
    public TableSchema getTableSchema() {
        return TableSchema.DEFAULT; // memories table
    }
    
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategy(UsageBasedDecayStrategy.class)
            .build();
    }
}

// Plugin 2: Uses 'recipe_memories' table
@MemoryType("RECIPES")
public class RecipePlugin implements MemoryPlugin {
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("recipe_memories") // Different table!
            .build();
    }
    
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategy(PermanentDecayStrategy.class) // Different settings!
            .build();
    }
}
```

**Result**:
- Same database: `memorix`
- Different tables: `memories` vs `recipe_memories`
- Different decay: Usage-based vs Permanent
- ✅ **Works perfectly!**

---

### **Use Case 2: Different DBs, Different Tables, Different Settings**

```yaml
# Config
memorix:
  multi-datasource:
    enabled: true
    
  datasources:
    default:
      url: jdbc:postgresql://localhost:5432/memorix
    documentation:
      url: jdbc:postgresql://localhost:5432/memorix_docs
    recipes:
      url: jdbc:postgresql://localhost:5432/memorix_recipes
```

```java
// Plugin 1: Default DB
@MemoryType("USER_PREFERENCE")
public class UserPreferencePlugin implements MemoryPlugin {
    public String getDataSourceName() { return "default"; }
    public TableSchema getTableSchema() { return TableSchema.DEFAULT; }
    public DecayConfig getDecayConfig() { /* usage-based */ }
}

// Plugin 2: Docs DB
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin {
    public String getDataSourceName() { return "documentation"; }
    public TableSchema getTableSchema() { /* custom */ }
    public DecayConfig getDecayConfig() { /* permanent */ }
}

// Plugin 3: Recipes DB
@MemoryType("RECIPES")
public class RecipePlugin implements MemoryPlugin {
    public String getDataSourceName() { return "recipes"; }
    public TableSchema getTableSchema() { /* custom */ }
    public DecayConfig getDecayConfig() { /* permanent */ }
}
```

**Result**:
- 3 separate databases
- Each with custom tables
- Each with custom settings
- ✅ **Full isolation!**

---

## 💡 **Key Insights**

### **1. Settings are NOT tied to DataSource**

```java
// ❌ WRONG THINKING
"If 2 plugins use same database, they must have same settings"

// ✅ CORRECT
"DataSource = WHERE to store"
"Settings = HOW to manage"
// These are INDEPENDENT!
```

### **2. TableSchema enables same-DB separation**

```java
// Same DB, different tables = logical separation without physical cost
RecipePlugin     → db:default, table:recipe_memories
UserPrefPlugin   → db:default, table:memories
```

### **3. All combinations work**

```
Same DB + Same Table + Same Settings     ✅ (redundant but works)
Same DB + Same Table + Different Settings ✅ (logical only)
Same DB + Diff Table + Same Settings     ✅ (physical separation)
Same DB + Diff Table + Diff Settings     ✅ (full separation)
Diff DB + Any Table  + Any Settings      ✅ (complete isolation)
```

---

## 🚀 **Production Recommendations**

### **Small Projects** (< 100k memories)
```java
// Use single DB, single table, different settings
multi-datasource.enabled = false
All plugins use TableSchema.DEFAULT
Differentiate via decay/query configs only
```

### **Medium Projects** (100k - 1M memories)
```java
// Use single DB, multiple tables
multi-datasource.enabled = false
Each plugin defines custom TableSchema
Custom indexes per use case
```

### **Large Projects** (> 1M memories)
```java
// Use multiple DBs
multi-datasource.enabled = true
Critical data → separate databases
Heavy queries → dedicated DB instances
```

---

## 📝 **Summary**

✅ **Yes, plugins CAN have different settings while using the same database!**

**Architecture provides 3 levels of flexibility**:
1. **DataSource** - Physical database separation
2. **TableSchema** - Physical table separation
3. **Configs** - Logical behavior separation

**Each level is independent** - mix and match as needed!

---

**Made with ❤️ for the Memorix community**

