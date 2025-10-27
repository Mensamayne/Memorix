# Embedding Provider Configuration Guide

## Overview

Memorix supports multiple embedding providers through a flexible, property-based configuration system. Providers are loaded conditionally based on your configuration, ensuring clean startups without unnecessary dependencies.

## Available Providers

### 1. Mock Provider (Development/Testing)
**Best for:** Local development, testing, CI/CD pipelines

**Activation:**
```yaml
memorix:
  embedding:
    provider: mock
```

**Features:**
- ✅ No external dependencies
- ✅ Deterministic embeddings (based on text hash)
- ✅ Fast (no network calls)
- ✅ Perfect for testing
- ❌ Not suitable for production

**How it works:**
Generates pseudo-random vectors based on the input text's hash code, ensuring the same input always produces the same embedding.

---

### 2. OpenAI Provider (Production)
**Best for:** Production deployments, real semantic search

**Activation:**
```yaml
memorix:
  embedding:
    provider: openai
    openai:
      api-key: sk-proj-your-key-here
      model: text-embedding-3-small  # or text-embedding-3-large
      base-url: https://api.openai.com/v1
      timeout: 30000  # milliseconds
      max-retries: 3
```

**Features:**
- ✅ High-quality embeddings
- ✅ Production-ready
- ✅ Automatic retry logic
- ✅ Configurable timeout
- ❌ Requires API key
- ❌ Costs money per request

**Security:** Store your API key in `secrets.yml` (gitignored):
```yaml
# secrets.yml
memorix:
  embedding:
    openai:
      api-key: sk-proj-your-actual-key-here
```

---

## Configuration Examples

### Local Development (No API Key)
`application.yml`:
```yaml
memorix:
  embedding:
    provider: mock
```

### Production (With OpenAI)
`application.yml`:
```yaml
spring:
  config:
    import: optional:file:./secrets.yml

memorix:
  embedding:
    provider: openai
    openai:
      model: text-embedding-3-small
      timeout: 30000
      max-retries: 3
```

`secrets.yml` (gitignored):
```yaml
memorix:
  embedding:
    openai:
      api-key: sk-proj-...
```

### Test Environment
`application-test.yml`:
```yaml
memorix:
  embedding:
    provider: mock

spring:
  profiles:
    active: test
```

---

## Provider Selection Logic

```
┌─────────────────────────────────────────┐
│ memorix.embedding.provider = ?          │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴───────┐
       │               │
  "mock"          "openai"
       │               │
       ▼               ▼
 ┌──────────┐   ┌──────────────┐
 │   Mock   │   │    OpenAI    │
 │ Provider │   │   Provider   │
 └──────────┘   └──────────────┘
       │               │
       ▼               ▼
  No external     Requires API
  dependencies       key
```

---

## Implementation Details

### Conditional Bean Loading

**MockEmbeddingProvider:**
```java
@Component
@ConditionalOnProperty(
    name = "memorix.embedding.provider",
    havingValue = "mock",
    matchIfMissing = true  // Default if property not set
)
public class MockEmbeddingProvider implements EmbeddingProvider {
    // ...
}
```

**OpenAIEmbeddingProvider:**
```java
@Component
@ConditionalOnProperty(
    name = "memorix.embedding.provider",
    havingValue = "openai"
)
public class OpenAIEmbeddingProvider implements EmbeddingProvider {
    // ...
}
```

### What This Means:
- Only ONE provider bean is created at startup
- No "bean not found" errors
- No unnecessary API key validation
- Clean, fast startups

---

## Switching Providers

### From Mock to OpenAI (Going to Production)

1. **Add secrets.yml:**
   ```bash
   echo "memorix:" > secrets.yml
   echo "  embedding:" >> secrets.yml
   echo "    openai:" >> secrets.yml
   echo "      api-key: sk-proj-YOUR-KEY" >> secrets.yml
   ```

2. **Update application.yml:**
   ```yaml
   memorix:
     embedding:
       provider: openai  # Changed from mock
   ```

3. **Restart application**

### From OpenAI to Mock (For Development)

1. **Update application.yml:**
   ```yaml
   memorix:
     embedding:
       provider: mock  # Changed from openai
   ```

2. **Restart application** (no API key needed)

---

## Troubleshooting

### Problem: "No bean of type EmbeddingProvider found"
**Cause:** No provider is configured

**Solution:** Set `memorix.embedding.provider` to either "mock" or "openai"

---

### Problem: "OpenAI API key not configured"
**Cause:** Provider is set to "openai" but no API key

**Solution:** 
1. Create `secrets.yml` with your API key
2. OR switch to mock provider

---

### Problem: "Connection refused" from OpenAI
**Causes:**
- No internet connection
- API key is invalid
- Rate limited

**Solutions:**
1. Check internet connectivity
2. Verify API key in secrets.yml
3. Check OpenAI dashboard for quota/limits
4. Temporarily switch to mock provider

---

## Best Practices

### ✅ DO:
- Use mock provider in development
- Use OpenAI provider in production
- Store API keys in `secrets.yml` (gitignored)
- Set reasonable timeouts (30s recommended)
- Enable retries (3 recommended)

### ❌ DON'T:
- Commit API keys to git
- Use mock provider in production
- Set very low timeouts (<10s)
- Disable retries in production

---

## Future Providers

Memorix is designed to support additional providers. Coming soon:

- **Ollama** (self-hosted, local models)
- **Cohere** (alternative cloud provider)
- **Azure OpenAI** (enterprise option)
- **Custom** (bring your own embedding model)

To add a new provider, implement `EmbeddingProvider` interface and add `@ConditionalOnProperty` annotation.

---

## Summary

```yaml
# Development: Fast, free, no setup
memorix:
  embedding:
    provider: mock

# Production: High quality, requires API key
memorix:
  embedding:
    provider: openai

# Test: Always mock
# (set in application-test.yml)
```

**The system automatically loads only the configured provider - no manual bean configuration needed!**

