# plain-specification

A lightweight, type-safe implementation of the **Specification Pattern** for Java, decoupling query construction from storage backends.

## Features

- **Type-safe Expression DSL** — Build queries using lambda expressions with compile-time type checking
- **Visitor Pattern** — Compile expression trees to multiple targets (in-memory predicates, SQL QueryWrappers, Comparators)
- **Hybrid Execution** — Automatically push down queries to storage backends when possible, fallback to in-memory filtering
- **Multiple Storage Backends** — MyBatis-Plus (SQL) and Redis out of the box
- **Extensible** — Add new storage backends by implementing `ISpecificationExecutor`
- **Async Support** — CompletableFuture-based async operations with proper transaction management
- **Zero Boilerplate** — Fluent API for building complex queries

## Requirements

- Java 8+
- Spring Framework 5.3.x (for MyBatis-Plus and Redis modules)

## Modules

```
plain-specification/
├── plain-specification-core/          Core expression DSL, visitor pattern, hybrid execution
├── plain-specification-executor/
│   ├── mybatis-plus/                  MyBatis-Plus integration (SQL generation)
│   └── redis/                         Redis integration (ZSET + HASH storage)
├── plain-specification-spring-boot-starter/  Spring Boot auto-configuration
└── plain-specification-example/       Example applications and usage demos
```

## Quick Start

### Maven Dependency

```xml
<!-- Core module (required) -->
<dependency>
    <groupId>org.plain</groupId>
    <artifactId>plain-specification-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- MyBatis-Plus executor (optional) -->
<dependency>
    <groupId>org.plain</groupId>
    <artifactId>plain-specification-executor-mybatisplus</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- Redis executor (optional) -->
<dependency>
    <groupId>org.plain</groupId>
    <artifactId>plain-specification-executor-redis</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
// Define your entity
public class User {
    private String name;
    private Integer age;
    // getters and setters
}

// Build a specification
Specification<User> spec = new Specification<>();
spec.query()
    .where(Expressions.<User>create()
        .equal(User::getName, "Alice")
        .and()
        .greaterThan(User::getAge, 18));

// In-memory evaluation
Collection<User> users = getAllUsers();
Collection<User> result = spec.evaluate(users);

// Check single entity
boolean matches = spec.isSatisfiedBy(user);
```

### Expression DSL

The `Expressions` class provides a fluent API for building type-safe queries:

```java
// Comparison operators
Expressions.<User>create().equal(User::getName, "Alice")
Expressions.<User>create().notEqual(User::getStatus, "INACTIVE")
Expressions.<User>create().greaterThan(User::getAge, 18)
Expressions.<User>create().greaterThanOrEqual(User::getScore, 100)
Expressions.<User>create().lessThan(User::getAge, 65)
Expressions.<User>create().lessThanOrEqual(User::getLevel, 10)

// Collection operators
Expressions.<User>create().in(User::getRole, Arrays.asList("ADMIN", "MANAGER"))
Expressions.<User>create().notIn(User::getStatus, Arrays.asList("BANNED", "SUSPENDED"))
Expressions.<User>create().between(User::getAge, 18, 65)

// String operators
Expressions.<User>create().like(User::getName, "%Alice%")

// Null checks
Expressions.<User>create().isNull(User::getDeletedAt)
Expressions.<User>create().notNull(User::getEmail)

// Logical operators
Expressions.<User>create()
    .equal(User::getName, "Alice")
    .and()
    .greaterThan(User::getAge, 18)
    .or()
    .lessThan(User::getScore, 100)
    .not()
    .equal(User::getStatus, "BANNED")

// Nested expressions
Expressions.<User>create()
    .equal(User::getName, "Alice")
    .and(
        new OrExpression<>(
            new GreaterThanExpression<>(User::getAge, 18),
            new LessThanExpression<>(User::getScore, 100)
        )
    )
```

### MyBatis-Plus Integration

```java
// Simple repository (domain type = mapper type)
@Repository
public class UserRepository extends MybatisPlusBaseRepository<User, Long> {
    
    public UserRepository(BaseMapper<User> baseMapper, Executor executor) {
        super(baseMapper, executor);
    }
    
    // Or with transaction support for async operations
    public UserRepository(BaseMapper<User> baseMapper, Executor executor,
                         PlatformTransactionManager transactionManager) {
        super(baseMapper, executor, transactionManager);
    }
}

// Repository with domain/PO separation
@Repository
public class UserRepository extends MybatisPlusBaseRepositoryOfP<User, UserPO, Long> {
    
    public UserRepository(BaseMapper<UserPO> baseMapper,
                         IConverter<User, UserPO> converter,
                         Executor executor) {
        super(baseMapper, converter, executor);
    }
}

// Usage
Specification<User> spec = new Specification<>();
spec.query().where(Expressions.<User>create()
    .greaterThan(User::getAge, 18)
    .and()
    .like(User::getName, "%Alice%"));

// Synchronous query (generates SQL: WHERE age > 18 AND name LIKE '%Alice%')
List<User> users = userRepository.findRange(spec);

// Paginated query
PageQuery pageQuery = new PageQuery(1, 20);
IPageResult<User> page = userRepository.page(spec, pageQuery);

// Async query with transaction support
CompletableFuture<Collection<User>> future = userRepository.findRangeAsync(spec);
```

