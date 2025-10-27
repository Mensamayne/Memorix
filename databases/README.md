# Memorix Databases

Lokalne bazy danych dla rozwoju i testowania.

## Struktura

```
databases/
├── memorix/             # Główna baza danych (default)
├── memorix_docs/        # Baza dla multi-datasource example
└── README.md            # Ten plik
```

**Note:** Inne aplikacje używające Memorix (np. Fitachio) mogą automatycznie tworzyć swoje bazy przez config:
```yaml
memorix:
  auto-create-database: true
```

## Uruchomienie

```bash
# W katalogu databases/
docker-compose up -d
```

Lub ręcznie:
```bash
# Utwórz bazy w PostgreSQL
createdb memorix
createdb memorix_docs
```

## Konfiguracja

Bazy są skonfigurowane w `application.yml`:
- `memorix` → `jdbc:postgresql://localhost:5432/memorix` (default)
- `memorix_docs` → `jdbc:postgresql://localhost:5432/memorix_docs` (multi-datasource example)

## Auto-Creation

Aplikacje używające Memorix mogą automatycznie tworzyć swoje bazy przez config:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/my_custom_db

memorix:
  auto-create-database: true  # Creates 'my_custom_db' automatically!
```
