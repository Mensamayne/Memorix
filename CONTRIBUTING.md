# 🤝 Contributing to Memorix

**Welcome! We're excited you want to contribute.**

---

## 🎯 Code of Conduct

Be respectful, constructive, and collaborative. We're building something great together.

---

## 🚀 Getting Started

### 1. Fork & Clone
```bash
git clone https://github.com/your-username/memorix.git
cd memorix
```

### 2. Setup Environment
```bash
# Install PostgreSQL 15+ with pgvector
# Or use Docker:
docker run -d \
  --name memorix-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=memorix_dev \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### 3. Build & Test
```bash
mvn clean install
mvn test
```

### 4. Create Branch
```bash
git checkout -b feature/my-awesome-feature
```

---

## 📝 Development Guidelines

### Code Quality Standards

Follow [CODE_QUALITY.md](CODE_QUALITY.md) strictly:

- ✅ Max 200 lines per class
- ✅ Max 20 lines per method
- ✅ 80%+ test coverage
- ✅ Zero compiler warnings
- ✅ No mocks, real TODOs
- ✅ JavaDoc for public API

### Commit Messages

```bash
# ✅ GOOD
git commit -m "feat: Add OpenAI embedding provider"
git commit -m "fix: Handle null decay config in UsageBasedStrategy"
git commit -m "docs: Update USAGE.md with batch examples"

# ❌ BAD
git commit -m "stuff"
git commit -m "fixed bug"
git commit -m "WIP"
```

**Format**: `type: description`

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `test`: Tests
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `chore`: Build/tooling

---

## 🧪 Testing Requirements

### Every PR Must Have

1. **Unit Tests**
   ```java
   @Test
   void shouldDoSomething() {
       // Arrange
       // Act
       // Assert
   }
   ```

2. **Integration Tests** (if touching storage/database)
   ```java
   @SpringBootTest
   @Testcontainers
   class MyIntegrationTest {
       @Container
       static PostgreSQLContainer<?> postgres = ...
   }
   ```

3. **Coverage Check**
   ```bash
   mvn jacoco:report
   # Open target/site/jacoco/index.html
   # Ensure > 80% for your changes
   ```

---

## 🔧 What to Contribute

### High Priority

- **Embedding Providers** (OpenAI, Ollama, Cohere)
- **Advanced Decay Strategies**
- **Performance Optimizations**
- **Documentation Improvements**
- **Example Projects**

### Medium Priority

- **Additional Plugins** (Email, Code, etc.)
- **Query Optimizations**
- **Monitoring/Metrics**
- **CLI Tools**

### Low Priority

- **UI Dashboard**
- **GraphQL API**
- **Multi-tenancy**

---

## 📋 Pull Request Process

### 1. Before Submitting

- [ ] All tests pass (`mvn test`)
- [ ] Coverage > 80% for new code
- [ ] No compiler warnings
- [ ] JavaDoc added for public API
- [ ] Code follows style guide
- [ ] Commit messages follow convention
- [ ] Branch is up to date with main

### 2. PR Template

```markdown
## Description
Brief description of changes

## Type
- [ ] Feature
- [ ] Bug fix
- [ ] Documentation
- [ ] Refactoring

## Changes
- Added X
- Fixed Y
- Updated Z

## Testing
- Added 5 unit tests
- Added 2 integration tests
- Coverage: 85%

## Checklist
- [ ] Tests pass
- [ ] Coverage > 80%
- [ ] JavaDoc complete
- [ ] No warnings
- [ ] Follows code quality standards
```

### 3. Review Process

1. Automated checks run (GitHub Actions)
2. Code review by maintainer
3. Revisions if needed
4. Approval
5. Merge

---

## 🏆 Recognition

Contributors will be:
- Listed in README.md
- Credited in release notes
- Given GitHub badges
- Thanked publicly

---

## 📚 Resources

### Documentation
- [PROJECT_VISION.md](PROJECT_VISION.md) - Architecture
- [CODE_QUALITY.md](CODE_QUALITY.md) - Standards
- [DECAY_STRATEGIES.md](DECAY_STRATEGIES.md) - Lifecycle guide
- [QUERY_LIMITS.md](QUERY_LIMITS.md) - Search optimization
- [USAGE.md](USAGE.md) - Usage tutorial

### Development
- [UNFINISHED_BRIDGES.md](UNFINISHED_BRIDGES.md) - Current TODOs
- Test examples in `src/test/java/`
- Phase completion docs: `PHASE*_COMPLETE.md`

---

## 💬 Communication

### Before Starting

- Check existing issues/PRs
- Comment on issue you want to work on
- Discuss major changes first

### While Working

- Ask questions early
- Share progress
- Request feedback

### After Submitting

- Respond to reviews promptly
- Be open to feedback
- Make requested changes

---

## 🎯 Good First Issues

Look for issues tagged:
- `good-first-issue`
- `help-wanted`
- `documentation`

Examples:
- Add usage examples
- Improve JavaDoc
- Write blog posts
- Create tutorials
- Fix typos

---

## 🚫 What We Don't Accept

- Code without tests
- Breaking changes without discussion
- Dependency bloat
- Mocks instead of TODOs
- Uncommented complex code
- Poor naming
- God classes

---

## 📜 License

By contributing, you agree your contributions will be licensed under MIT License.

---

## 🙏 Thank You!

Every contribution makes Memorix better. Whether it's code, docs, or bug reports - we appreciate it!

**Let's build something great together.**

---

*Questions? Open a discussion or issue!*

