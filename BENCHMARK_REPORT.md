# Plain Specification 性能基准测试报告

## 测试环境

- **JDK**: OpenJDK 21.0.12
- **JMH**: 1.37
- **测试日期**: 2026-09-11
- **测试模式**: Average Time (平均耗时)

---

## 1. 内存评估性能

### 测试场景

| 基准测试 | 说明 |
|---------|------|
| `benchmarkSimpleFilter` | 单条件过滤：`age >= 30` |
| `benchmarkComplexFilter` | 多条件组合：`age >= 25 AND status = 'ACTIVE' OR age <= 20` |
| `benchmarkFilterWithOrder` | 过滤 + 排序：`age >= 25` 然后按 `age ASC, name DESC` 排序 |
| `benchmarkInMemoryEvaluator` | 使用 `InMemorySpecificationEvaluator` 直接评估 |

### 测试结果

| 数据量 | 简单过滤 | 复杂过滤 | 过滤+排序 | Evaluator |
|--------|---------|---------|----------|-----------|
| 100 | 0.37 μs | 0.51 μs | 1.18 μs | 0.33 μs |
| 1,000 | 3.95 μs | 5.23 μs | 31.51 μs | 3.88 μs |
| 10,000 | 37.09 μs | 53.63 μs | 405.08 μs | 37.81 μs |

### 分析

1. **线性增长**：简单过滤和复杂过滤的性能随数据量线性增长
   - 100 → 1,000：约 10 倍数据，10 倍时间
   - 1,000 → 10,000：约 10 倍数据，10 倍时间

2. **排序开销**：带排序的查询性能显著下降
   - 10,000 数据量时，排序开销是纯过滤的 **10 倍**（405 μs vs 37 μs）
   - 这是因为排序需要 O(n log n) 的复杂度

3. **Evaluator 直接调用**：与 `spec.evaluate()` 性能相当，无额外开销

### 建议

- **小数据量（< 1,000）**：内存评估完全可行，性能优秀
- **中等数据量（1,000 - 10,000）**：简单过滤仍可接受（< 50 μs），排序需谨慎
- **大数据量（> 10,000）**：建议使用数据库后端，避免全量内存评估

---

## 2. Specification 构建性能

### 测试场景

| 基准测试 | 说明 |
|---------|------|
| `benchmarkQuickApiSimple` | 快捷 API 单条件 |
| `benchmarkQuickApiComplex` | 快捷 API 多条件 |
| `benchmarkQuickApiWithOrder` | 快捷 API 带排序 |
| `benchmarkTraditionalApiSimple` | 传统 API 单条件 |
| `benchmarkTraditionalApiComplex` | 传统 API 多条件 |
| `benchmarkTraditionalApiWithOrder` | 传统 API 带排序 |

### 测试结果

| 构建方式 | 简单 | 复杂 | 带排序 |
|---------|------|------|--------|
| 快捷 API | 14.36 ns | 19.45 ns | 40.21 ns |
| 传统 API | 10.81 ns | 15.53 ns | 36.59 ns |

### 分析

1. **构建性能极高**：所有构建操作都在纳秒级别（10-40 ns）
2. **快捷 API 略慢**：比传统 API 慢约 30%，因为需要额外的类型推断和链式调用处理
3. **排序增加开销**：带排序的构建比纯过滤慢约 2-3 倍

### 结论

- **构建开销可忽略**：相比评估性能（微秒级），构建性能（纳秒级）几乎无影响
- **快捷 API 推荐使用**：虽然略慢 30%，但绝对差异仅 5-10 ns，可读性优势远超性能损失

---

## 3. 性能优化建议

### 3.1 查询优化

```java
// ❌ 不推荐：大数据量内存排序
List<User> users = getAllUsers(); // 100,000 条
spec.evaluate(users); // 排序耗时可能超过 1 秒

// ✅ 推荐：使用数据库后端
userRepo.page(spec, new PageQuery(1, 20)); // 数据库排序，毫秒级
```

### 3.2 索引建议

对于 MyBatis-Plus 后端，确保常用查询字段有索引：

```java
// 高频查询
Specification.where(User::getStatus).eq("ACTIVE")
    .and(User::getAge).gte(18)
    .build();

// 建议索引
CREATE INDEX idx_status_age ON users(status, age);
```

### 3.3 缓存策略

对于重复查询，考虑缓存 Specification 结果：

```java
// 缓存 Specification 对象（构建成本低）
private static final ISpecification<User> ACTIVE_USERS = 
    Specification.where(User::getStatus).eq("ACTIVE").build();

// 缓存查询结果（评估成本高）
@Cacheable("activeUsers")
public List<User> getActiveUsers() {
    return userRepo.findRange(ACTIVE_USERS);
}
```

---

## 4. 与数据库查询对比（预估）

| 操作 | 内存评估 (10,000) | MySQL (预估) | Redis (预估) |
|------|------------------|--------------|--------------|
| 简单过滤 | 37 μs | 1-5 ms | 5-10 ms |
| 复杂过滤 | 54 μs | 2-10 ms | 10-20 ms |
| 排序 | 405 μs | 5-20 ms | 不支持 |
| 分页 | N/A | 1-5 ms | 10-50 ms |

**结论**：
- 内存评估在小数据量时性能优异
- 数据库在大数据量和复杂查询时更有优势
- Redis 适合快速读取，但不适合复杂查询

---

## 5. 运行基准测试

### 前置条件

```bash
mvn clean test-compile
```

### 运行单个基准

```bash
java -cp "target/test-classes:target/classes:$(mvn dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout)" \
  org.openjdk.jmh.Main InMemoryEvaluationBenchmark.benchmarkSimpleFilter
```

### 运行所有基准

```bash
java -cp "target/test-classes:target/classes:$(mvn dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout)" \
  org.openjdk.jmh.Main ".*Benchmark"
```

### 参数说明

- `-i 5`：测量迭代次数
- `-w 3`：预热迭代次数
- `-f 2`：Fork 次数
- `-bm avgt`：测量模式（平均时间）
- `-tu us`：时间单位（微秒）

---

## 6. 总结

| 指标 | 结论 |
|------|------|
| **构建性能** | 优秀（纳秒级），快捷 API 与传统 API 差异可忽略 |
| **小数据量评估** | 优秀（< 1,000 条，< 5 μs） |
| **中等数据量评估** | 良好（1,000-10,000 条，< 100 μs） |
| **排序性能** | 中等（10,000 条约 400 μs），建议大数据量使用数据库 |
| **扩展性** | 线性增长，适合中小规模数据 |

**最佳实践**：
1. 小数据量（< 10,000）：优先使用内存评估，简单高效
2. 大数据量（> 10,000）：使用 MyBatis-Plus 后端，利用数据库索引
3. 需要排序/分页：始终使用数据库后端
4. 缓存热点查询：减少重复评估开销
