# plain-specification 项目路线图

## 项目现状评估

### 基本信息
- **版本**: 1.0-SNAPSHOT（尚未发布正式版）
- **代码规模**: 88 个 Java 源文件，约 7,100 行生产代码
- **Java 基线**: JDK 8（已有升级到 JDK 25 的计划，部分完成）
- **测试**: 6 个测试文件，67 个测试用例，测试/源码比约 28%
- **TODO/FIXME**: 0（代码干净）

### 模块结构
```
plain-specification/
├── plain-specification-core/          (2,946 行, 测试比 7%)
│   ├── 表达式 DSL (Expressions, 18 种表达式类型)
│   ├── Visitor 模式 (内存/SQL/排序 三种编译目标)
│   ├── 混合执行策略 (HybridSpecificationEvaluator)
│   ├── Repository 接口 (IBaseRepository, IReadBaseRepository)
│   └── 分页/排序/验证 基础设施
├── plain-specification-executor/
│   ├── mybatis-plus/                  (1,324 行, 测试比 56%)
│   │   ├── 表达式 → QueryWrapper 编译
│   │   ├── 域对象/PO 双向转换
│   │   ├── 多租户支持
│   │   └── 同步 + 异步 (CompletableFuture) 全量 CRUD
│   └── redis/                         (2,846 行, 测试比 20%)
│       ├── ZSET + per-entity HASH 存储
│       ├── Lua 脚本原子写入
│       ├── Pipeline 批量读取
│       ├── Spring Boot Auto-Configuration
│       └── 策略模式 (Serializer/IdExtractor/FieldExtractor 等全部可替换)
```

### 已完成的工作

#### 第一轮修复 (11 项)
| # | 严重度 | 问题 | 状态 |
|---|--------|------|------|
| 1 | 严重 | `@Transactional` 在异步方法上无效 | ✅ 已修复 |
| 2 | 严重 | Redis `page()` 分页结果错误 | ✅ 已修复 |
| 3 | 严重 | `visitBetween` NPE | ✅ 已修复 |
| 4 | 高 | ThreadLocal 内存泄漏 | ✅ 已修复 |
| 5 | 高 | `visitNotEqual` 语义不一致 | ✅ 已修复 |
| 6 | 高 | `multiGetHashes` 未使用 Pipeline | ✅ 已修复 |
| 7 | 中 | `getExceptions()` 命名错误 | ✅ 已修复 |
| 8 | 中 | LambdaUtil/MpFieldNameResolver 重复代码 | ✅ 已修复 |
| 9 | 中 | `supportedExpressions()` raw type 强转 | ✅ 已修复 |
| 10 | 中 | 日志框架不一致 (JUL vs SLF4J) | ✅ 已修复 |
| 11 | 中 | 新增快速 reflectionFieldExtractor | ✅ 已修复 |

#### 第二轮修复 (7 项)
| # | 严重度 | 问题 | 状态 |
|---|--------|------|------|
| 12 | 严重 | Pipeline 返回 `Map<byte[], byte[]>` 未反序列化 | ✅ 已修复 |
| 13 | 严重 | `selectCompiler()` NPE | ✅ 已修复 |
| 14 | 高 | `thenBy(false)` 不传播 chainDiscarded | ✅ 已修复 |
| 15 | 高 | `fastReflectionFieldExtractor` 未过滤 synthetic 字段 | ✅ 已修复 |
| 16 | 中 | `visitLike` null NPE | ✅ 已修复 |
| 17 | 中 | `visitExists`/`visitNotExists` null NPE | ✅ 已修复 |
| 18 | 中 | 字段遮蔽问题（子类字段被父类覆盖） | ✅ 已修复 |
| 19 | 低 | `getFileName` 命名不准确 → `getPropertyName` | ✅ 已修复 |
| 20 | 低 | 未使用的 import 残留 | ✅ 已修复 |

---

## 路线图

### Phase 1: 稳定化与质量基线 (当前 → v0.1)

> 目标: 代码质量达到可发布标准，补齐测试和文档

#### 1.1 测试覆盖 [优先级: 高] ✅ 完成
当前核心模块测试比已从 7% 提升到 28%:

