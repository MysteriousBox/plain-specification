package org.plain.specification.redis;

import org.plain.specification.core.*;
import org.plain.specification.core.spi.ISpecificationExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Redis 通用仓储抽象基类（ZSET + per-entity HASH 存储）。
 *
 * <p>设计模式说明：
 * <ul>
 *   <li>本类<strong>只依赖最终组装好的配置对象</strong>（{@link RedisRepositoryConfig}），不负责任何配置组装、计算或合并。</li>
 *   <li>所有配置组装、合并、覆盖逻辑应在外部（如 Resolver/Factory）完成，传入本类的 config 必须是可直接使用的最终配置。</li>
 *   <li>本类专注于仓储操作，所有配置项均通过 config 字段只读获取。</li>
 * </ul>
 * <p>存储架构：
 * <ul>
 *   <li>索引：使用 ZSET（配置项 {@link RedisRepositoryConfig#getResolvedZSetKey()}）存储 id 与 score，用于有序/分页扫描；</li>
 *   <li>实体：每个实体占用一个独立的 HASH，key = {@link RedisRepositoryConfig#getResolvedHashKeyPrefix()} + id，
 *       HASH 的 field 对应实体属性（由 {@link FieldExtractor} 提取）；</li>
 * </ul>
 * 此类保证写入索引与实体的原子性（通过 Lua 脚本完成 ZADD + HSET(s) + EXPIRE）。
 * 使用说明：构建 {@link RedisRepositoryConfig} 时必须提供 {@link FieldExtractor} 与 {@link EntityBuilder}，
 * 否则在运行时会抛出明确的异常。
 * </p>
 *
 * @param <T>   实体类型
 * @param <TID> 实体主键类型（建议实现 {@link java.io.Serializable}）
 * @author Jayden.Liang
 * @since 1.0
 */
@Slf4j
public abstract class BaseRedisRepository<T, TID> implements IBaseRepository<T, TID>, ISpecificationExecutor<T> {

    private static final String ERROR_ID_FROM_ZSET_RANGE_MUST_NOT_BE_NULL = "id from ZSet range must not be null";

    private final StringRedisTemplate redisTemplate;
    private final RedisRepositoryConfig<T, TID> config;
    private final RedisHashMapper<T, TID> hashMapper;
    private final RedisLuaWriter<T, TID> luaWriter;
    private final RedisBatchScanner<T, TID> batchScanner;

    /**
     * 构造器。
     *
     * @param redisTemplate 非空的 {@link StringRedisTemplate}
     * @param config        非空的 {@link RedisRepositoryConfig}
     * @throws IllegalArgumentException 当参数为 null 时抛出
     */
    protected BaseRedisRepository(StringRedisTemplate redisTemplate, RedisRepositoryConfig<T, TID> config) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("redisTemplate must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.hashMapper = new RedisHashMapper<>(redisTemplate, config);
        this.luaWriter = new RedisLuaWriter<>(redisTemplate, config, hashMapper);
        this.batchScanner = new RedisBatchScanner<>(redisTemplate, config, hashMapper, this::fetchHashes);
    }

    /**
     * 返回当前仓储的配置对象（只读）。
     *
     * @return {@link RedisRepositoryConfig} 实例（不可为 {@code null}）
     * @since 1.0
     */
    protected RedisRepositoryConfig<T, TID> getConfig() {
        return config;
    }

    // =========================================================================
    // CRUD — save / update
    // =========================================================================

    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to save must not be null");
        }
        return luaWriter.saveAtomic(entity);
    }

    @Override
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities to saveRangeAsync must not be null");
        }
        return CompletableFuture.supplyAsync(() -> {
            for (T e : entities) {
                luaWriter.saveAtomic(e);
            }
            return entities;
        }, getAsyncExecutor());
    }

    @Override
    public void update(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to update must not be null");
        }
        luaWriter.saveAtomic(entity);
    }

    @Override
    public CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities to updateRangeAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> entities.forEach(luaWriter::saveAtomic), getAsyncExecutor());
    }

    // =========================================================================
    // CRUD — delete
    // =========================================================================

    @Override
    public void deleteById(TID id) {
        if (id == null) {
            throw new IllegalArgumentException("id to deleteById must not be null");
        }
        luaWriter.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<TID> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("ids to deleteByIds must not be null");
        }
        luaWriter.deleteByIds(ids);
    }

    @Override
    public void delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to delete must not be null");
        }
        luaWriter.deleteAtomic(entity);
    }

    @Override
    public CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities to deleteRangeAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> entities.forEach(luaWriter::deleteAtomic), getAsyncExecutor());
    }

    @Override
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to deleteRangeAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> {
            Collection<T> entities = findRange(specification);
            if (entities != null && !entities.isEmpty()) {
                entities.forEach(luaWriter::deleteAtomic);
            }
        }, getAsyncExecutor());
    }

    // =========================================================================
    // CRUD — read
    // =========================================================================

    @Override
    public T findById(TID id) {
        if (id == null) {
            throw new IllegalArgumentException("id to findById must not be null");
        }
        final String idStr = String.valueOf(id);
        final String entityKey = getKey(idStr);
        final EntityBuilder<T> builder = getConfig().getEntityBuilder();
        if (builder == null) {
            throw new IllegalStateException("EntityBuilder must be configured to read from per-entity HASH");
        }
        return hashMapper.mapHashToEntity(entityKey, builder);
    }

    @Override
    public T findOne(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findOne must not be null");
        }
        return batchScanner.findOneWithBatch(specification);
    }

    @Override
    public Collection<T> findRange(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findRange must not be null");
        }
        return batchScanner.findRangeWithBatch(specification, 0, -1);
    }

    // =========================================================================
    // count / page
    // =========================================================================

    @Override
    public long count(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to count must not be null");
        }
        return batchScanner.countOptimized(specification);
    }

    /**
     * 统计所有实体数量（无条件）。
     *
     * @return 当前仓储所有实体数量
     * @author Jayden.Liang
     * @since 1.0
     */
    public long count() {
        Long total = redisTemplate.opsForZSet().size(Objects.requireNonNull(zSetKey()));
        return total == null ? 0L : total;
    }

    @Override
    public IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to page must not be null");
        }
        if (pageQuery == null) {
            throw new IllegalArgumentException("pageQuery to page must not be null");
        }
        if (pageQuery.getPage() < 1) {
            throw new IllegalArgumentException("Page number must be greater than 0, actual: " + pageQuery.getPage());
        }
        if (pageQuery.getPageSize() < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0, actual: " + pageQuery.getPageSize());
        }

        if (!batchScanner.hasWhereFilters(specification)) {
            return pageOptimized(specification, pageQuery);
        }

        final Collection<T> allMatched = batchScanner.findRangeWithBatch(specification, 0, -1);
        final List<T> matchedList = new ArrayList<>(allMatched);
        final long total = matchedList.size();
        final int fromIndex = (int) Math.min((long) (pageQuery.getPage() - 1) * pageQuery.getPageSize(), total);
        final int toIndex = (int) Math.min((long) fromIndex + pageQuery.getPageSize(), total);
        final List<T> pageRecords = new ArrayList<>(matchedList.subList(fromIndex, toIndex));
        return new RedisPageResultAdapter<>(pageRecords, pageQuery, total);
    }

    /**
     * 优化分页路径：无 WHERE 过滤时，直接使用 ZSET 原生命令。
     * <ul>
     *   <li>ZCARD — O(1) 获取总数</li>
     *   <li>ZRANGE with offset/count — O(log N + M) 仅获取当前页 ID</li>
     *   <li>Pipeline HGETALL — 仅读取当前页实体</li>
     * </ul>
     */
    private IPageResult<T> pageOptimized(ISpecification<T> specification, PageQuery pageQuery) {
        final String zKey = Objects.requireNonNull(zSetKey());
        final long total = batchScanner.countOptimized(specification);

        final int page = pageQuery.getPage();
        final int pageSize = pageQuery.getPageSize();
        final long offset = (long) (page - 1) * pageSize;

        if (offset >= total) {
            return new RedisPageResultAdapter<>(Collections.emptyList(), pageQuery, total);
        }

        final Set<?> ids = redisTemplate.opsForZSet().range(zKey, offset, offset + pageSize - 1);
        if (ids == null || ids.isEmpty()) {
            return new RedisPageResultAdapter<>(Collections.emptyList(), pageQuery, total);
        }

        final List<String> keyList = ids.stream()
                .map(item -> Objects.requireNonNull(item, ERROR_ID_FROM_ZSET_RANGE_MUST_NOT_BE_NULL))
                .map(item -> getKey(item.toString()))
                .collect(Collectors.toList());

        final List<Map<Object, Object>> hashes = fetchHashes(keyList);
        final List<T> records = hashMapper.buildEntities(hashes);

        return new RedisPageResultAdapter<>(records, pageQuery, total);
    }

    // =========================================================================
    // Protected fetchHashes — overridable for test compatibility
    // =========================================================================

    /**
     * Batch-read HASH entries for the given keys (pipeline).
     * Subclasses may override this method (e.g. for testing without pipeline mocks).
     *
     * @param keyList entity key list
     * @return list of field maps (null for missing keys)
     * @since 1.0
     */
    protected List<Map<Object, Object>> fetchHashes(List<String> keyList) {
        return hashMapper.fetchHashes(keyList);
    }

    // =========================================================================
    // Accessor / SPI methods
    // =========================================================================

    protected final String serialize(T entity) {
        try {
            return getConfig().getSerializer().serialize(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Serialization failed", e);
        }
    }

    protected final T deserialize(String s) {
        try {
            return getConfig().getDeserializer().deserialize(s);
        } catch (Exception e) {
            throw new IllegalStateException("Deserialization failed", e);
        }
    }

    /**
     * 获取 ZSet 的 key（从配置读取）。
     *
     * @return ZSet 的 Redis key
     * @throws IllegalStateException 当配置中未设置 zSetKey 时抛出
     * @since 1.0
     */
    protected final String zSetKey() {
        String k = getConfig().getResolvedZSetKey();
        if (k == null || k.isEmpty()) {
            throw new IllegalStateException("resolvedZSetKey must be configured and not empty");
        }
        return k;
    }

    /**
     * 使用配置的 {@link IdExtractor} 从实体中提取 id。
     *
     * @param entity 实体对象
     * @return 提取出的 id
     * @throws IllegalStateException 当提取失败时抛出
     * @since 1.0
     */
    protected final TID getId(T entity) {
        try {
            return getConfig().getIdExtractor().getId(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract id", e);
        }
    }

    /**
     * 使用配置的 {@link ScoreProvider} 计算当前实体的 score 值（用于 ZSET）。
     *
     * @param entity 实体
     * @return score 值
     * @since 1.0
     */
    protected final double getScore(T entity) {
        return getConfig().getScoreProvider().getScore(entity);
    }

    /**
     * 使用配置的 {@link TtlProvider} 计算实体的 TTL（秒）；若为 {@code null} 则使用配置的默认 TTL。
     *
     * @param entity 实体
     * @return TTL（秒）或 {@code null}
     * @since 1.0
     */
    protected final Long getTtl(T entity) {
        return getConfig().getTtlProvider().getTtl(entity);
    }

    /**
     * 获取用于异步执行的 {@link Executor}，若未配置则返回 {@link ForkJoinPool#commonPool()}。
     *
     * @return 异步执行器
     * @since 1.0
     */
    @Override
    public Executor getAsyncExecutor() {
        Executor exe = getConfig().getAsyncExecutor();
        return exe != null ? exe : ForkJoinPool.commonPool();
    }

    /**
     * 生成实体对应的 per-entity HASH 的 Redis key。
     *
     * @param id 实体 id 的字符串形式（不得为 {@code null}）
     * @return 具体的 Redis key（例如：prefix + id）
     * @since 1.0
     */
    protected String getKey(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String prefix = getConfig().getResolvedHashKeyPrefix();
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalStateException("resolvedHashKeyPrefix in config must not be null or empty");
        }
        return prefix + id;
    }

    // -------------------------------------------------------------------------
    // ISpecificationExecutor SPI
    // -------------------------------------------------------------------------

    @Override
    public org.plain.specification.core.visitor.IExpressionVisitor<T, ?> getVisitor() {
        throw new UnsupportedOperationException(
                "Redis repository does not support expression pushdown; all filtering is performed in-memory via batch scan");
    }

    @Override
    public List<T> execute(ISpecification<T> specification) {
        return new ArrayList<>(findRange(specification));
    }

    @Override
    public IPageResult<T> execute(ISpecification<T> specification, PageQuery pageQuery) {
        return page(specification, pageQuery);
    }

    @Override
    public Set<Class<? extends org.plain.specification.core.expression.IExpression<T>>> supportedExpressions() {
        return Collections.emptySet();
    }
}
