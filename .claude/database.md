# Database & Migrations

## Flyway

- Location: `src/main/resources/db/migration/`
- Naming: `V<N>__<description>.sql` (e.g., `V1__create_users_table.sql`)
- Versions are **sequential and immutable** — never edit a committed migration
- Schema: `walkthrough` (set in `application-dev.properties`)
- `spring.jpa.hibernate.ddl-auto=none` — Flyway manages all schema changes

## SQL Conventions

```sql
CREATE TABLE some_table
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    content    TEXT,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_some_table_user_id ON some_table (user_id);
```

- Primary keys: `UUID` with `gen_random_uuid()`
- Timestamps: `TIMESTAMPTZ` (timezone-aware)
- Foreign keys: always specify `ON DELETE CASCADE` for child tables
- Indexes: on foreign key columns and frequently queried fields

## BaseEntity

All JPA entities extend `BaseEntity`:

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist  → sets both timestamps
    @PreUpdate   → updates updatedAt
}
```

## JPA Patterns

- Parent-child: `@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)`
- Ordering: `@OrderBy("sortOrder ASC")` on collection fields
- Lazy loading: `@ManyToOne(fetch = FetchType.LAZY)` for parent references
- Repository: extend `JpaRepository<SomeEntity, UUID>`, use Spring Data derived query methods

## Key Files

- `modules/_shared/entity/BaseEntity.java`
- `src/main/resources/db/migration/V*.sql`
- `src/main/resources/application-dev.properties`
