# Walkthrough-Server Refactoring Plan

> Status: proposed. Goal: address scaling code smells (bloated classes, duplication,
> tight infra coupling, sub-optimal mapping, inconsistent exception handling) while
> keeping the codebase **readable, maintainable, adaptable, and consistent**.
>
> Companion analysis: this plan assumes the code-review findings on the analytics,
> walkthrough-version, comment-sync, profile, and GitHub modules.

## Guiding principles (the bar every change is held to)

| Goal | Concrete rule we'll enforce |
|---|---|
| **Readable** | No method > ~30 lines; no class > ~200 lines; no stringly-typed map access in business logic. |
| **Maintainable** | One source of truth per decision (token fetch, ownership, cache eviction, routing). |
| **Adaptable** | Business layer depends only on **ports** (interfaces in `business`/`_shared`). All vendor SDKs (RabbitMQ, Elasticsearch, RestClient) live behind adapters in `infra`. Swapping Rabbit→Kafka touches *only* `_shared/infra/messaging`. |
| **Consistent** | One way to do each thing, written down in `.claude`. MapStruct for 1:1 mapping; `*Assembler` for multi-source; `*Publisher`/`*Handler`/`*Provider` for ports; `*Amqp`/`*Kafka`/`*Es` for adapters. |

The work is split into **6 phases**, each independently shippable and compiling. They're
ordered so low-risk shared building blocks land first and the riskiest decomposition
lands on top of a safety net.

---

## Phase 0 — Safety net (do this first)

Refactors of `AnalyticsServiceImpl`, `WalkthroughVersionServiceImpl`, and
`CommentSyncConsumer` change subtle output. Lock current behavior before touching it.

- Add **characterization tests** (MockMvc / `@SpringBootTest` slice) for the highest-risk
  endpoints: `GET /analytics/.../review-progress`, `chapter-attention`, `unread-summary`,
  `repo-metrics`, `GET /walkthroughs/{id}/diff`, and the comment-sync consumer path.
- Capture real JSON responses as golden files so the assemblers in Phase 3 must reproduce
  them byte-for-byte.
- **Verification:** `./gradlew test` green before any production change.

---

## Phase 1 — Cross-cutting kernel (DRY, low risk, additive)

Eliminate the five copy-pasted patterns. These are mostly *new* classes plus mechanical
call-site edits.

**1.1 `GitHubTokenProvider`** — kills the token-fetch triplication
(`GitHubPrServiceImpl`, `GitHubRepoServiceImpl`, `GitHubCommentServiceImpl`).

```java
// _shared/infra/github/GitHubTokenProvider.java
@Component @RequiredArgsConstructor
public class GitHubTokenProvider {
    private final UserService userService;
    public String accessTokenFor(UUID userId) {
        String token = userService.findById(userId).getGithubAccessToken();
        if (token == null || token.isBlank())
            throw new GitHubAccessTokenNotFoundException("GitHub access token not found for user " + userId);
        return token;
    }
}
```

Inject it into the 3 services; delete their private
`getAccessTokenFromUser`/`getGitHubAccessToken`.

**1.2 Ownership guard** — replaces 6 hand-written `if (!x.getUserId().equals(userId)) throw …`.

```java
// _shared/security/OwnershipGuard.java
public final class OwnershipGuard {
    public static void require(UUID ownerId, UUID actorId, Supplier<? extends AppException> onDenied) {
        if (!ownerId.equals(actorId)) throw onDenied.get();
    }
}
```

Module exceptions stay module-specific (passed via supplier), preserving current behavior.

**1.3 `findOrThrow` helper** — standardizes the repeated `findById().orElseThrow(...)`.

```java
// _shared/repository/Repositories.java
public static <T> T orThrow(Optional<T> found, Supplier<? extends AppException> onMissing) {
    return found.orElseThrow(onMissing);
}
```

**1.4 Cache-eviction policy component** — removes the 6–8 line `@Caching(evict={…})` blocks
duplicated across `create/update/delete` (and fixes the drift bug where `create`'s list is
missing `WALKTHROUGH_DETAIL`/`WALKTHROUGH_COMMENT_COUNTS`).

