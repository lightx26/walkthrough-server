# API Design

## Response Wrapping

Every API response is wrapped in a standard envelope:

| Type | Use Case | Fields |
|------|----------|--------|
| `DataResponse<T>` | Success | `success`, `message`, `data` |
| `ErrorResponse` | Error | `success`, `message`, `errorCode`, `errors` (optional map) |

For collections, `T` is one of:

| Wrapper | Use Case | Fields |
|---------|----------|--------|
| `ListData<T>` | Simple list | `items` |
| `SliceData<T>` | Infinite scroll | `items`, `hasNext` |
| `PageData<T>` | Pagination | `items`, `page`, `size`, `totalElements`, `totalPages` |

Example: `DataResponse<ListData<WalkthroughSummaryResponse>>`

## URL Patterns

```
/v1/<resource>                          # collection
/v1/<resource>/{id}                     # single item
/v1/<resource>?param=value              # filtered collection
/v1/github/repos/{owner}/{repo}/pulls   # nested resource
```

- Version prefix: `/v1/`
- Path variables for IDs and resource hierarchy
- Query params for filters, pagination, search

## HTTP Status Codes

| Code | When |
|------|------|
| `200 OK` | Successful GET, PUT |
| `201 Created` | Successful POST |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation failure, invalid input |
| `401 Unauthorized` | Missing/invalid/expired token |
| `403 Forbidden` | Authenticated but not authorized |
| `404 Not Found` | Resource doesn't exist |
| `500 Internal Server Error` | Unexpected error |
| `502 Bad Gateway` | External API failure (GitHub) |

## Controller Formula

```java
@PostMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<DataResponse<SomeResponse>> create(
        @AuthenticationPrincipal AuthUser authUser,
        @Valid @RequestBody SomeRequest request) {
    UUID userId = UUID.fromString(authUser.getUserId());
    SomeEntity entity = someService.create(userId, request);      // 1. delegate
    SomeResponse response = someMapper.toResponse(entity);         // 2. map
    return ResponseEntity.status(HttpStatus.CREATED)               // 3. wrap
            .body(DataResponse.of(response));
}
```

## Key Files

- `interceptors/ApiResponse.java` — base response class
- `interceptors/DataResponse.java` — success wrapper
- `interceptors/ErrorResponse.java` — error wrapper
- `modules/_shared/dto/ListData.java`, `PageData.java`, `SliceData.java`
