# 🚀 Memorix - Setup Instructions

## 1️⃣ Prerequisites
- **Java 17+**
- **Docker Desktop** (for PostgreSQL + pgvector)
- **OpenAI API Key** (get from https://platform.openai.com/api-keys)

---

## 2️⃣ Quick Setup

### Step 1: Clone Repository
```bash
git clone https://github.com/YOUR_USERNAME/memorix.git
cd memorix
```

### Step 2: Configure Secrets
```bash
# Copy example file
cp secrets.yml.example secrets.yml

# Edit and add your OpenAI API key
notepad secrets.yml  # Windows
# or
vim secrets.yml      # Linux/Mac
```

**secrets.yml should look like:**
```yaml
memorix:
  embedding:
    openai:
      api-key: sk-proj-YOUR_REAL_KEY_HERE
```

⚠️ **IMPORTANT**: `secrets.yml` is gitignored and will NEVER be committed!

### Step 3: Start Database
```bash
# Option A: Using docker-compose (recommended)
docker-compose up -d postgres

# Option B: Manual PostgreSQL with pgvector
cd databases
docker-compose up -d
```

### Step 4: Run Tests
```bash
mvn clean test
```

### Step 5: Start Application
```bash
# Development mode
mvn spring-boot:run -f memorix-core/pom.xml

# Or build and run
mvn clean package
java -jar memorix-core/target/memorix-core-1.0.0-SNAPSHOT-exec.jar
```

🎉 **Done!** API running at http://localhost:8080

---

## 3️⃣ Docker Deployment

### Full Stack (App + Database)
```bash
# Make sure secrets.yml exists
cp secrets.yml.example secrets.yml
# Edit secrets.yml with your API key

# Start everything
docker-compose up -d

# View logs
docker logs -f memorix-app

# Stop
docker-compose down
```

---

## 4️⃣ Verify Installation

### Check API
```bash
curl http://localhost:8080/api/memorix/plugins
```

Expected output:
```json
["USER_PREFERENCE","DOCUMENTATION","CONVERSATION"]
```

### Test Memory Storage
```bash
curl -X POST http://localhost:8080/api/memorix/memories \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "content": "I love pizza",
    "pluginType": "USER_PREFERENCE",
    "importance": 0.9
  }'
```

---

## 5️⃣ Configuration Files

| File | Purpose | Tracked in Git? |
|------|---------|----------------|
| `secrets.yml.example` | Template for secrets | ✅ Yes |
| `secrets.yml` | **YOUR REAL API KEYS** | ❌ **NO - GITIGNORED!** |
| `application.yml` | Public configuration | ✅ Yes |
| `.gitignore` | Protects secrets | ✅ Yes |

---

## 6️⃣ Troubleshooting

### "OpenAI API key not configured"
```bash
# Check if secrets.yml exists
ls secrets.yml

# Verify format
cat secrets.yml
```

### "Connection to database failed"
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Start database
docker-compose up -d postgres
```

### Tests failing
```bash
# Clean and rebuild
mvn clean test

# Tests use Testcontainers - Docker must be running!
docker version
```

---

## 7️⃣ Next Steps

- 📖 Read [USAGE.md](docs/guides/USAGE.md) for API examples
- 🐳 Check [DOCKER_QUICK_START.md](docs/guides/DOCKER_QUICK_START.md) for deployment
- 🧪 Explore [Playground](http://localhost:8080/playground/) for interactive demo
- 📚 Browse [Project Vision](docs/PROJECT_VISION.md) for architecture

---

## 🔐 Security Reminder

**NEVER commit `secrets.yml` to git!**

The file is already in `.gitignore`, but always verify:
```bash
git status
# secrets.yml should NOT appear in "Changes to be committed"
```

If you accidentally committed secrets:
```bash
# Remove from git history (DANGEROUS - consult git docs)
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch secrets.yml" \
  --prune-empty --tag-name-filter cat -- --all
```

---

**Happy coding! 🎉**