```java
// walkthrough/business/cache/WalkthroughCacheEvictor.java
@Component @RequiredArgsConstructor
public class WalkthroughCacheEvictor {
    private final CacheManager cacheManager;
    public void onWrite(UUID userId, UUID walkthroughId) { /* single definition of the eviction set */ }
}
```

Service methods drop the annotation walls and call `evictor.onWrite(...)`. One source of
truth; testable.

**1.5 `SyncStatus` enum** — replaces `"pending"/"synced"/"failed"/"permanently_failed"`
strings in `CommentSyncConsumer`/`CommentRetryScheduler` (mirrors existing
`WalkthroughStatus`, `AnnotationStatus`). Add a Flyway migration only if the column needs a
CHECK; the stored values stay the same strings.

**1.6 Fix exception hierarchy** — make `SearchException` and `IndexingException` extend
`AppException` (set `503`/`500` + error code). Now they flow through the standard handler
and the search handler stops being a special case.

**Verification:** tests green; no behavior change; grep confirms the duplicated blocks are gone.

---

## Phase 2 — Hexagonal messaging (the "switch RabbitMQ → Kafka" requirement)

**Target:** business code never imports `org.springframework.amqp`. A transport swap = add
one adapter package + change config; zero business edits. We generalize the
*already-correct* `WalkthroughEventPublisher`/`...Amqp` pattern to **all** events and split
every consumer into **(infra listener adapter) → (business handler)**.

**2.1 Outbound port (business-facing, no infra deps)** — `_shared/messaging/`:

```java
public interface DomainEvent { UUID aggregateId(); Instant occurredAt(); String eventType(); }
public interface DomainEventPublisher { void publish(DomainEvent event); }
```

Make `WalkthroughEvent extends DomainEvent` (`aggregateId()` defaults to `walkthroughId()`);
add a `CommentCreatedEvent` that implements it. Replace the direct-`RabbitTemplate`
`CommentEventProducer` with the shared publisher.

**2.2 Single Rabbit adapter + routing registry** — `_shared/infra/messaging/rabbit/`:

```java
@Component @RequiredArgsConstructor
class RabbitDomainEventPublisher implements DomainEventPublisher {
    private final RabbitTemplate rabbit;
    private final EventRoutingRegistry routes;          // eventType -> (exchange, routingKey)
    private final EventMessageFactory messages;         // DomainEvent -> transport DTO
    public void publish(DomainEvent e) {
        var r = routes.routeFor(e.eventType());
        rabbit.convertAndSend(r.exchange(), r.routingKey(), messages.toMessage(e));
    }
}
```

This collapses `WalkthroughEventPublisherAmqp` + `CommentEventProducer` into one publisher.
The `switch`-on-event-type routing logic moves into a declarative `EventRoutingRegistry`
(one table).

**2.3 Inbound: split consumers into adapter + handler.** Today `CommentSyncConsumer`
(62-line method) and `ActivitySyncConsumer` live in `business/` with `@RabbitListener`.
Split each:

- **Business handler** (port + impl, no messaging imports): `comment/business/sync/CommentSyncHandler`,
  `profile/business/sync/ActivitySyncHandler`, search handler. These hold the logic.
- **Infra listener adapter** in `_shared/infra/messaging/rabbit/listeners/`: thin
  `@RabbitListener` that deserializes the transport message → domain command → calls the handler.

```java
@Component @RequiredArgsConstructor
class CommentSyncRabbitListener {
    private final CommentSyncHandler handler;
    @RabbitListener(queues = RabbitMQConfig.COMMENT_QUEUE)
    void onMessage(CommentEventMessage msg) { handler.handle(toCommand(msg)); }
}
```

**Result — to adopt Kafka later:** add `_shared/infra/messaging/kafka/` with
`KafkaDomainEventPublisher` (eventType→topic) + `@KafkaListener` adapters, toggle with a
`@ConditionalOnProperty(messaging.transport=kafka|rabbit)`. **Nothing in any `business/`
package changes.**

**Verification:** existing consumer tests pass against the new handler; an adapter unit test
asserts routing.

---

## Phase 3 — Restore the layer boundary (business stops building Response DTOs)

The biggest architectural fix. **Rule:** services return entities or `business/models/`
records; presentation maps them. MapStruct for 1:1; `@Component *Assembler` for
multi-source aggregation (documented convention).

