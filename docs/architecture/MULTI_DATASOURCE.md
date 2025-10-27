# 🗄️ Multi-DataSource Support

**Complete guide to using multiple databases with Memorix plugins**

---

## 🎯 Overview

Memorix now supports **pluginowe zarządzanie wieloma bazami danych** - każdy plugin może deklarować własną bazę danych i strukturę tabeli.

### Use Cases

✅ **Kiedy używać wielu baz:**
- Różne polityki retencji (dokumentacja = permanent, rozmowy = 30 dni)
- Różne wymagania bezpieczeństwa (docs = restricted access)
- Różne zespoły zarządzają różnymi typami pamięci
- Niezależne skalowanie (ciężkie zapytania docs vs lekkie preferences)
- Wymagania regulacyjne (dane osobowe w oddzielnej bazie)

❌ **Kiedy NIE używać:**
- Mały projekt z podobnymi wymaganiami
- Prostota > funkcjonalność
- Wszystkie typy mają podobne wzorce dostępu

---

## 🚀 Quick Start

### 1. Włącz multi-datasource w aplikacji

```yaml
# application.yml
memorix:
  # KROK 1: Włącz multi-datasource routing
  multi-datasource:
    enabled: true  # ← To aktywuje routing!
  
  # KROK 2: Skonfiguruj datasources
  datasources:
    default:
      url: jdbc:postgresql://localhost:5432/memorix
      username: postgres
      password: postgres
      hikari:
        maximum-pool-size: 10
        minimum-idle: 2
      
    documentation:
      url: jdbc:postgresql://localhost:5432/memorix_docs
      username: postgres
      password: postgres
      hikari:
        maximum-pool-size: 5  # Mniejszy pool dla docs
        minimum-idle: 1
      
    recipes:
      url: jdbc:postgresql://localhost:5432/memorix_recipes
      username: postgres
      password: postgres
      hikari:
        maximum-pool-size: 20  # Większy pool dla recipes
        minimum-idle: 5
```

### 1a. Przygotuj bazy danych

```sql
-- PostgreSQL setup
CREATE DATABASE memorix;
CREATE DATABASE memorix_docs;
CREATE DATABASE memorix_recipes;

-- W każdej bazie:
\c memorix
CREATE EXTENSION IF NOT EXISTS vector;

\c memorix_docs
CREATE EXTENSION IF NOT EXISTS vector;

\c memorix_recipes
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Deklaracja datasource w pluginie

```java
@Component
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "documentation";  // Używa memorix_docs
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("doc_memories")
            .vectorDimension(1536)
            .addCustomColumn("doc_version VARCHAR(50)")
            .addCustomColumn("doc_category VARCHAR(100)")
            .addCustomIndex("CREATE INDEX idx_category ON doc_memories(doc_category)")
            .build();
    }
}
```

### 3. Użycie (automatyczne routowanie!)

```java
// Automatycznie zapisane do memorix_docs
Memory doc = memoryService.save(
    "user123",
    "API endpoint /users returns user list",
    "DOCUMENTATION"
);

// Automatycznie zapisane do memorix_recipes  
Memory recipe = memoryService.save(
    "user123",
    "Pizza Margherita recipe",
    "RECIPE"
);
```

---

## 🏗️ Architektura

### Routing Flow

```
┌─────────────────────┐
│  Application Code   │
│  memoryService.save │
└──────────┬──────────┘
           │
    ┌──────▼───────────────────┐
    │    MemoryService         │
    │  1. Get plugin           │
    │  2. Set datasource ctx   │
    └──────────┬───────────────┘
               │
    ┌──────────▼──────────────────┐
    │  PluginDataSourceRouter     │
    │  Determines target database │
    └──┬───────────┬──────────┬───┘
       │           │          │
   ┌───▼───┐  ┌───▼────┐  ┌──▼─────┐
   │ DB 1  │  │  DB 2  │  │  DB 3  │
   │(def)  │  │ (docs) │  │(recipe)│
   └───────┘  └────────┘  └────────┘