| 已补充测试的类 | 模块 | 测试数 |
|---|---|---|
| `Specification` | core | 9 |
| `SpecificationBuilder` | core | 10 |
| `PredicateExpressionVisitor` | core | 32 |
| `HybridSpecificationEvaluator` | core | 6 |
| `Expressions` | core | 3 |
| `IExpression` | core | 7 |

**结果**: 67 个测试用例全部通过

#### 1.2 文档 [优先级: 高] ✅ 完成
- [x] 创建根目录 `README.md`（英文，项目介绍 + 快速开始 + 模块说明）
- [x] 创建 `CONTRIBUTING.md`（贡献指南）
- [ ] Core 模块 API 使用示例（已包含在 README.md 中）
- [ ] MyBatis-Plus 模块使用示例（已包含在 README.md 中）
- [ ] Redis 模块 README 补充 Pipeline、分页限制说明

#### 1.3 Java 版本升级 [优先级: 中]
- 第二次升级会话（source/target → 25）尚余 Step 3-4 未完成
- 需决定: 保持 JDK 8 源码兼容 vs 全面升级到 JDK 25 语法
- 建议: 保持 JDK 8 源码 + JDK 25 编译运行（第一次升级的方案），扩大用户群

#### 1.4 发布 v0.1 [优先级: 高] ✅ 就绪
- [x] 所有现有测试通过 ✅ (67/67)
- [x] 核心类测试覆盖 ✅
- [x] README.md 存在 ✅
- [x] Maven Central 发布准备 ✅（GPG、Javadoc JAR、Source JAR、nexus-staging-maven-plugin）

---

### Phase 2: 功能完善 (v0.1 → v0.5)

> 目标: 补齐核心功能短板，提升实用性

#### 2.1 Redis 表达式下推 [优先级: 高]
当前 Redis 模块不支持任何表达式下推，所有查询全量扫描 + 内存过滤。可实现基础下推:

| 表达式 | Redis 实现方式 | 复杂度 |
|---|---|---|
| `Equal` | ZSET 的 `ZRANGEBYSCORE`（配合 score 索引）或 HASH 字段匹配 | 低 |
| `GreaterThan` / `LessThan` | `ZRANGEBYSCORE` 的 min/max 参数 | 低 |
| `In` | 多次 `ZRANGEBYSCORE` 合并 | 中 |
| `Between` | `ZRANGEBYSCORE` 的 min + max | 低 |
| `IsNull` / `IsNotNull` | HASH 字段存在性检查 | 低 |
| `OrderBy` | ZSET 天然有序，直接利用 | 低 |

实现后，`supportedExpressions()` 返回支持的类型，`HybridSpecificationEvaluator` 自动决策下推还是内存过滤。

#### 2.2 排序求值器 [优先级: 中]
当前 `InMemorySpecificationEvaluator` 只注册了 `WhereEvaluator`，没有 `OrderEvaluator`。排序表达式虽然被编译为 `Comparator`，但内存评估管线不会自动应用排序。

- [ ] 实现 `OrderEvaluator`，在 `InMemorySpecificationEvaluator` 默认构造中注册
- [ ] 排序应在过滤之后应用

#### 2.3 更多存储后端 [优先级: 中]
Specification 模式的最大价值在于查询与存储解耦。可以增加:

| 后端 | 价值 | 复杂度 |
|---|---|---|
| **Elasticsearch** | 全文搜索 + 结构化查询，天然适合 Specification | 高 |
| **MongoDB** | 文档数据库，Spring Data MongoDB 生态成熟 | 中 |
| **JPA/Hibernate** | 覆盖传统 JPA 用户，Specification → Criteria API | 中 |
| **内存集合 (Collections)** | 零依赖的纯内存实现，用于测试和小型应用 | 低 |

建议优先做 **Elasticsearch**（与 Redis 互补：Redis 做缓存/快速查询，ES 做全文搜索）和 **JPA**（最大用户群）。

#### 2.4 动态 Specification [优先级: 中]
当前表达式 DSL 是编译时类型安全的（`SFunction<T,R>` lambda），但缺少运行时动态构建能力:

```java
// 期望的 API：从 HTTP 请求参数动态构建
Specification<User> spec = new Specification<User>()
    .query()
    .where(Expressions.dynamic("name", Operator.LIKE, "%Alice%"))
    .where(Expressions.dynamic("age", Operator.GT, 18))
    .getSpecification();
```