**3.1 Analytics** (`AnalyticsServiceImpl`, 394 LOC, imports 11 response DTOs):

- Introduce typed read-models in `analytics/business/models/` (records: `ReviewProgress`,
  `ChapterAttention`, `UnreadSummary`, `RepoMetrics`, `AuthorWalkthroughSummary`).
- Replace stringly-typed `Tuple.get("col")` access with **JPA projections** (interface or
  record projections in `AnalyticsQueryRepository`) → kills the `toInt/toBool/toInstant` casts.
- Add `AnalyticsAssembler` (presentation) mapping read-model → `*Response`.
- Decompose the god-service into cohesive collaborators behind the existing
  `AnalyticsService` facade: `ReviewProgressCalculator`, `ChapterAttentionCalculator`,
  `RepoMetricsCalculator`. Each < 80 lines.
- Fix the N+1 in `getAuthorSummary` (batch reviewer rows for all walkthrough ids in one query).

**3.2 Versioning** (`WalkthroughVersionServiceImpl`, 421 LOC):

- Introduce a **typed snapshot model** — deserialize `WalkthroughSnapshotEntity.walkthroughContent`
  into `SnapshotContent` records instead of `Map<String,Object>`. This deletes all 5
  `@SuppressWarnings("unchecked")` and the `getStringField/getIntField/getListField` helpers.
- Split into three focused services: `StalenessChecker`, `WalkthroughVersionCreator`,
  `WalkthroughDiffService`; move `VersionDiffResponse` assembly into a `VersionDiffAssembler`.
- Move the duplicated `captureSnapshot` into a shared `WalkthroughSnapshotService` (also used
  by `WalkthroughServiceImpl`).

**3.3 Profile / Pin / Activity:** move `ProfileResponse`/`PinnedWalkthroughResponse`/`ActivityEntryResponse`
construction into assemblers; services return models/entities. Fix the **N+2 in
`ProfileServiceImpl.getReviewing`** (batch-load walkthroughs + creators by id instead of
per-row `findById`).

**Verification:** Phase 0 golden-file tests must still pass exactly. `grep "presentation.dto"`
in `business/` returns nothing.

---

## Phase 4 — Consistency cleanups

**4.1 Consolidate exception handlers (9 → 2).** `AppException` already carries `httpStatus`
+ `errorCode`, so per-module scoping isn't needed for the response. Replace the 8 module
handlers (with their duplicated cross-module catches of `UserNotFoundException` ×3,
`WalkthroughNotFoundException` ×2, etc.) with **one** `@RestControllerAdvice` handling
`AppException` uniformly, keeping `GlobalExceptionHandler` for framework exceptions. Fold
`utils/ExceptionResponse` into it (rename away the confusing "Response that isn't a response").

**Does one handler for many modules get large? No — and that's the point.** A handler's
size is driven by the **number of `@ExceptionHandler` methods**, not by how many modules or
requests route to it. The consolidated handler dispatches on the polymorphic base type, so
it is **one method, fixed-size forever**:

```java
@Slf4j
@RestControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(AppException.class)   // catches EVERY subclass polymorphically
    public ResponseEntity<ErrorResponse> handle(AppException ex, HttpServletRequest req) {
        log.warn("{} at {}: {}", ex.getClass().getSimpleName(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }
}
```

A new exception in a new module is handled automatically (it extends `AppException`, which
already carries status + code + message) — **zero edits here**. The large, *growing*
artifact is the current 9-handler setup, which enumerates exceptions one method at a time
and duplicates the shared ones.

**Before/after (approx.):** 9 advices × ~30–50 lines each (≈ 300 lines, growing per module)
→ `AppExceptionHandler` ~12 lines (fixed) + `GlobalExceptionHandler` for framework
exceptions (unchanged). This collapse is only safe *because* every current per-module
handler does the identical `log.warn(...)` + `respond(ex)` — there is no per-type logic to lose.

**Escape hatch (graceful, pay-as-you-go).** If a *specific* exception ever needs special
treatment (different log level, masked message, extra header), add **one** targeted method
to the same advice; Spring picks the most-specific match, so it coexists with the catch-all:

