# Security

## Authentication Flow

The app uses **JWT + httpOnly cookie** authentication with GitHub OAuth.

```
1. User clicks "Sign in with GitHub" → redirects to GitHub OAuth
2. GitHub redirects back with code → client sends to POST /v1/auth/github
3. Server exchanges code for GitHub access token, fetches user info, upserts user
4. Server generates JWT access token (30 min) + refresh token (30 days)
5. Both tokens set as httpOnly cookies (secure in prod, non-secure in dev)
6. Client stores user info in Redux (no localStorage, no token exposure)
```

## Token Extraction

`JwtAuthenticationFilter` extracts the access token in priority order:

1. `access_token` httpOnly cookie (primary — used by the browser client)
2. `Authorization: Bearer <token>` header (fallback — for API clients)

If valid, it builds an `AuthUser` principal and sets it in `SecurityContextHolder`.

## AuthUser Principal

```java
@Getter
@Builder
public class AuthUser {
    private String userId;      // UUID as string
    private String username;
    private String displayName;
    private String avatarUrl;
}
```

Injected in controllers via `@AuthenticationPrincipal AuthUser authUser`.

## Securing Endpoints

```java
// Require authentication (most endpoints)
@PreAuthorize("isAuthenticated()")
public ResponseEntity<...> myEndpoint(@AuthenticationPrincipal AuthUser authUser) { ... }

// Public endpoints (login, refresh, health)
@PermitAll
public ResponseEntity<...> publicEndpoint() { ... }
```

Method-level security is enabled: `@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)`.

## Infrastructure Services

| Service | Location | Purpose |
|---------|----------|---------|
| `TokenService` | `_shared/infra/jwt/` | Generate/validate JWTs, extract claims |
| `CookieService` | `_shared/infra/cookie/` | Set/clear/extract auth cookies from requests |

## Key Files

- `security/JwtAuthenticationFilter.java` — servlet filter
- `security/AuthUser.java` — principal class
- `configs/SecurityConfig.java` — Spring Security configuration
- `modules/_shared/infra/jwt/TokenService.java` — JWT interface
- `modules/_shared/infra/cookie/CookieService.java` — cookie interface
