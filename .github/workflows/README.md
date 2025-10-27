# 🚀 CI/CD Pipeline

## What it does

Every time you push code or create a Pull Request, GitHub Actions automatically:

### 1️⃣ **Tests** (Job 1)
- ✅ Runs 173 tests on CI
- ⚠️ 30 Testcontainer tests skipped (Docker/Testcontainers issues on GitHub Actions)
- ⚠️ 24 OpenAI E2E tests skipped (require real API key)
- ✅ **227 tests pass locally** with Docker
- ✅ Generates code coverage report
- ✅ Uploads coverage to Codecov (optional)
- ⏱️ Takes ~6 minutes

**Skipped on CI (work locally):**
- `ImportancePersistenceTest` (4 tests)
- `SearchFunctionalityIntegrationTest` (7 tests)
- `ValidationErrorHandlingTest` (19 tests)

These require Testcontainers/Docker which has initialization issues on GitHub Actions runners.
All tests pass perfectly in local development environment.

### 2️⃣ **Build** (Job 2)
- ✅ Compiles the entire project
- ✅ Packages to JAR file
- ✅ Uploads artifact for download
- ⏱️ Takes ~2 minutes

### 3️⃣ **Docker** (Job 3)
- ✅ Builds Docker image
- ✅ Verifies image exists
- ⏱️ Takes ~3 minutes

---

## Status Badge

The CI/CD badge in README shows:
- 🟢 **Green** = All tests passing
- 🔴 **Red** = Something failed
- 🟡 **Yellow** = Running

---

## Viewing Results

1. Go to: https://github.com/Mensamayne/Memorix/actions
2. Click on any workflow run
3. See detailed logs for each job

---

## Local Testing

Run the same checks locally before pushing:

```bash
# Run tests
mvn clean test

# Build project
mvn clean package -DskipTests

# Build Docker
docker build -t memorix:test .
```

---

## Configuration

The workflow is defined in `.github/workflows/ci.yml`

**Triggers:**
- Every push to `main` or `master`
- Every Pull Request

**Requirements:**
- Java 17
- Maven
- Docker
- Ubuntu runner

---

## Future Enhancements

Possible additions:
- ✅ Deploy to Docker Hub
- ✅ Publish to Maven Central
- ✅ Run E2E tests with real OpenAI
- ✅ Performance benchmarks
- ✅ Security scanning