```java
    @ExceptionHandler(GitHubAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handle(GitHubAuthFailedException ex) {
        log.error("GitHub auth failed: {}", ex.getMessage());   // error, not warn
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode(), "Re-authentication required"));
    }
```

You only pay complexity where a real exception to the rule exists — instead of paying it 9×
upfront for uniformity you don't have.

> *Trade-off:* you lose the *option* of per-module log wording for the same exception type
> (in practice unused today — all handlers just `log.warn` the message). If true per-module
> context is wanted later, the better tool is an MDC/filter tagging every log line with the
> request path — not 9 hand-maintained advices. **Decision point:** collapse to one advice
> (recommended) vs. keep per-module handlers extracting a shared base advice.

**4.2 Kill the silent swallow** in `WalkthroughController.listRecentlyReviewed`
(`catch(Exception) → return null`). Replace with a batch service method
`listRecentlyReviewed(userId)` that fetches accessible walkthroughs by id in one query and
assembles in the service — no per-item try/catch.

**4.3 Merge the duplicate diff parsers.** `DiffPositionParser` (comment) and `DiffLineMapper`
(walkthrough) both hand-parse unified-diff hunk headers with silent `NumberFormatException`
swallowing. Merge into one `_shared`/util `UnifiedDiff` with a single, tested hunk parser.

**4.4 Manual pagination → Spring Data `Slice`** in `ActivityServiceImpl` and
`ReadProgressServiceImpl`.

**4.5 Controller post-mapping setters** (`setCommentCount`, `setReadChapterIds`,
`setWalkthroughsCount`) → move enrichment into the assembler so mapping is complete in one place.

---

## Phase 5 — Documentation (make `.claude` describe reality + the new conventions)

- **`layer-responsibilities.md` / `context-separation.md`** — keep the "no presentation DTOs
  in business" rule (now true) and add the **mapper-vs-assembler** convention and **when
  `business/models/` is mandatory**.
- **`exception-handling.md`** — document the single-advice model and the `AppException`-only
  rule (incl. search now compliant).
- **New `messaging.md`** — document the port/adapter model and the exact steps to add a Kafka
  transport (the adaptability contract).
- **New `caching.md`** — document the eviction-policy component and cache key conventions.
- **`code-style.md`** — record the real mapping policy; **`project-structure.md`** — add
  `configs/`, `interceptors/`, `security/`, and module `infra/` sub-packages.

---

## Target package shape (after refactor)

```
_shared/
  messaging/                 # ports: DomainEvent, DomainEventPublisher (no infra deps)
  infra/messaging/
    rabbit/                  # RabbitDomainEventPublisher, listeners, routing registry
    (kafka/)                 # drop-in later — only place that changes for a transport swap
  infra/github/              # GitHubClientImpl, GitHubTokenProvider
  security/OwnershipGuard    # shared domain guards
<module>/business/
  models/                    # domain read-models / value records  (now actually used)
  services/                  # logic only — returns models/entities
  sync/                      # inbound event handlers (ports, no @RabbitListener)
<module>/presentation/
  mapper/                    # MapStruct 1:1
  assembler/                 # multi-source → *Response
```

---

## Sequencing, risk & rollback

| Phase | Risk | Why safe | Rollback unit |
|---|---|---|---|
| 0 Tests | none | additive | n/a |
| 1 Kernel | low | additive + mechanical call-site swaps | per-class |
| 2 Messaging | medium | behavior-preserving; ports mirror existing good pattern | per-consumer |
| 3 Layering | **high** | guarded by Phase 0 golden tests | per-module |
| 4 Consistency | medium | small, isolated | per-item |
| 5 Docs | none | docs only | n/a |

Each phase is a separate PR (Phase 3 ideally one PR per module). Everything stays green
between phases.

---

## Acceptance criteria

- `grep -r "presentation.dto" business/` → empty.
- `grep -r "org.springframework.amqp" --include=*.java` → only under `_shared/infra/messaging/`.
- No `@SuppressWarnings("unchecked")` in `walkthrough/business`.
- No method > 30 lines in analytics/version/comment-sync; no `catch (Exception) { return null }`.
- Token fetch, ownership check, and walkthrough cache-eviction each defined exactly once.
- A written `messaging.md` proving the Kafka-swap path; golden-file API tests unchanged.

---
