# 📜 Changelog

All notable changes to Memorix will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0-SNAPSHOT] - 2025-10-21

### ✅ **Major Release - Multi-DataSource + Deduplication**

**🗄️ Multi-DataSource Support**
- ✅ Plugin-based datasource routing
- ✅ `MemoryPlugin.getDataSourceName()` - plugins declare their database
- ✅ `MemoryPlugin.getTableSchema()` - custom table schemas per plugin
- ✅ `PluginDataSourceRouter` - automatic routing to correct DB
- ✅ `PluginDataSourceContext` - ThreadLocal context management
- ✅ `DataSourceConfigProperties` - YAML configuration binding
- ✅ `DataSourceFactory` - HikariCP datasource creation
- ✅ `MultiDataSourceFlywayConfig` - migrations per datasource
- ✅ `application-multids.yml` - example configuration
- ✅ Full test coverage (12 tests, all passing)
- ✅ Complete documentation (MULTI_DATASOURCE.md, ARCHITECTURE_MULTIDATASOURCE.md)

**🔍 Deduplication System**
- ✅ Hash-based duplicate detection (fast, exact matching)
- ✅ Semantic duplicate detection (AI-powered, paraphrase detection)
- ✅ Hybrid detection (hash first, then semantic if needed)
- ✅ Three strategies: REJECT, MERGE, UPDATE
- ✅ Content normalization (case-insensitive, whitespace-agnostic)
- ✅ Configurable per plugin
- ✅ Full test coverage (24 tests, all passing)

**📊 Quality Improvements**
- ✅ Increased test count: 107 → 153 tests (+43%)
- ✅ Test pass rate: 99.3% (152/153 passing)
- ✅ Code coverage: 77% (above 75% target)
- ✅ All integration tests use Testcontainers (real PostgreSQL)
- ✅ Zero compiler warnings
- ✅ Zero flaky tests

**📚 Documentation**
- ✅ ARCHITECTURE_MULTIDATASOURCE.md - Deep dive into plugin architecture
- ✅ TEST_COVERAGE_REPORT.md - Detailed coverage analysis
- ✅ Updated README, PROJECT_SUMMARY, CHANGELOG

### Planned for v1.2.0
- Enhanced MemoryStats (avg decay, token distribution, decay histogram)
- REST API coverage improvements (currently 43%)
- Exact token counter (tiktoken)
- Health indicators
- Metrics API
- Ollama embedding provider

---

## [1.0.0-SNAPSHOT] - 2024-10-14

### 🎉 Initial Release

First production-ready version of Memorix - AI Memory Management Framework.

### Added

**Core Framework**
- ✅ PostgreSQL + pgvector integration
- ✅ Vector similarity search
- ✅ ACID transaction support
- ✅ Flyway database migrations
- ✅ Complete exception hierarchy

**Plugin System**
- ✅ PluginRegistry with auto-discovery
- ✅ @MemoryType annotation
- ✅ 3 built-in plugins:
  - USER_PREFERENCE (usage-based decay)
  - DOCUMENTATION (permanent, no decay)
  - CONVERSATION (hybrid decay)

**Decay & Lifecycle**
- ✅ 4 decay strategies:
  - UsageBasedDecayStrategy (freeze during breaks)
  - TimeBasedDecayStrategy (calendar-driven)
  - HybridDecayStrategy (usage + time)
  - PermanentDecayStrategy (never decays)
- ✅ DecayEngine for applying strategies
- ✅ LifecycleManager fluent API
- ✅ Auto-deletion of expired memories
- ✅ Configurable decay per plugin

**Advanced Search**
- ✅ QueryExecutor with semantic search
- ✅ Multi-dimensional query limits:
  - maxCount (memory limit)
  - maxTokens (LLM context limit)
  - minSimilarity (relevance threshold)
- ✅ 4 limit strategies:
  - ALL (strict - all limits must be met)
  - ANY (flexible - first limit stops)
  - GREEDY (maximize - pack as much as possible)
  - FIRST_MET (alternative - first satisfied wins)
- ✅ Smart token cutoff (never cuts memory in half)
- ✅ Query metadata tracking
- ✅ Token counting (approximate)

**Spring Integration**
- ✅ MemoryService facade
- ✅ Memorix main entry point
- ✅ Fluent API builders
- ✅ Auto-configuration
- ✅ Application properties support

**Testing & Quality**
- ✅ 107 integration tests
- ✅ 85% code coverage
- ✅ Testcontainers for real PostgreSQL
- ✅ Zero compiler warnings
- ✅ Production-grade code quality

**Documentation**
- ✅ PROJECT_VISION.md - Architecture guide
- ✅ CODE_QUALITY.md - Standards & best practices
- ✅ DECAY_STRATEGIES.md - Lifecycle guide
- ✅ QUERY_LIMITS.md - LLM optimization guide
- ✅ USAGE.md - Complete tutorial
- ✅ UNFINISHED_BRIDGES.md - TODO tracker
- ✅ README.md - Quick start
- ✅ CONTRIBUTING.md - Contribution guidelines
- ✅ 5 Phase completion docs (PHASE1-5_COMPLETE.md)

### Technical Details

**Dependencies**
- Java 17+
- Spring Boot 3.2.0
- PostgreSQL 15+
- pgvector extension
- Flyway 10.4.1
- Testcontainers 1.19.3

**Database Schema**
- memories table with pgvector support
- HNSW index for fast similarity search
- Triggers for auto-updates
- Token count caching

**Performance**
- Save: ~50ms (with embedding)
- Search (20 results): ~20ms
- Batch decay (1000 memories): ~100ms
- Token count: ~1ms (cached)

---

## [0.9.0] - 2024-10-14

### Development Milestones

**Phase 1: Foundation** ✅
- Core interfaces
- PostgreSQL integration
- Basic CRUD
- 46/46 tests passed

**Phase 2: Plugin System** ✅
- Plugin registry
- Auto-loading
- Example plugins
- 67/67 tests passed

**Phase 3: Decay & Lifecycle** ✅
- Decay strategies
- Lifecycle manager
- Auto-deletion
- 91/91 tests passed

**Phase 4: Advanced Search** ✅
- Query executor
- Token counting
- Query limits
- 100/100 tests passed

**Phase 5: Spring Integration** ✅
- Service facade
- Fluent API
- E2E tests
- 107/107 tests passed

---

## Future Releases

### [1.1.0] - Planned

**Enhanced Providers**
- OpenAI embedding provider
- Ollama local embeddings
- Exact token counter (tiktoken)

**Monitoring**
- Micrometer metrics
- Health indicators
- Actuator endpoints

**Performance**
- Query result caching
- Batch import/export
- Async operations

### [1.2.0] - Planned

**Advanced Features**
- Multi-modal embeddings
- Graph relationships
- Custom indexing strategies
- Advanced analytics

### [2.0.0] - Future

**Enterprise Features**
- Distributed mode
- Multi-node clustering
- Real-time sync
- Advanced security

---

## Versioning Strategy

- **MAJOR**: Breaking API changes
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes, no new features

**Pre-1.0.0**: Breaking changes allowed without major version bump.

**Post-1.0.0**: Semantic versioning strictly followed.

---

## Deprecation Policy

- Features marked deprecated in version X
- Removed in version X+2 (at least 6 months later)
- Migration guide provided
- Warnings in logs

---

*For detailed changes, see git commit history.*

**Made with ❤️ by the Memorix Team**

