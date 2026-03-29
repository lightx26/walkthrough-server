# Exception Handling

## Design Principles

1. **One exception class per error type** — no factory-method god-classes.
2. **All module exceptions extend `AppException`** — which carries `httpStatus`, `errorCode`, and `message`.
3. **Each module has its own exception handler** — scoped by `basePackageClasses` to its controller.
4. **`GlobalExceptionHandler` stays lean** — only handles framework exceptions and the unexpected catch-all.

## Exception Hierarchy

```
RuntimeException
└── AppException (abstract)                         # _shared/exceptions/
    ├── InvalidTokenException                       # auth/exceptions/
    ├── TokenExpiredException                       # auth/exceptions/
    ├── NotAuthenticatedException                   # auth/exceptions/
    ├── UserNotFoundException                       # user/exceptions/
    ├── GitHubAccessTokenNotFoundException          # github/exceptions/
    ├── GitHubAuthFailedException                   # _shared/infra/github/
    ├── GitHubApiException                          # _shared/infra/github/
    └── GitHubResourceNotFoundException             # _shared/infra/github/
```

## Creating a New Exception

Every exception follows the same pattern — extend `AppException`, set HTTP status and error code:

```java
package com.pet.walkthroughserver.modules.<module>.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class SomethingWentWrongException extends AppException {

    public SomethingWentWrongException(String message) {
        super(HttpStatus.BAD_REQUEST, "SOMETHING_WENT_WRONG", message);
    }
}
```

Then throw it directly:

```java
throw new SomethingWentWrongException("Detailed explanation here");
```

## Exception Handlers

### Module Handlers (`exceptionHandlers/`)

Each module handler is scoped to its controller's package using `@RestControllerAdvice(basePackageClasses = ...)`.

A module handler must handle:
- Its own module's exception types.
- Cross-cutting exceptions from dependencies it calls (e.g., `AuthExceptionHandler` also handles `UserNotFoundException` because auth calls `UserService`, and `GitHubAuthFailedException` because auth calls `GitHubAuthClient`).

All handlers use `AppException` fields to build a uniform response:

```java
private ResponseEntity<ErrorResponse> respond(AppException ex) {
    return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
}
```

### Global Handler

`GlobalExceptionHandler` only handles:
- `MethodArgumentNotValidException` — bean validation failures
- `MissingServletRequestParameterException` — missing query params
- `MethodArgumentTypeMismatchException` — wrong param types
- `Exception` — unexpected catch-all (returns 500)

It does NOT handle any `AppException` subclass — those are handled by module handlers.

## Adding a Handler for a New Module

```java
@Slf4j
@RestControllerAdvice(basePackageClasses = NewModuleController.class)
public class NewModuleExceptionHandler {

    @ExceptionHandler(NewModuleSpecificException.class)
    public ResponseEntity<ErrorResponse> handle(NewModuleSpecificException ex) {
        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return respond(ex);
    }

    private ResponseEntity<ErrorResponse> respond(AppException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }
}
```
