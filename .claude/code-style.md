# Code Style

## Lombok Annotations by Class Type

### Entities
```java
@Entity
@Table(name = "table_name")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class SomeEntity extends BaseEntity { ... }
```

### Response DTOs (immutable)
```java
@Getter @Builder
public class SomeResponse {
    private UUID id;
    private String name;
}
```

### Request DTOs (mutable, for Jackson deserialization)
```java
@Getter @Setter
public class SomeRequest {
    @NotBlank private String name;
    @NotNull @Min(1) private Integer count;
}
```

### Services
```java
@Service
@RequiredArgsConstructor  // constructor injection via Lombok
public class SomeServiceImpl implements SomeService { ... }
```

### Exception Handlers
```java
@Slf4j
@Order(1)
@RestControllerAdvice(basePackageClasses = SomeController.class)
public class SomeExceptionHandler { ... }
```

### Business Models (Java records)
```java
@Builder
public record GitHubUserData(Long githubId, String username, ...) { }
```

## MapStruct Mappers

- Always declare as interface with `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)`
- Use `@Mapping` for field name mismatches
- Use `default` methods for custom conversion logic
- Spring injects the generated implementation automatically

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SomePresentationMapper {
    SomeResponse toResponse(SomeEntity entity);
    List<SomeResponse> toResponseList(List<SomeEntity> entities);
}
```

## Validation

- Use Jakarta validation annotations on request DTO fields
- Common: `@NotBlank`, `@NotNull`, `@Min`, `@Size`
- Validation errors handled by `GlobalExceptionHandler.handleValidation()`

## Jackson

- `@JsonInclude(JsonInclude.Include.NON_NULL)` on base response classes
- `@JsonProperty("fieldName")` for JSON name mapping (e.g., `isPrivate`)
- `@JsonAlias("snake_case")` on infra DTOs for GitHub API responses