### Redis Integration

```java
// Repository implementation
@Repository
public class UserRedisRepository extends BaseRedisRepository<User, Long> {
    
    public UserRedisRepository(StringRedisTemplate redisTemplate, Executor executor) {
        super(redisTemplate, RedisRepositoryConfig.<User, Long>builder()
            .defaultZsetName("user")
            .idExtractor(User::getId)
            .asyncExecutor(executor)
            .fieldExtractor(DefaultStrategies.reflectionFieldExtractor())
            .scoreProvider(entity -> (double) System.currentTimeMillis())
            .build());
    }
}

// Usage
Specification<User> spec = new Specification<>();
spec.query().where(Expressions.<User>create()
    .equal(User::getStatus, "ACTIVE"));

// Query (full scan + in-memory filtering)
Collection<User> users = userRedisRepository.findRange(spec);

// Paginated query (scans all, filters, then paginates)
IPageResult<User> page = userRedisRepository.page(spec, pageQuery);

// Async batch operations
CompletableFuture<Collection<User>> saved = userRedisRepository.saveRangeAsync(users);
CompletableFuture<Long> deleted = userRedisRepository.deleteRangeAsync(spec);
```

**Redis Storage Model:**
- Each entity stored as HASH: `user:{id}` → field-value map
- ZSET for indexing: `user:index` → score=timestamp, member=id
- Lua scripts for atomic writes
- Pipeline for batch reads

### Sorting

```java
Specification<User> spec = new Specification<>();
spec.query()
    .where(Expressions.<User>create().greaterThan(User::getAge, 18))
    .orderBy(Expressions.<User>create().orderBy(User::getName), true)
    .thenBy(Expressions.<User>create().orderByDescending(User::getScore), true);
```

### Conditional Query Building

```java
Specification<User> spec = new Specification<>();

// Conditionally add where clauses
spec.query().where(Expressions.<User>create().equal(User::getName, name), 
                   name != null);

// Conditionally add ordering (chain discarded if condition is false)
spec.query()
    .orderBy(Expressions.<User>create().orderBy(User::getAge), sortByAge)
    .thenBy(Expressions.<User>create().orderBy(User::getName), true);  // skipped if orderBy was discarded
```

## Architecture

### Core Concepts

1. **Specification** — Entry point for building queries. Holds expression descriptors and delegates to evaluator.

2. **Expressions** — Fluent DSL for building type-safe expression trees. Supports 18 expression types (Equal, GreaterThan, Like, And, Or, Not, etc.).

3. **IExpression** — Expression tree node interface. Each expression implements `accept(IExpressionVisitor)` for the Visitor pattern.

4. **IExpressionVisitor** — Compiles expression trees to different targets:
   - `PredicateExpressionVisitor` → `Predicate<T>` for in-memory evaluation
   - `MybatisPlusExpressionVisitor` → MyBatis-Plus `QueryWrapper` for SQL generation
   - `OrderExpressionVisitor` → `Comparator<T>` for sorting

5. **ISpecificationEvaluator** — Executes queries:
   - `InMemorySpecificationEvaluator` — Filters collections using predicates
   - `HybridSpecificationEvaluator` — Pushes down to backend when possible, fallback to memory

6. **ISpecificationExecutor** — Storage backend interface. Implementations declare which expressions they support via `supportedExpressions()`.

### Hybrid Execution Strategy

The `HybridSpecificationEvaluator` automatically decides whether to push down queries:

```
1. Collect all expression types from Specification
2. Compare with executor.supportedExpressions()
3. If all supported → delegate to executor (SQL/Redis)
4. If any unsupported → fetch all from executor, filter in-memory
```

Example:
- MyBatis-Plus supports all 18 expression types → always pushes down
- Redis supports 0 expression types → always in-memory filtering
- Future: Elasticsearch might support Equal/Like/Between → partial pushdown

## Project Status

**Current Version**: 1.0-SNAPSHOT (pre-release)

**Completed**:
- ✅ 20 bug fixes (NPE, transaction, pagination, type safety, etc.)
- ✅ 67 unit tests (core module)
- ✅ Compilation passes on all modules

**In Progress**:
- ⬜ Documentation (README, CONTRIBUTING, API examples)
- ⬜ Maven Central release preparation
- ⬜ Redis expression pushdown (Equal/Between/GreaterThan)

**Roadmap**:
- Phase 1 (v0.1): Stability & quality baseline — **80% complete**
- Phase 2 (v0.5): Feature completeness — Redis pushdown, OrderEvaluator, more backends
- Phase 3 (v1.0): Ecosystem & maturity — Spring Boot Starter, benchmarks, observability

See [ROADMAP.md](ROADMAP.md) for details.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License.

## Author

Jayden.Liang
