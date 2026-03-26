# Walkthrough Server

Spring Boot backend for Walkthrough-PR — a structured code review tool for GitHub pull requests.

## Tech Stack

- **Java 21** with **Spring Boot 4.0.3**
- **PostgreSQL** with **Flyway** migrations
- **MapStruct** for DTO mapping, **Lombok** for boilerplate reduction
- **Spring Security** with JWT + httpOnly cookies
- **Gradle** build system

## Quick Commands

```bash
./gradlew bootRun        # Start dev server (port 8080)
./gradlew compileJava    # Compile only
./gradlew test           # Run tests
```

## Architecture Rules

- [Project Structure](.claude/project-structure.md) — module layout, naming conventions
- [Layer Responsibilities](.claude/layer-responsibilities.md) — presentation / business / repository duties
- [Context Separation](.claude/context-separation.md) — module boundaries, inter-module communication

## Patterns

- [Exception Handling](.claude/exception-handling.md) — AppException hierarchy, module-scoped handlers
- [Security](.claude/security.md) — JWT auth, AuthUser principal, endpoint protection
- [Code Style](.claude/code-style.md) — Lombok, MapStruct, validation, Jackson annotations
- [Database](.claude/database.md) — Flyway migrations, SQL conventions, BaseEntity, JPA patterns
- [API Design](.claude/api-design.md) — response wrapping, URL patterns, HTTP status codes