适用于: REST API 通用查询接口、管理后台筛选器、报表系统。

#### 2.5 Specification 组合器 [优先级: 低]
支持多个 Specification 的 AND/OR/NOT 组合:

```java
Specification<User> active = ...;
Specification<User> adult = ...;
Specification<User> combined = Specifications.and(active, adult);
Specification<User> either = Specifications.or(active, adult);
```

---

### Phase 3: 生态与成熟度 (v0.5 → v1.0)

> 目标: 达到生产可用的成熟度，建立生态

#### 3.1 Spring Boot Starter [优先级: 高]
- [ ] `plain-specification-spring-boot-starter` 自动配置
- [ ] `@EnableSpecification` 注解
- [ ] 属性配置（batchSize、defaultTtl、async 线程池等）
- [ ] Actuator 健康检查（Redis 连接、连接池状态）

#### 3.2 性能基准测试 [优先级: 中]
- [ ] JMH 基准: 表达式构建、Visitor 编译、内存求值
- [ ] Redis 基准: 单条/批量读写、Pipeline vs 非 Pipeline、不同数据量级
- [ ] MyBatis-Plus 基准: 表达式编译为 SQL 的开销
- [ ] 发布性能报告

#### 3.3 可观测性 [优先级: 中]
- [ ] 慢查询日志（超过阈值的 Specification 执行记录）
- [ ] Metrics 集成（Micrometer）: 查询次数、耗时、下推/内存过滤比例
- [ ] 查询计划日志（记录 HybridSpecificationEvaluator 的下推决策）

#### 3.4 高级特性 [优先级: 低]
- [ ] **Specification 缓存**: 相同表达式的 Predicate/QueryWrapper 编译结果缓存
- [ ] **批量操作优化**: Redis `saveRangeAsync` 当前逐条 Lua 调用，可优化为 MSET + 单次 Lua
- [ ] **乐观锁支持**: Repository 层面的版本号检查
- [ ] **事件发布**: 实体变更事件（SavedEvent、DeletedEvent），便于缓存失效/同步
- [ ] **多租户增强**: MyBatis-Plus 已有 TenantContext，Redis 已有 TenantProvider，统一抽象

#### 3.5 发布 v1.0 [优先级: 高]
- [ ] API 冻结（标记所有 public API 的 @since 版本）
- [ ] 完整的 Javadoc（所有 public 类/方法）
- [ ] 迁移指南（从 0.x 到 1.0 的 breaking changes）
- [ ] Maven Central 正式发布

---

## 当前进度总览

```
Phase 1: 稳定化与质量基线 (v0.1)
  ████████████████████████ 100% ✅
  ✅ 20 个 bug 修复完成
  ✅ 67 个测试用例全通过
  ✅ 核心类测试覆盖 (Specification, SpecificationBuilder, PredicateExpressionVisitor, HybridSpecificationEvaluator)
  ✅ README.md / CONTRIBUTING.md 已创建
  ✅ Maven Central 发布准备完成 (GPG, Source JAR, Javadoc JAR)

Phase 2: 功能完善 (v0.5)
  ████░░░░░░░░░░░░░░░░░░░░ 20%
  ✅ 混合执行策略框架已就位
  ✅ Redis Pipeline 优化已完成
  ⬜ Redis 表达式下推（待做）
  ⬜ 排序求值器（待做）
  ⬜ 更多存储后端（待做）
  ⬜ 动态 Specification（待做）

Phase 3: 生态与成熟度 (v1.0)
  ░░░░░░░░░░░░░░░░░░░░░░░░  0%
  ⬜ Spring Boot Starter
  ⬜ 性能基准测试
  ⬜ 可观测性
  ⬜ 高级特性
```

## 建议的下一步行动

1. **立即**: 补充核心类单元测试（`Specification`, `PredicateExpressionVisitor`, `SpecificationBuilder`）
2. **短期**: 创建 `README.md`，实现 Redis 基础表达式下推（Equal/Between/GreaterThan/LessThan）
3. **中期**: 实现 `OrderEvaluator`，开发 Spring Boot Starter
4. **长期**: 新增 Elasticsearch 或 JPA 执行器，发布 v1.0