```

### Components

1. **PluginDataSourceContext** - ThreadLocal przechowujący aktualny datasource
2. **PluginDataSourceRouter** - AbstractRoutingDataSource wybierający bazę
3. **MultiDataSourceConfig** - Konfiguracja Spring DataSource
4. **TableSchema** - Model definiujący strukturę tabeli

---

## 📖 API Reference

### MemoryPlugin Methods

#### `getDataSourceName()`

```java
@Override
public String getDataSourceName() {
    return "documentation";  // Nazwa datasource z application.yml
}
```

**Returns:** Nazwa datasource (musi być skonfigurowana w YAML)  
**Default:** `"default"`

#### `getTableSchema()`

```java
@Override
public TableSchema getTableSchema() {
    return TableSchema.builder()
        .tableName("doc_memories")          // Nazwa tabeli
        .vectorDimension(1536)              // Wymiar wektora embeddings
        .addCustomColumn("version VARCHAR(50)")
        .addCustomIndex("CREATE INDEX idx_version ON doc_memories(version)")
        .build();
}
```

**Returns:** Schema tabeli z custom kolumnami i indeksami  
**Default:** `TableSchema.DEFAULT` (uses `memories` table)

---

## 🎯 Examples

### Example 1: Documentation Plugin

```java
@Component
@MemoryType("DOCUMENTATION")
public class DocumentationPlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "documentation";
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("documentation_memories")
            .vectorDimension(1536)
            .addCustomColumn("doc_version VARCHAR(50)")
            .addCustomColumn("doc_category VARCHAR(100)")
            .addCustomColumn("last_reviewed TIMESTAMP")
            .addCustomIndex("CREATE INDEX idx_doc_version ON documentation_memories(doc_version)")
            .addCustomIndex("CREATE INDEX idx_doc_category ON documentation_memories(doc_category)")
            .build();
    }
    
    @Override
    public DecayConfig getDecayConfig() {
        return DecayConfig.builder()
            .strategyClassName("io.memorix.lifecycle.PermanentDecayStrategy")
            .initialDecay(100)
            .autoDelete(false)  // Documentation never expires
            .build();
    }
}
```

### Example 2: Recipe Plugin

```java
@Component
@MemoryType("RECIPE")
public class RecipePlugin implements MemoryPlugin {
    
    @Override
    public String getDataSourceName() {
        return "recipes";
    }
    
    @Override
    public TableSchema getTableSchema() {
        return TableSchema.builder()
            .tableName("recipe_memories")
            .vectorDimension(768)  // Smaller embeddings for recipes
            .addCustomColumn("cuisine VARCHAR(100)")
            .addCustomColumn("difficulty VARCHAR(50)")
            .addCustomColumn("prep_time INTEGER")
            .addCustomIndex("CREATE INDEX idx_cuisine ON recipe_memories(cuisine)")
            .build();
    }
}
```

---

## 🔧 Configuration

### Pełna konfiguracja krok po kroku

#### Krok 1: Włącz multi-datasource

```yaml
# application.yml
memorix:
  multi-datasource:
    enabled: true  # ← KRYTYCZNE: To włącza routing!
```

**Bez `enabled: true` routing NIE DZIAŁA!** System używa wtedy standardowej single-database konfiguracji.

#### Krok 2: Pełna konfiguracja

```yaml
spring:
  application:
    name: memorix-app

memorix:
  # KROK 1: Włącz multi-datasource
  multi-datasource:
    enabled: true
  
  # KROK 2: Skonfiguruj datasources
  datasources:
    default:
      url: jdbc:postgresql://localhost:5432/memorix
      username: postgres
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 10
        minimum-idle: 2
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        
    documentation:
      url: jdbc:postgresql://docs-db.example.com:5432/memorix_docs
      username: docs_user
      password: ${DOCS_DB_PASSWORD}
      hikari:
        maximum-pool-size: 5   # Mniejszy pool
        minimum-idle: 1
        
    recipes:
      url: jdbc:postgresql://recipes-db.example.com:5432/memorix_recipes
      username: recipes_user
      password: ${RECIPES_DB_PASSWORD}
      hikari:
        maximum-pool-size: 20  # Większy pool dla ruchu
        minimum-idle: 5
        
  # KROK 3: Embedding provider
  embedding:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model: text-embedding-3-small

