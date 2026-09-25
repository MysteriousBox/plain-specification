# plain-specification 项目路线图

## 项目现状评估

### 基本信息
- **版本**: 1.0-SNAPSHOT（尚未发布正式版）
- **Java 基线**: JDK 8
- **测试**: 251 个测试用例全部通过
- **TODO/FIXME**: 0（代码干净）

### 模块结构
```
plain-specification/
├── plain-specification-core/                    核心表达式 DSL + Visitor 模式
├── plain-specification-executor/
│   ├── mybatis-plus/                            MyBatis-Plus 执行器 + 自动配置
│   └── redis/                                   Redis 执行器 + 自动配置
├── plain-specification-spring-boot-starter/     Spring Boot Starter（一键引入）
└── plain-specification-example/                 H2 + MyBatis-Plus 完整示例
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

#### 第三轮：功能增强
| # | 功能 | 状态 |
|---|------|------|
| 21 | `Comparisons` 比较操作符辅助类 | ✅ 完成 |
| 22 | `QuickSpecificationBuilder` 快速 API（字段优先链式调用） | ✅ 完成 |
| 23 | `FieldCondition` 类型安全条件构建 | ✅ 完成 |
| 24 | `thenBy`/`thenByDescending` 多字段排序 | ✅ 完成 |
| 25 | `OrderExpression.ascending` 显式排序方向（修复 SQL 排序方向检测） | ✅ 完成 |
| 26 | MyBatis-Plus 自动配置（async executor + pagination interceptor） | ✅ 完成 |
| 27 | Redis 分页优化（无 WHERE 时使用 ZCARD + ZRANGE） | ✅ 完成 |
| 28 | Redis 自动配置增强（`@ConditionalOnClass` 守卫） | ✅ 完成 |
| 29 | Spring Boot Starter 模块 | ✅ 完成 |
| 30 | `@EnableSpecification` 注解 | ✅ 完成 |
| 31 | Example 模块（H2 + MyBatis-Plus 完整示例 + 18 集成测试） | ✅ 完成 |

---

## 路线图

### Phase 1: 稳定化与质量基线 (v0.1) ✅ 完成

> 目标: 代码质量达到可发布标准，补齐测试和文档

- [x] 20 个 bug 修复
- [x] 251 个测试用例全部通过
- [x] README.md / CONTRIBUTING.md
- [x] Maven Central 发布准备（GPG、Source JAR、Javadoc JAR）

---

### Phase 2: 功能完善 (v0.1 → v0.5)

> 目标: 补齐核心功能短板，提升实用性

#### 2.1 快速 API ✅ 完成
- [x] `QuickSpecificationBuilder` 字段优先链式调用
- [x] `FieldCondition` 类型安全条件构建
- [x] `thenBy`/`thenByDescending` 多字段排序
- [x] `OrderExpression.ascending` 显式排序方向

#### 2.2 自动配置 ✅ 完成
- [x] MyBatis-Plus 自动配置（async executor + pagination interceptor）
- [x] Redis 自动配置增强（`@ConditionalOnClass` 守卫）
- [x] 属性外部化（`plain.specification.async.*`、`plain.redis.repo.*`）

#### 2.3 Redis 性能优化 ✅ 完成
- [x] Pipeline 批量读取
- [x] 无 WHERE 过滤时 ZCARD + ZRANGE 原生分页

#### 2.4 Redis 表达式下推 [优先级: 高]
当前 Redis 模块不支持任何表达式下推，所有查询全量扫描 + 内存过滤。可实现基础下推:

| 表达式 | Redis 实现方式 | 复杂度 |
|---|---|---|
| `Equal` | ZSET 的 `ZRANGEBYSCORE`（配合 score 索引）或 HASH 字段匹配 | 低 |
| `GreaterThan` / `LessThan` | `ZRANGEBYSCORE` 的 min/max 参数 | 低 |
| `In` | 多次 `ZRANGEBYSCORE` 合并 | 中 |
| `Between` | `ZRANGEBYSCORE` 的 min + max | 低 |
| `IsNull` / `IsNotNull` | HASH 字段存在性检查 | 低 |
| `OrderBy` | ZSET 天然有序，直接利用 | 低 |

#### 2.5 排序求值器 [优先级: 中]
- [ ] 实现 `OrderEvaluator`，在 `InMemorySpecificationEvaluator` 默认构造中注册
- [ ] 排序应在过滤之后应用

#### 2.6 更多存储后端 [优先级: 中]
| 后端 | 价值 | 复杂度 |
|---|---|---|
| **Elasticsearch** | 全文搜索 + 结构化查询 | 高 |
| **MongoDB** | 文档数据库 | 中 |
| **JPA/Hibernate** | 覆盖传统 JPA 用户 | 中 |
| **内存集合 (Collections)** | 零依赖纯内存实现 | 低 |

#### 2.7 动态 Specification [优先级: 中]
运行时动态构建表达式，适用于 REST API 通用查询接口、管理后台筛选器。

#### 2.8 Specification 组合器 [优先级: 低]
支持多个 Specification 的 AND/OR/NOT 组合。

---

### Phase 3: 生态与成熟度 (v0.5 → v1.0)

> 目标: 达到生产可用的成熟度，建立生态

#### 3.1 Spring Boot Starter ✅ 完成
- [x] `plain-specification-spring-boot-starter` 一键引入
- [x] `@EnableSpecification` 注解
- [x] 属性配置（`plain.specification.async.*`、`plain.redis.repo.*`）
- [ ] Actuator 健康检查（Redis 连接、连接池状态）

#### 3.2 性能基准测试 [优先级: 中]
- [ ] JMH 基准: 表达式构建、Visitor 编译、内存求值
- [ ] Redis 基准: 单条/批量读写、Pipeline vs 非 Pipeline
- [ ] MyBatis-Plus 基准: 表达式编译为 SQL 的开销

#### 3.3 可观测性 [优先级: 中]
- [ ] 慢查询日志
- [ ] Metrics 集成（Micrometer）
- [ ] 查询计划日志

#### 3.4 高级特性 [优先级: 低]
- [ ] Specification 缓存
- [ ] 批量操作优化
- [ ] 乐观锁支持
- [ ] 事件发布
- [ ] 多租户增强

#### 3.5 发布 v1.0 [优先级: 高]
- [ ] API 冻结
- [ ] 完整的 Javadoc
- [ ] 迁移指南
- [ ] Maven Central 正式发布

---

## 当前进度总览

```
Phase 1: 稳定化与质量基线 (v0.1)
  ████████████████████████ 100% ✅
  ✅ 20 个 bug 修复
  ✅ 251 个测试用例全通过
  ✅ README / CONTRIBUTING / Maven Central 准备

Phase 2: 功能完善 (v0.5)
  ████████████░░░░░░░░░░░░ 55%
  ✅ 快速 API（QuickSpecificationBuilder + FieldCondition）
  ✅ 自动配置（MyBatis-Plus + Redis）
  ✅ Redis 分页优化（ZCARD + ZRANGE）
  ✅ Example 模块
  ⬜ Redis 表达式下推
  ⬜ 排序求值器
  ⬜ 更多存储后端
  ⬜ 动态 Specification

Phase 3: 生态与成熟度 (v1.0)
  ████░░░░░░░░░░░░░░░░░░░░ 15%
  ✅ Spring Boot Starter
  ✅ @EnableSpecification
  ⬜ Actuator 健康检查
  ⬜ 性能基准测试
  ⬜ 可观测性
  ⬜ 高级特性
```

## 建议的下一步行动

1. **短期**: 实现 Redis 基础表达式下推（Equal/Between/GreaterThan/LessThan），减少全量扫描
2. **中期**: 实现 `OrderEvaluator`，补齐内存排序管线
3. **中长期**: 新增 Elasticsearch 或 JPA 执行器
4. **长期**: Actuator 健康检查、性能基准测试、发布 v1.0
