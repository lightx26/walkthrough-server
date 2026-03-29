# Context Separation

## Module Boundaries

Each module (`auth`, `user`, `github`) is a self-contained vertical slice. Modules should:

- **Own their data** — entities, repositories, and business logic stay inside the module.
- **Expose only interfaces** — other modules depend on service interfaces, not implementations.
- **Define their own exceptions** — never throw another module's exception; let it bubble naturally.

## How Modules Communicate

Modules call each other through **business service interfaces** — never through controllers, repositories, or direct entity access.

```
auth ──depends on──> user (via UserService interface)
github ──depends on──> user (via UserService interface)
```

- `AuthServiceImpl` calls `UserService.findById()` and `UserService.findOrCreateByGitHub()`.
- `GitHubServiceImpl` calls `UserService.findById()` to resolve the current user's GitHub access token.

## Shared Infrastructure (`_shared/infra/`)

Infrastructure clients sit outside modules and are injected where needed:

| Client              | Used by        | Purpose                                   |
|---------------------|----------------|-------------------------------------------|
| `GitHubAuthClient`  | auth           | OAuth token exchange, fetch user info      |
| `GitHubResourceClient` | github      | Fetch repos, PRs, search                  |
| `TokenService`      | auth, security | Generate/validate JWTs                     |
| `CookieService`     | auth           | Set/clear/extract auth cookies             |

Both `GitHubAuthClient` and `GitHubResourceClient` are interfaces implemented by a single `GitHubClientImpl` — the split is for interface segregation (auth callers don't see resource methods and vice versa).

## Exception Scoping

Exception handlers are scoped by `basePackageClasses` to ensure each handler only catches exceptions from its own controller's request lifecycle:

```java
@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler { ... }
```

This means:
- If `AuthController` triggers a `UserNotFoundException` (via `UserService`), `AuthExceptionHandler` catches it.
- If `UserController` triggers a `UserNotFoundException`, `UserExceptionHandler` catches it.
- Same exception type, different handlers — each can log context-appropriate messages.

## DTOs Per Layer

Each layer has its own data objects to prevent coupling:

```
Client  <──  Presentation DTOs  <──  Business Models  <──  Repository Entities
         (UserResponse)          (GitHubUserData)        (UserEntity)
```

- **Presentation DTOs** — shaped for the API consumer (camelCase JSON, only relevant fields).
- **Business models** — shaped for business logic (records, value objects).
- **Entities** — shaped for the database (JPA annotations, relationships).

Presentation mappers (`*PresentationMapper`) convert entities/models to response DTOs. This keeps each layer independent — changing the DB schema doesn't break the API contract, and vice versa.

## Rules of Thumb

1. A controller should never import from `repository/` of another module.
2. A service should never return a presentation DTO — return entities or business models.
3. Infrastructure exceptions (e.g., `GitHubApiException`) are thrown in the infra layer and bubble up through the business layer to the handler — services do NOT need to catch and re-wrap them unless adding business context.
4. When adding a new module, follow the same `presentation/business/exceptions/repository` structure — consistency over cleverness.
