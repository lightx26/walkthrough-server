# Project Structure

## Overview

This is a Spring Boot application organized into **feature modules** under `modules/`. Each module is self-contained and follows a consistent layered architecture.

## Top-Level Layout

```
src/main/java/com/pet/walkthroughserver/
├── exceptionHandlers/          # Module-scoped exception handlers
├── modules/
│   ├── _shared/                # Shared code used across modules
│   │   ├── dto/                # Common response types (ApiResponse, DataResponse, ErrorResponse, etc.)
│   │   ├── entity/             # Base entity (BaseEntity with audit fields)
│   │   ├── exceptions/         # Abstract AppException base class
│   │   └── infra/              # Shared infrastructure clients
│   │       ├── cookie/         # Cookie management (CookieService)
│   │       ├── github/         # GitHub API clients + infra exceptions
│   │       └── jwt/            # JWT token service
│   ├── auth/                   # Authentication module
│   ├── github/                 # GitHub data module
│   └── user/                   # User module
├── config/                     # Spring configuration classes
└── security/                   # Spring Security filters, AuthUser, SecurityUtils
```

## Module Internal Structure

Every module follows the same three-layer layout:

```
modules/<module>/
├── presentation/               # HTTP layer (controllers, request/response DTOs, mappers)
│   ├── <Module>Controller.java
│   ├── dto/                    # Request and response DTOs for this module
│   └── mapper/                 # Mappers that convert between business models and presentation DTOs
├── business/                   # Business logic layer
│   ├── services/               # Service interfaces and implementations
│   └── models/                 # Business domain models (not entities, not DTOs)
├── exceptions/                 # Module-specific exception classes
└── repository/                 # Data access layer (JPA entities, Spring Data repositories)
```

## Naming Conventions

| Layer        | Class suffix examples                          |
|--------------|------------------------------------------------|
| Presentation | `*Controller`, `*Response`, `*Request`, `*PresentationMapper` |
| Business     | `*Service`, `*ServiceImpl`, business model records/classes     |
| Repository   | `*Entity`, `*Repository`                       |
| Exceptions   | Specific names like `UserNotFoundException`, `InvalidTokenException` |
