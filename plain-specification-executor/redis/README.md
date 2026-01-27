# Redis 仓储模块使用说明

本模块提供了基于 Spring Data Redis 的通用仓储抽象（ZSET + per-entity HASH 存储），支持实体的高效存储、分页、过期等操作。

## 依赖引入

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>org.plain</groupId>
    <artifactId>plain-specification-executor-redis</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 快速开始

### 1. 定义实体类

```java
public class User {
    private Long id;
    private String name;
    private Integer age;
    // getter/setter ...
}
```

### 2. 实现必要的接口

```java
import org.plain.specification.redis.*;

// 字段提取器：实体 -> 字段映射
FieldExtractor<User> fieldExtractor = user -> Map.of(
    "id", user.getId(),
    "name", user.getName(),
    "age", user.getAge()
);

// 实体构建器：字段映射 -> 实体
EntityBuilder<User> entityBuilder = fields -> {
    User u = new User();
    u.setId(Long.valueOf(fields.get("id").toString()));
    u.setName((String) fields.get("name"));
    u.setAge(Integer.valueOf(fields.get("age").toString()));
    return u;
};

// 主键提取器
IdExtractor<User, Long> idExtractor = User::getId;

// ZSet score 提供器（如按 id 排序）
ScoreProvider<User> scoreProvider = user -> user.getId();

// 可选：TTL 提供器
TtlProvider<User> ttlProvider = user -> 3600L; // 1小时

// 实体序列化/反序列化（如 JSON）
Serializer<User> serializer = user -> null; // 实现序列化
Deserializer<User> deserializer = str -> null; // 实现反序列化
```

### 3. 构建仓储配置

```java
RedisRepositoryConfig<User, Long> config = RedisRepositoryConfig.<User, Long>builder()
    .zSetKey("user:zset")
    .keyPrefix("user:")
    .serializer(serializer)
    .deserializer(deserializer)
    .idExtractor(idExtractor)
    .scoreProvider(scoreProvider)
    .ttlProvider(ttlProvider)
    .fieldExtractor(fieldExtractor)
    .entityBuilder(entityBuilder)
    .build();
```

### 4. 创建仓储实现

```java
public class UserRedisRepository extends RedisBaseRepository<User, Long> {
    public UserRedisRepository(StringRedisTemplate redisTemplate, RedisRepositoryConfig<User, Long> config) {
        super(redisTemplate, config);
    }
}
```

### 5. 使用示例

```java
UserRedisRepository repo = new UserRedisRepository(redisTemplate, config);
User user = new User();
user.setId(1L);
user.setName("张三");
user.setAge(20);
repo.save(user); // 保存
User loaded = repo.findById(1L); // 查询
repo.deleteById(1L); // 删除
```

更多高级用法请参考源码及接口注释。
