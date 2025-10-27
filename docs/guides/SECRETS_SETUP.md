# 🔐 Secrets Configuration - Memorix

**Secure configuration for internal microservice**

---

## 📁 **PLIKI**

### 1. `secrets.yml` (GITIGNORED!)
```yaml
# ⚠️ NIGDY NIE COMMITUJ DO GIT! ⚠️

memorix:
  embedding:
    openai:
      api-key: sk-proj-YOUR_OPENAI_API_KEY_HERE
```

**Lokalizacja**: `./secrets.yml` (w root projektu)

---

### 2. `.gitignore` (CHRONI SEKRETY!)
```
secrets.yml
secrets-*.yml
*.env
.env
```

**Lokalizacja**: `./.gitignore`

---

### 3. `application.yml` (PUBLICZNY - commitowany)
```yaml
spring:
  config:
    import: optional:file:./secrets.yml  # ← Importuje sekrety!

memorix:
  embedding:
    provider: openai  # mock | openai | ollama
    openai:
      model: text-embedding-3-small
      base-url: https://api.openai.com/v1
      timeout: 30000
      max-retries: 3
```

**Lokalizacja**: `./memorix-core/src/main/resources/application.yml`

---

## 🚀 **JAK TO DZIAŁA**

### 1. Spring Boot uruchamia się
```
Spring Boot → Czyta application.yml
            → Widzi spring.config.import
            → Importuje secrets.yml
            → Merguje konfigurację
```

### 2. EmbeddingConfig się tworzy
```java
@ConfigurationProperties(prefix = "memorix.embedding")
public class EmbeddingConfig {
    private OpenAIConfig openai;  // ← Zawiera api-key z secrets.yml!
}
```

### 3. OpenAIEmbeddingProvider startuje
```java
public OpenAIEmbeddingProvider(EmbeddingConfig config) {
    this.apiKey = config.getOpenai().getApiKey();  // ← Z secrets.yml!
    // Validation, HTTP client setup...
}
```

### 4. Użytkownik zapisuje memory
```java
memorix.store("user123")
    .content("User loves pizza")
    .save();

// Memorix automatycznie:
// → OpenAIEmbeddingProvider.embed("User loves pizza")
// → HTTP POST do api.openai.com (z kluczem z config!)
// → Zwraca float[1536]
// → Zapisuje do PostgreSQL
```

**User NIGDY nie widzi klucza!** ✅

---

## 🔒 **SECURITY**

### ✅ Co jest bezpieczne:

**1. secrets.yml gitignored**
```bash
$ git status
# secrets.yml NIE pojawi się!
```

**2. Klucz tylko w config**
```java
// User code - ZERO mention klucza
memorix.store("user").content("text").save();
```

**3. Internal microservice**
- Nie exposed do internetu
- Network isolation
- Single tenant
- Jeden klucz dla całego serwisu

---

### ⚠️ Co MUSI być przestrzegane:

**1. NIE commituj secrets.yml**
```bash
# ZAWSZE sprawdź przed commit:
git status
# secrets.yml NIE może być staged!
```

**2. NIE hardcode klucza w kodzie**
```java
// ❌ NIGDY
String key = "sk-proj-...";

// ✅ ZAWSZE z config
@Autowired EmbeddingConfig config;
String key = config.getOpenai().getApiKey();
```

**3. NIE loguj klucza**
```java
// ❌ NIGDY
log.info("API Key: " + apiKey);

// ✅ OK
log.info("OpenAI provider initialized");
```

---

## 🎯 **DEPLOYMENT**

### Środowisko lokalne (dev):
```bash
# 1. Utwórz secrets.yml w root
cp secrets.yml.example secrets.yml

# 2. Edytuj klucz
vim secrets.yml

# 3. Uruchom
mvn spring-boot:run
```

---

### Docker:
```dockerfile
# Dockerfile
FROM openjdk:17-slim
COPY target/memorix-core.jar app.jar

# Secrets przez volume mount
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# docker-compose.yml
services:
  memorix:
    image: memorix:latest
    volumes:
      - ./secrets.yml:/app/secrets.yml:ro  # Read-only!
    environment:
      - SPRING_CONFIG_IMPORT=optional:file:/app/secrets.yml
```

---

### Kubernetes:
```yaml
# Secret
apiVersion: v1
kind: Secret
metadata:
  name: memorix-secrets
type: Opaque
stringData:
  OPENAI_API_KEY: sk-proj-...

---

# Deployment
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: memorix
        env:
        - name: MEMORIX_EMBEDDING_OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: memorix-secrets
              key: OPENAI_API_KEY
```

---

## 🔄 **PROVIDER SWITCHING**

### Mock (dla testów):
```yaml
# application-test.yml
memorix:
  embedding:
    provider: mock  # ← Deterministic, fast
```

### OpenAI (produkcja):
```yaml
# application.yml
memorix:
  embedding:
    provider: openai  # ← Real embeddings

# secrets.yml
memorix:
  embedding:
    openai:
      api-key: sk-proj-...
```

### Ollama (local, future):
```yaml
# application-dev.yml
memorix:
  embedding:
    provider: ollama  # ← Future implementation
    ollama:
      base-url: http://localhost:11434
      model: nomic-embed-text
```

---

## 📊 **MONITORING**

### Logi przy starcie:
```
2025-10-14 20:00:00 INFO  OpenAIEmbeddingProvider : OpenAI Embedding Provider initialized (model: text-embedding-3-small, dimension: 1536)
```

### Logi przy embedding:
```
2025-10-14 20:00:01 DEBUG OpenAIEmbeddingProvider : Generated embedding for text (length: 24, dimension: 1536)
```

### Logi przy błędzie:
```
2025-10-14 20:00:02 WARN  OpenAIEmbeddingProvider : Embedding generation failed (attempt 1/3): Connection timeout
2025-10-14 20:00:03 ERROR EmbeddingException : Failed to generate embedding after 3 attempts
```

---

## ⚡ **PERFORMANCE**

| Operation | Time | Cost (OpenAI) |
|-----------|------|---------------|
| Generate embedding | ~200ms | $0.00002 |
| Save with embedding | ~250ms | $0.00002 |
| Search (no new embed) | ~20ms | $0 |

**Monthly estimate** (10k saves/day):
- API calls: 300,000/month
- Cost: ~$6/month
- Bardzo tanie! ✅

---

## 🆘 **TROUBLESHOOTING**

### "OpenAI API key not configured"
```bash
# Check if secrets.yml exists
ls secrets.yml

# Check content
cat secrets.yml

# Verify import in application.yml
grep "spring.config.import" application.yml
```

### "Provider openai not found"
```bash
# Check provider property
grep "provider:" application.yml

# Should be: provider: openai (not mock!)
```

### "Connection timeout"
```yaml
# Increase timeout
memorix:
  embedding:
    openai:
      timeout: 60000  # 60 seconds
```

---

## ✅ **CHECKLIST**

Przed deployment:

- [x] secrets.yml utworzony
- [x] .gitignore zawiera secrets.yml
- [x] application.yml importuje secrets
- [x] API key jest poprawny
- [x] Provider ustawiony na "openai"
- [ ] Przetestuj prawdziwy OpenAI call
- [ ] Zweryfikuj że secrets.yml NIE jest w git

---

**Gotowe! OpenAI provider skonfigurowany i zabezpieczony!** 🔒✅

