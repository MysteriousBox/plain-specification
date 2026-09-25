# Plain Specification 使用指南

## 目录

- [快速开始](#快速开始)
- [核心概念](#核心概念)
- [快捷 API](#快捷-api)
- [Repository 集成](#repository-集成)
- [内存评估](#内存评估)
- [异步操作](#异步操作)
- [高级用法](#高级用法)
- [存储后端](#存储后端)

---

## 快速开始

### 1. 添加依赖

```xml
<!-- 核心模块（必需） -->
<dependency>
    <groupId>org.plain</groupId>
    <artifactId>plain-specification-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- MyBatis-Plus 支持（可选） -->
<dependency>
    <groupId>org.plain</groupId>
    <artifactId>plain-specification-executor-mybatisplus</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- Redis 支持（可选） -->
<dependency>
    <groupId>org.plain</groupId>
    <artifactId>plain-specification-executor-redis</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 定义实体

```java
@Data
public class User {
    private Long id;
    private String name;
    private Integer age;
    private String status;
    private LocalDateTime createdAt;
}
```

### 3. 构建查询

```java
// 基础查询
ISpecification<User> spec = Specification.where(User::getAge).gte(18)
    .and(User::getStatus).eq("ACTIVE")
    .build();

// 带排序
ISpecification<User> spec = Specification.where(User::getAge).gte(18)
    .orderBy(User::getAge)
    .thenByDescending(User::getName)
    .build();
```

---

## 核心概念

### Specification（规格）

`ISpecification<T>` 是查询的核心抽象，表示一组可组合的查询条件：

```java
public interface ISpecification<T> {
    // 内存过滤
    Collection<T> evaluate(Collection<T> entities);
    
    // 单实体判断
    Boolean isSatisfiedBy(T entity);
    
    // 获取表达式（供后端编译）
    Iterable<IExpressionDescriptor<T>> getWhereExpressions();
    Iterable<OrderExpressionInfo<T>> getOrderExpressions();
}
```

### Expression（表达式）

表达式是查询条件的原子单元：

| 类型 | 说明 | 示例 |
|------|------|------|
| `EqualExpression` | 等于 | `eq("Alice")` |
| `NotEqualExpression` | 不等于 | `neq("DELETED")` |
| `GreaterThanExpression` | 大于 | `gt(18)` |
| `GreaterThanOrEqualExpression` | 大于等于 | `gte(18)` |
| `LessThanExpression` | 小于 | `lt(60)` |
| `LessThanOrEqualExpression` | 小于等于 | `lte(60)` |
| `BetweenExpression` | 区间 | `between(18, 60)` |
| `InExpression` | 包含 | `in("A", "B", "C")` |
| `NotInExpression` | 不包含 | `notIn("X", "Y")` |
| `LikeExpression` | 模糊匹配 | `like("%test%")` |
| `IsNullExpression` | 为空 | `isNull()` |
| `IsNotNullExpression` | 非空 | `isNotNull()` |
| `AndExpression` | 逻辑与 | 自动组合 |
| `OrExpression` | 逻辑或 | `.or(field).eq(value)` |
| `NotExpression` | 逻辑非 | 内部使用 |
| `OrderExpression` | 排序 | `orderBy(field)` |

### Visitor（访问者）

`IExpressionVisitor<T, R>` 将表达式编译为目标格式：

| Visitor | 目标 | 用途 |
|---------|------|------|
| `PredicateExpressionVisitor` | `Predicate<T>` | 内存过滤 |
| `OrderExpressionVisitor` | `Comparator<T>` | 内存排序 |
| `MybatisPlusExpressionVisitor` | `QueryWrapper<T>` | MyBatis-Plus SQL |
| `MybatisPlusEntityToPoVisitor` | `QueryWrapper<P>` | Domain/PO 分离 |

---

## 快捷 API

### 基础用法

```java
// 单条件
ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();

// 多条件 AND
ISpecification<User> spec = Specification.where(User::getAge).gte(18)
    .and(User::getStatus).eq("ACTIVE")
    .and(User::getName).like("%test%")
    .build();

// OR 条件
ISpecification<User> spec = Specification.where(User::getStatus).eq("VIP")
    .or(User::getAge).gte(60)
    .build();
// 等价于: status = 'VIP' OR age >= 60
```

### 比较操作

```java
// 等于 / 不等于
Specification.where(User::getName).eq("Alice")
Specification.where(User::getStatus).neq("DELETED")

// 大于 / 小于
Specification.where(User::getAge).gt(18)      // > 18
Specification.where(User::getAge).gte(18)     // >= 18
Specification.where(User::getAge).lt(60)      // < 60
Specification.where(User::getAge).lte(60)     // <= 60

// 区间
Specification.where(User::getAge).between(18, 60)

// 包含
Specification.where(User::getStatus).in("ACTIVE", "VIP", "ADMIN")
Specification.where(User::getRole).notIn("GUEST", "BANNED")

// 模糊匹配（仅 String 字段）
Specification.where(User::getName).like("%Alice%")
Specification.where(User::getEmail).like("test@%")

// 空值判断
Specification.where(User::getDeletedAt).isNull()
Specification.where(User::getEmail).isNotNull()
```

### 排序

```java
// 单字段升序
Specification.where(User::getAge).gt(0)
    .orderBy(User::getAge)
    .build();

// 单字段降序
Specification.where(User::getAge).gt(0)
    .orderByDescending(User::getAge)
    .build();

// 多字段排序
Specification.where(User::getStatus).eq("ACTIVE")
    .orderBy(User::getAge)              // 先按年龄升序
    .thenByDescending(User::getName)    // 再按名字降序
    .build();
```

### 复杂条件组合

```java
// (age >= 18 AND status = 'ACTIVE') OR (age >= 60)
ISpecification<User> spec = Specification.where(User::getAge).gte(18)
    .and(User::getStatus).eq("ACTIVE")
    .or(User::getAge).gte(60)
    .build();

// 注意：OR 会开启新的条件组
// 上面的 SQL 等价于：
// WHERE (age >= 18 AND status = 'ACTIVE') OR age >= 60
```

---

## Repository 集成

### MyBatis-Plus 直接映射

适用于实体类与数据库表直接对应的场景：

```java
// 1. 定义 Mapper
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

// 2. 定义 Repository
@Repository
public class UserRepository extends MybatisPlusBaseRepository<User, Long> {
    
    public UserRepository(UserMapper mapper, Executor executor) {
        super(mapper, executor);
    }
    
    // 可添加自定义查询方法
    public User findByEmail(String email) {
        return findOne(Specification.where(User::getEmail).eq(email).build());
    }
}

// 3. 使用
@Autowired
private UserRepository userRepo;

// 查询单个
User user = userRepo.findOne(
    Specification.where(User::getName).eq("Alice").build()
);

// 查询列表
List<User> users = new ArrayList<>(userRepo.findRange(
    Specification.where(User::getAge).gte(18).build()
));

// 分页查询
IPageResult<User> page = userRepo.page(
    Specification.where(User::getStatus).eq("ACTIVE")
        .orderByDescending(User::getCreatedAt)
        .build(),
    new PageQuery(1, 20)  // 第1页，每页20条
);

// 统计数量
long count = userRepo.count(
    Specification.where(User::getStatus).eq("ACTIVE").build()
);
```

### MyBatis-Plus Domain/PO 分离

适用于领域模型与持久化对象分离的场景：

```java
// 1. 定义 PO（持久化对象）
@Data
@TableName("users")
public class UserPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String user_name;  // 数据库字段名
    private Integer user_age;
    private String user_status;
}

// 2. 定义 Converter
public class UserConverter implements IConverter<User, UserPO> {
    
    @Override
    public UserPO convert(User domain) {
        UserPO po = new UserPO();
        po.setId(domain.getId());
        po.setUser_name(domain.getName());
        po.setUser_age(domain.getAge());
        po.setUser_status(domain.getStatus());
        return po;
    }
    
    @Override
    public User reverse(UserPO po) {
        User domain = new User();
        domain.setId(po.getId());
        domain.setName(po.getUser_name());
        domain.setAge(po.getUser_age());
        domain.setStatus(po.getUser_status());
        return domain;
    }
}

// 3. 定义 Repository
@Repository
public class UserRepository extends MybatisPlusBaseRepositoryOfP<User, UserPO, Long> {
    
    public UserRepository(UserMapper mapper, UserConverter converter, Executor executor) {
        super(mapper, converter, executor);
    }
}

// 4. 使用（与直接映射相同）
List<User> users = new ArrayList<>(userRepo.findRange(
    Specification.where(User::getAge).gte(18).build()  // 使用领域模型字段
));
```

### Redis 仓储

适用于缓存场景：

```java
// 1. 定义配置
RedisRepositoryConfig<User, Long> config = RedisRepositoryConfig.<User, Long>builder()
    .resolvedZSetKey("users:index")
    .resolvedHashKeyPrefix("user:")
    .fieldExtractor(entity -> {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", entity.getName());
        fields.put("age", entity.getAge());
        fields.put("status", entity.getStatus());
        return fields;
    })
    .entityBuilder(fields -> {
        User user = new User();
        user.setName((String) fields.get("name"));
        user.setAge((Integer) fields.get("age"));
        user.setStatus((String) fields.get("status"));
        return user;
    })
    .idExtractor(User::getId)
    .scoreProvider(user -> (double) user.getCreatedAt().toEpochMilli())
    .batchSize(100)
    .defaultTtl(3600)
    .build();

// 2. 定义 Repository
@Repository
public class UserRedisRepository extends BaseRedisRepository<User, Long> {
    
    public UserRedisRepository(StringRedisTemplate redisTemplate, 
                               RedisRepositoryConfig<User, Long> config) {
        super(redisTemplate, config);
    }
}

// 3. 使用
userRepo.save(user);
User found = userRepo.findById(1L);
Collection<User> activeUsers = userRepo.findRange(
    Specification.where(User::getStatus).eq("ACTIVE").build()
);
```

---

## 内存评估

### 过滤集合

```java
List<User> allUsers = getAllUsers();

ISpecification<User> spec = Specification.where(User::getAge).gte(18)
    .and(User::getStatus).eq("ACTIVE")
    .build();

// 过滤
Collection<User> filtered = spec.evaluate(allUsers);

// 判断单个
boolean match = spec.isSatisfiedBy(user);
```

### 排序支持

内存评估同时支持 WHERE 过滤和 ORDER BY 排序：

```java
List<User> users = getAllUsers();

ISpecification<User> spec = Specification.where(User::getAge).gte(18)
    .orderBy(User::getAge)
    .thenByDescending(User::getName)
    .build();

// 返回的结果已经按 age 升序、name 降序排列
Collection<User> sorted = spec.evaluate(users);
```

### 便捷方法

```java
// 查询所有
Collection<User> all = repo.findAll();
CompletableFuture<Collection<User>> allAsync = repo.findAllAsync();

// 判断存在
boolean exists = repo.exists(Specification.where(User::getEmail).eq("test@example.com").build());
CompletableFuture<Boolean> existsAsync = repo.existsAsync(spec);

// Optional 包装
Optional<User> user = repo.findOptional(Specification.where(User::getId).eq(1L).build());
CompletableFuture<Optional<User>> userAsync = repo.findOptionalAsync(spec);
```

---

## 异步操作

### 默认异步方法

所有 Repository 接口都提供异步版本：

```java
// 同步
User user = repo.findById(1L);

// 异步
CompletableFuture<User> future = repo.findByIdAsync(1L);
future.thenAccept(u -> System.out.println(u.getName()));
```

### 自定义线程池

```java
@Repository
public class UserRepository extends MybatisPlusBaseRepository<User, Long> {
    
    private static final Executor CUSTOM_EXECUTOR = Executors.newFixedThreadPool(10);
    
    public UserRepository(UserMapper mapper) {
        super(mapper, CUSTOM_EXECUTOR);
    }
    
    @Override
    public Executor getAsyncExecutor() {
        return CUSTOM_EXECUTOR;
    }
}
```

### 批量异步操作

```java
// 批量保存
List<User> newUsers = getNewUsers();
CompletableFuture<Collection<User>> saved = repo.saveRangeAsync(newUsers);

// 批量更新
CompletableFuture<Void> updated = repo.updateRangeAsync(users);

// 批量删除
CompletableFuture<Void> deleted = repo.deleteRangeAsync(users);

// 按条件删除
CompletableFuture<Void> deleted = repo.deleteRangeAsync(
    Specification.where(User::getStatus).eq("DELETED").build()
);
```

### 事务支持

MyBatis-Plus Repository 支持通过 `TransactionTemplate` 管理事务：

```java
@Repository
public class UserRepository extends MybatisPlusBaseRepository<User, Long> {
    
    public UserRepository(UserMapper mapper, Executor executor, 
                          PlatformTransactionManager txManager) {
        super(mapper, executor, txManager);
    }
}

// 批量操作自动在事务中执行
repo.saveRangeAsync(users);  // 事务保护
repo.updateRangeAsync(users); // 事务保护
repo.deleteRangeAsync(users); // 事务保护
```

---

## 高级用法

### 传统 Builder API

除了快捷 API，也可以使用传统的 `Expressions` 构建：

```java
Specification<User> spec = new Specification<>();
spec.query().where(
    Expressions.<User>create()
        .greaterThan(User::getAge, 18)
        .and(new EqualExpression<>(User::getStatus, "ACTIVE"))
);
spec.query().orderBy(
    Expressions.<User>create().orderBy(User::getAge)
);
```

### 混合执行策略

`HybridSpecificationEvaluator` 优先将查询下推到后端，不支持时自动降级：

```java
// Redis 后端（不支持表达式下推）
HybridSpecificationEvaluator<User> evaluator = 
    new HybridSpecificationEvaluator<>(redisExecutor);

// 自动降级为内存过滤
Collection<User> result = evaluator.evaluate(allUsers, spec);
```

### 自定义 Visitor

```java
public class ElasticsearchVisitor extends AbstractExpressionVisitor<User, QueryBuilder> {
    
    @Override
    public <V extends Comparable<V>> QueryBuilder visitEqual(EqualExpression<User, V> expr) {
        return QueryBuilders.termQuery(getFieldName(expr.getLeft()), expr.getRight());
    }
    
    @Override
    public QueryBuilder visitAnd(AndExpression<User> expr) {
        return QueryBuilders.boolQuery()
            .must(expr.getLeft().accept(this))
            .must(expr.getRight().accept(this));
    }
    
    // ... 其他方法
}

// 使用
ElasticsearchVisitor visitor = new ElasticsearchVisitor();
List<QueryBuilder> queries = spec.selectCompiler(visitor);
```

### 自定义 Evaluator

```java
public class CachingEvaluator implements ISpecificationEvaluator {
    
    private final Cache<String, Collection<?>> cache = ...;
    private final ISpecificationEvaluator delegate = InMemorySpecificationEvaluator.DEFAULT;
    
    @Override
    public <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> spec) {
        String key = computeCacheKey(spec);
        return (Collection<T>) cache.get(key, () -> delegate.evaluate(entities, spec));
    }
}
```

---

## 存储后端

### 后端对比

| 特性 | MyBatis-Plus | Redis |
|------|--------------|-------|
| WHERE 下推 | ✅ SQL | ❌ 内存过滤 |
| ORDER BY 下推 | ✅ SQL | ❌ 内存排序 |
| 分页 | ✅ 数据库分页 | ⚠️ 全量扫描后分页 |
| 事务 | ✅ 支持 | ❌ 单命令原子性 |
| 批量操作 | ✅ 批量 SQL | ⚠️ 逐条 Lua |
| 适用场景 | 关系型数据库 | 缓存、快速读取 |

### MyBatis-Plus 表达式支持

MyBatis-Plus 后端支持所有表达式类型的 SQL 下推：

```java
@Override
public Set<Class<? extends IExpression<T>>> supportedExpressions() {
    // 返回 16 种支持的表达式类型
    // EqualExpression, NotEqualExpression, GreaterThanExpression, ...
}
```

### Redis 存储架构

```
ZSET  users:index          → 存储 id 和 score（用于排序/分页）
HASH  user:1               → 存储实体字段
HASH  user:2               → 存储实体字段
...
```

写入操作通过 Lua 脚本保证原子性：

```lua
-- 保存
redis.call('ZADD', zsetKey, score, id)
redis.call('HSET', entityKey, field1, value1, field2, value2, ...)
redis.call('EXPIRE', entityKey, ttl)

-- 删除
redis.call('ZREM', zsetKey, id)
redis.call('DEL', entityKey)
```

---

## 附录

### 完整示例

```java
// 实体
@Data
public class User {
    private Long id;
    private String name;
    private Integer age;
    private String status;
    private String email;
    private LocalDateTime createdAt;
}

// Repository
@Repository
public class UserRepository extends MybatisPlusBaseRepository<User, Long> {
    public UserRepository(UserMapper mapper, Executor executor) {
        super(mapper, executor);
    }
    
    // 自定义查询
    public List<User> findActiveAdults() {
        return new ArrayList<>(findRange(
            Specification.where(User::getStatus).eq("ACTIVE")
                .and(User::getAge).gte(18)
                .orderBy(User::getName)
                .build()
        ));
    }
    
    public IPageResult<User> search(String keyword, int page, int size) {
        return page(
            Specification.where(User::getName).like("%" + keyword + "%")
                .or(User::getEmail).like("%" + keyword + "%")
                .orderByDescending(User::getCreatedAt)
                .build(),
            new PageQuery(page, size)
        );
    }
}

// 使用
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepo;
    
    public User getUser(Long id) {
        return userRepo.findById(id);
    }
    
    public List<User> searchUsers(String keyword) {
        return userRepo.search(keyword, 1, 20).getRecords();
    }
    
    public CompletableFuture<User> createUserAsync(User user) {
        return userRepo.saveAsync(user);
    }
    
    public long countActiveUsers() {
        return userRepo.count(
            Specification.where(User::getStatus).eq("ACTIVE").build()
        );
    }
}
```

### 常见问题

**Q: 快捷 API 和传统 API 可以混用吗？**

A: 可以。两者都生成 `ISpecification`，可以互相传递。

**Q: 内存评估支持排序吗？**

A: 支持。`spec.evaluate(collection)` 会同时执行 WHERE 过滤和 ORDER BY 排序。

**Q: 如何自定义异步线程池？**

A: 覆盖 Repository 的 `getAsyncExecutor()` 方法，或在构造时传入自定义 `Executor`。

**Q: Redis 后端支持分页吗？**

A: 支持，但实现是全量扫描后在内存分页。大数据量场景建议使用 MyBatis-Plus。

**Q: 如何实现软删除？**

A: 使用 `isNull`/`isNotNull` 判断删除时间字段：

```java
Specification.where(User::getDeletedAt).isNull()  // 未删除
```
