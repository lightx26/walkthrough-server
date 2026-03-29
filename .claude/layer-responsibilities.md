# Layer Responsibilities

## Presentation Layer (`presentation/`)

**Purpose:** Accept HTTP requests and return HTTP responses. Nothing more.

- Controllers receive requests, delegate to business services, and return responses.
- Request/response DTOs live here — they define the API contract with the client.
- Presentation mappers convert between business models/entities and response DTOs.
- Controllers should NOT contain business logic — only input extraction, delegation, and response wrapping.
- Controllers wrap results in `DataResponse<T>` for success or throw exceptions that handlers catch.

```java
// Good: thin controller
@GetMapping("/me")
public ResponseEntity<DataResponse<UserResponse>> me(@AuthenticationPrincipal AuthUser authUser) {
    UserEntity user = authService.me(UUID.fromString(authUser.getUserId()));
    return ResponseEntity.ok(DataResponse.of(userPresentationMapper.toResponse(user)));
}
```

## Business Layer (`business/`)

**Purpose:** Encapsulate all business rules and orchestration.

- Services contain the core logic: validation, orchestration, domain rules.
- Business models (`business/models/`) represent domain concepts — they are NOT JPA entities and NOT API DTOs.
- Services depend on repository interfaces and shared infrastructure clients (e.g., `GitHubAuthClient`).
- Services throw **module-specific exceptions** (e.g., `InvalidTokenException`) — never generic exceptions.

## Repository Layer (`repository/`)

**Purpose:** Data access and persistence.

- JPA entities live here — annotated with `@Entity`, `@Table`, etc.
- Spring Data repositories (interfaces extending `JpaRepository`) live here.
- Entities should NOT leak into the presentation layer directly when possible; use mappers.

## Shared Module (`_shared/`)

**Purpose:** Code that serves multiple modules.

- `dto/` — Common response wrappers: `DataResponse<T>`, `ErrorResponse`, `ListData<T>`, `SliceData<T>`, `PageData<T>`.
- `exceptions/` — The abstract `AppException` base class that all module exceptions extend.
- `infra/` — Infrastructure clients shared across modules (GitHub API clients, JWT service, cookie service).
- Infra clients define their own exceptions (e.g., `GitHubApiException`, `GitHubAuthFailedException`).

## Response Types

| Type              | Use case                               |
|-------------------|----------------------------------------|
| `DataResponse<T>` | Success — wraps any data type as `T`   |
| `ErrorResponse`   | Error — carries `errorCode`, `message`, optional `errors` map |
| `ListData<T>`     | Simple list of items                   |
| `SliceData<T>`    | Infinite scroll (items + `hasNext`)    |
| `PageData<T>`     | Pagination (items + page/size/total)   |

Usage: `DataResponse<PageData<UserResponse>>` for a paginated user list.
