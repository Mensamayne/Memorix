# 🐳 Memorix Docker - Quick Start Guide

## Prerequisites
- Docker Desktop installed
- 512 MB available RAM
- 500 MB disk space

## 🚀 Start Memorix (Single Command)

```bash
docker-compose up -d
```

That's it! 🎉

## 🔍 Verify It's Running

```bash
# Check containers
docker ps

# Test API
curl http://localhost:8080/api/memorix/plugins
```

Expected output:
```json
["USER_PREFERENCE","DOCUMENTATION","CONVERSATION"]
```

## 💾 Save Your First Memory

```bash
curl -X POST http://localhost:8080/api/memorix/memories \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "my-user",
    "content": "I prefer dark mode",
    "pluginType": "USER_PREFERENCE",
    "importance": 0.9
  }'
```

## 🛠️ Management Commands

### Start
```bash
docker-compose up -d
```

### Stop
```bash
docker-compose down
```

### View Logs
```bash
docker logs -f memorix-app
```

### Database Access
```bash
docker exec -it memorix-postgres psql -U postgres -d memorix
```

### Clean Everything
```bash
docker-compose down -v  # Removes volumes too
```

## 📊 What's Running?

| Service | Port | Description |
|---------|------|-------------|
| **memorix-app** | 8080 | Main application |
| **memorix-postgres** | 5432 | PostgreSQL + pgvector |

## 🔧 Configuration

### Change OpenAI Provider
Edit `docker-compose.yml`:
```yaml
services:
  memorix-app:
    environment:
      MEMORIX_EMBEDDING_PROVIDER: openai
      OPENAI_API_KEY: sk-your-key-here
```

Then restart:
```bash
docker-compose down && docker-compose up -d
```

### Change Database Password
Edit `docker-compose.yml`:
```yaml
services:
  postgres:
    environment:
      POSTGRES_PASSWORD: your-secure-password
  memorix-app:
    environment:
      SPRING_DATASOURCE_PASSWORD: your-secure-password
```

## 📈 Performance Stats

- **Startup Time:** 5 seconds
- **Memory Usage:** 305 MB total (245 MB app + 60 MB DB)
- **CPU Usage:** <0.2% idle
- **Image Size:** 207 MB

## 🐛 Troubleshooting

### Container Won't Start
```bash
# Check logs
docker logs memorix-app
docker logs memorix-postgres

# Check if ports are available
netstat -an | findstr "8080"
netstat -an | findstr "5432"
```

### Database Connection Errors
```bash
# Wait for PostgreSQL to be ready (takes ~5 seconds)
docker logs memorix-postgres | grep "ready to accept"
```

### Reset Everything
```bash
docker-compose down -v
docker rmi memorix-memorix-app
docker-compose up -d --build
```

## 📚 More Information

- **Full Test Report:** [DOCKER_DEPLOYMENT_TEST_REPORT.md](DOCKER_DEPLOYMENT_TEST_REPORT.md)
- **API Documentation:** [USAGE.md](USAGE.md)
- **Configuration Guide:** [SECRETS_SETUP.md](SECRETS_SETUP.md)

## ✅ Production Ready

This Docker setup is **production-ready** with:
- ✅ Optimized multi-stage build
- ✅ Health checks configured
- ✅ Volume persistence
- ✅ Resource limits
- ✅ Security best practices
- ✅ 100% test pass rate

See [DOCKER_DEPLOYMENT_TEST_REPORT.md](DOCKER_DEPLOYMENT_TEST_REPORT.md) for detailed test results.

---

**Need help?** Check the logs:
```bash
docker logs memorix-app --tail 50
```