# KROK 4: Flyway (opcjonalne)
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

### Szybki start z przykładową konfiguracją

```bash
# Użyj gotowego profilu
java -jar memorix.jar --spring.profiles.active=multids

# Lub przekopiuj
cp src/main/resources/application-multids.yml application.yml
```

---

## 🧪 Testing

### Integration Test

```java
@SpringBootTest
@ActiveProfiles("test")
class MultiDataSourceIntegrationTest {
    
    @Autowired
    private MemoryService memoryService;
    
    @Test
    void testPluginUsesCorrectDataSource() {
        // Save to documentation database
        Memory doc = memoryService.save(
            "user123",
            "API documentation",
            "DOCUMENTATION"
        );
        
        // Save to recipes database
        Memory recipe = memoryService.save(
            "user123",
            "Pizza recipe",
            "RECIPE"
        );
        
        // Both saved successfully to different databases
        assertNotNull(doc.getId());
        assertNotNull(recipe.getId());
    }
}
```

---

## 💡 Best Practices

### 1. Naming Conventions

```java
// ✅ GOOD: Clear datasource names
getDataSourceName() { return "documentation"; }
getDataSourceName() { return "recipes"; }

// ❌ BAD: Generic names
getDataSourceName() { return "db1"; }
getDataSourceName() { return "datasource"; }
```

### 2. Fallback to Default

```java
@Override
public String getDataSourceName() {
    // If 'documentation' not configured, falls back to 'default'
    return "documentation";
}
```

### 3. Custom Columns

```java
// ✅ GOOD: Meaningful custom columns
.addCustomColumn("doc_version VARCHAR(50)")
.addCustomColumn("last_reviewed TIMESTAMP")

// ❌ BAD: Generic columns without purpose
.addCustomColumn("field1 VARCHAR(100)")
.addCustomColumn("data TEXT")
```

### 4. Indexes

```java
// ✅ GOOD: Index frequently queried columns
.addCustomIndex("CREATE INDEX idx_category ON memories(category)")

// ❌ BAD: Index everything
.addCustomIndex("CREATE INDEX idx_content ON memories(content)")  // Full-text!
```

---

## 🚨 Troubleshooting

### Issue: "Unknown datasource 'documentation'"

**Przyczyna:** Datasource nie jest skonfigurowany w `application.yml`

**Rozwiązanie:**
```yaml
memorix:
  datasources:
    documentation:
      url: jdbc:postgresql://...
```

### Issue: Plugin uses wrong database

**Przyczyna:** DataSource context nie jest ustawiony

**Rozwiązanie:** MemoryService automatycznie ustawia kontekst - upewnij się, że używasz `memoryService.save()` zamiast bezpośrednio `memoryStore.save()`

### Issue: Custom columns not created

**Przyczyna:** Migracje Flyway nie są automatycznie generowane dla custom schemas

**Rozwiązanie:** Obecnie custom columns są deklaratywne - tworzenie tabel z custom columns będzie dodane w przyszłości. Na razie używaj domyślnej struktury.

---

## 🔮 Future Enhancements

Planowane funkcjonalności:

- ✅ Automatyczne tworzenie tabel z custom schema
- ✅ Migracje Flyway per datasource
- ✅ Schema versioning
- ✅ Multi-database transactions (XA)
- ✅ Read replicas support
- ✅ Sharding per plugin type

---

## 📚 Related Documentation

- **[README.md](README.md)** - Quick overview
- **[USAGE.md](USAGE.md)** - Complete usage guide
- **[PROJECT_VISION.md](PROJECT_VISION.md)** - Architecture & design

---

**Questions?** Check [USAGE.md](USAGE.md) or open an issue on GitHub.

