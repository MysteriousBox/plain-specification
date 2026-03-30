package org.plain.specification.redis;

import org.plain.specification.core.*;
import org.plain.utils.JsonUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.logging.Logger;

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
public abstract class RedisBaseRepository<T, TID> implements IBaseRepository<T, TID> {
    private static final Logger logger = Logger.getLogger(RedisBaseRepository.class.getName());

    private final StringRedisTemplate redisTemplate;
    private final RedisRepositoryConfig<T, TID> config;

    /**
     * Lua 脚本：原子保存到 ZSET，并对每个实体的 HASH 执行 HSET(field,value ...) 与 EXPIRE（per-entity key）
     * KEYS: [zsetKey, entityKey]
     * ARGV: [score, id, field1, value1, field2, value2, ..., ttl]
     */
    private static final RedisScript<Long> LUA_SAVE_PER_ENTITY_SCRIPT = RedisScript.of(
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])\n" +
                    "local idx = 3\n" +
                    "local last = #ARGV\n" +
                    "while idx < last do\n" +
                    "  redis.call('HSET', KEYS[2], ARGV[idx], ARGV[idx+1])\n" +
                    "  idx = idx + 2\n" +
                    "end\n" +
                    "local ttl = tonumber(ARGV[last])\n" +
                    "if ttl and ttl > 0 then\n" +
                    "  redis.call('EXPIRE', KEYS[2], ttl)\n" +
                    "end\n" +
                    "return 1", Long.class);

    private static final RedisScript<Long> LUA_DELETE_PER_ENTITY_SCRIPT = RedisScript.of(
            "redis.call('ZREM', KEYS[1], ARGV[1])\n" +
                    "redis.call('DEL', KEYS[2])\n" +
                    "return 1", Long.class);

    private static final RedisScript<Long> LUA_DELETE_BATCH_PER_ENTITY_SCRIPT = RedisScript.of(
            "redis.call('ZREM', KEYS[1], unpack(ARGV))\n" +
                    "for i = 2, #KEYS do\n" +
                    "  redis.call('DEL', KEYS[i])\n" +
                    "end\n" +
                    "return 1", Long.class);

    /**
     * 构造器。
     *
     * @param redisTemplate 非空的 {@link StringRedisTemplate}
     * @param config        非空的 {@link RedisRepositoryConfig}
     * @throws IllegalArgumentException 当参数为 null 时抛出
     */
    protected RedisBaseRepository(StringRedisTemplate redisTemplate, RedisRepositoryConfig<T, TID> config) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("redisTemplate must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    /**
     * 返回当前仓储的配置对象（只读）。
     *
     * @return {@link RedisRepositoryConfig} 实例（不可为 {@code null}）
     * @since 1.0
     */
    @SuppressWarnings("unused")
    protected RedisRepositoryConfig<T, TID> getConfig() {
        return config;
    }

    /**
     * 保存单个实体（同步）。
     *
     * <p>处理流程：
     * <ol>
     *   <li>使用配置的 {@link FieldExtractor} 将实体拆分为 field->value 映射；</li>
     *   <li>通过 Lua 脚本原子地将 id/score 写入 ZSET，并对目标 per-entity HASH 执行 HSET field value ... 以及 EXPIRE；</li>
     *   <li>若未配置 FieldExtractor 或返回空映射会抛出异常以避免写入无效数据。</li>
     * </ol>
     * </p>
     *
     * @param entity 要保存的实体，不得为 {@code null}
     * @return 返回已保存的实体（通常为传入对象本身）
     * @throws IllegalArgumentException 当 {@code entity} 为 {@code null}
     * @throws IllegalStateException    当未配置 {@link FieldExtractor} 或者 FieldExtractor 返回空映射
     * @since 1.0
     */
    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to save must not be null");
        }
        return saveAtomic(entity);
    }

    /**
     * 内部：原子保存实现（ZSET + per-entity HASH）。
     */
    private T saveAtomic(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to save must not be null");
        }

        final String zKey = getConfig().getResolvedZSetKey();
        if (zKey == null || zKey.isEmpty()) {
            throw new IllegalStateException("resolvedZSetKey in config must not be null or empty");
        }
        final double score = getScore(entity);
        final String id = Objects.requireNonNull(getId(entity), "id extracted from entity must not be null").toString();
        final Long entityTtl = getTtl(entity);
        final long ttl = entityTtl == null ? getConfig().getDefaultTtl() : entityTtl;

        final FieldExtractor<T> fe = getConfig().getFieldExtractor();
        if (fe == null) {
            throw new IllegalStateException("FieldExtractor must be configured for per-entity HASH storage");
        }

        final Map<String, Object> fields = fe.extractFields(entity);
        if (fields == null || fields.isEmpty()) {
            throw new IllegalStateException("FieldExtractor returned empty field map");
        }


        final String entityKey = getKey(id);
        if (entityKey == null || entityKey.isEmpty()) {
            throw new IllegalStateException("entityKey must not be null or empty");
        }
        final List<String> argsList = new ArrayList<>(2 + fields.size() * 2 + 1);
        argsList.add(String.valueOf(score));
        argsList.add(id);
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            argsList.add(e.getKey());
            argsList.add(serializeFieldValue(e.getValue()));
        }
        argsList.add(String.valueOf(ttl));

        redisTemplate.execute(LUA_SAVE_PER_ENTITY_SCRIPT, Arrays.asList(zKey, entityKey), argsList.toArray());
        return entity;
    }

    /**
     * 异步保存实体。
     *
     * @param entity 要保存的实体，不得为 {@code null}
     * @return 异步完成时返回已保存实体的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code entity} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<T> saveAsync(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to saveAsync must not be null");
        }
        return CompletableFuture.supplyAsync(() -> {
            saveAtomic(entity);
            return entity;
        }, executor());
    }

    /**
     * 异步批量保存实体集合。
     *
     * @param entities 要保存的实体集合，不得为 {@code null}
     * @return 异步完成时返回已保存实体集合的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code entities} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities to saveRangeAsync must not be null");
        }
        return CompletableFuture.supplyAsync(() -> {
            for (T e : entities) {
                saveAtomic(e);
            }
            return entities;
        }, executor());
    }

    /**
     * 更新实体（等同于保存）。
     *
     * @param entity 要更新的实体，不得为 {@code null}
     * @throws IllegalArgumentException 当 {@code entity} 为 {@code null}
     * @since 1.0
     */
    @Override
    public void update(T entity) {
        saveAtomic(entity);
    }

    /**
     * 异步更新实体。
     *
     * @param entity 要更新的实体，不得为 {@code null}
     * @return 表示异步执行结果的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code entity} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Void> updateAsync(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to updateAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> saveAtomic(entity), executor());
    }

    /**
     * 异步批量更新实体集合。
     *
     * @param entities 实体集合，不得为 {@code null}
     * @return 表示异步执行结果的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code entities} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities to updateRangeAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> entities.forEach(this::saveAtomic), executor());
    }

    /**
     * 根据 id 删除实体（从索引和对应的 per-entity HASH 中移除）。
     *
     * @param id 要删除实体的 id，不得为 {@code null}
     * @throws IllegalArgumentException 当 {@code id} 为 {@code null}
     * @since 1.0
     */
    @Override
    public void deleteById(TID id) {
        if (id == null) {
            throw new IllegalArgumentException("id to deleteById must not be null");
        }
        final String zKey = zSetKey();
        final String idStr = String.valueOf(id);
        final String entityKey = getKey(idStr);
        redisTemplate.execute(LUA_DELETE_PER_ENTITY_SCRIPT, Arrays.asList(zKey, entityKey), idStr);
    }

    /**
     * 批量根据 id 集合删除实体（原子执行索引 ZREM 与删除相应的 per-entity HASH）。
     *
     * @param ids id 集合，不得为 {@code null} 或包含 {@code null}
     * @throws IllegalArgumentException 当 {@code ids} 为 {@code null} 或包含 {@code null}
     * @since 1.0
     */
    @Override
    public void deleteByIds(Collection<TID> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("ids to deleteByIds must not be null");
        }
        final String zKey = getConfig().getResolvedZSetKey();
        final List<String> keys = new ArrayList<>();
        keys.add(zKey);
        final List<String> idStrings = new ArrayList<>();
        for (TID id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("id in ids must not be null");
            }
            final String s = String.valueOf(id);
            idStrings.add(s);
            keys.add(getKey(s));
        }
        redisTemplate.execute(LUA_DELETE_BATCH_PER_ENTITY_SCRIPT, keys, idStrings.toArray());
    }

    /**
     * 根据实体删除（从索引和存储中移除）。
     *
     * @param entity 要删除的实体，不得为 {@code null}
     * @throws IllegalArgumentException 当 {@code entity} 为 {@code null}
     * @since 1.0
     */
    @Override
    public void delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to delete must not be null");
        }
        deleteAtomic(entity);
    }

    /**
     * 原子删除实体（内部方法）。
     *
     * @param entity 要删除的实体，不得为 {@code null}
     * @throws IllegalArgumentException 当 {@code entity} 为 {@code null}
     * @since 1.0
     */
    public void deleteAtomic(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to deleteAtomic must not be null");
        }
        final String id = getId(entity).toString();
        redisTemplate.execute(LUA_DELETE_PER_ENTITY_SCRIPT, Arrays.asList(zSetKey(), getKey(id)), id);
    }

    /**
     * 异步删除实体。
     *
     * @param entity 要删除的实体，不得为 {@code null}
     * @return 表示异步执行结果的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code entity} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to deleteAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> {
            try {
                deleteAtomic(entity);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor());
    }

    /**
     * 异步批量删除实体集合。
     *
     * @param entities 要删除的实体集合，不得为 {@code null}
     * @return 表示异步执行结果的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code entities} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities to deleteRangeAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> entities.forEach(this::deleteAtomic), executor());
    }

    /**
     * 按 id 查找实体（同步）。
     *
     * <p>实现：从 per-entity HASH 使用 {@code HGETALL}（通过 {@link StringRedisTemplate#opsForHash().entries}）读取所有字段，
     * 然后使用配置的 {@link EntityBuilder} 将字段映射重建为实体对象。</p>
     *
     * @param id 实体 id，不得为 {@code null}
     * @return 若找到则返回实体，否则返回 {@code null}
     * @throws IllegalArgumentException 当 {@code id} 为 {@code null}
     * @throws IllegalStateException    当未配置 {@link EntityBuilder} 或重建失败
     * @since 1.0
     */
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
        return mapHashToEntity(entityKey, builder);
    }

    /**
     * 异步按 id 查找实体。
     *
     * @param id 实体 id，不得为 {@code null}
     * @return 返回包装了查找结果的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code id} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<T> findByIdAsync(TID id) {
        if (id == null) {
            throw new IllegalArgumentException("id to findByIdAsync must not be null");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findById(id);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor());
    }

    /**
     * 根据规格查找单个实体（同步）。
     *
     * @param specification 规格，不得为 {@code null}
     * @return 满足规格的实体，若找到多个则返回第一个；若未找到则返回 {@code null}
     * @throws IllegalArgumentException 当 {@code specification} 为 {@code null}
     * @since 1.0
     */
    @Override
    public T findOne(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findOne must not be null");
        }
        return findOneWithBatch(specification);
    }

    /**
     * 根据规格查找实体集合（同步）。
     *
     * @param specification 规格，不得为 {@code null}
     * @return 满足规格的实体集合
     * @throws IllegalArgumentException 当 {@code specification} 为 {@code null}
     * @since 1.0
     */
    @Override
    public Collection<T> findRange(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findRange must not be null");
        }
        return findRangeWithBatch(specification, 0, -1);
    }

    /**
     * 异步根据规格查找单个实体。
     *
     * @param specification 规格，不得为 {@code null}
     * @return 异步结果，包含找到的实体或 null
     * @throws IllegalArgumentException 当 {@code specification} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<T> findOneAsync(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findOneAsync must not be null");
        }
        return CompletableFuture.supplyAsync(() -> findOne(specification), executor());
    }

    /**
     * 根据规格异步删除实体（先查出匹配实体然后逐个删除）。
     *
     * @param specification 规格，不得为 {@code null}
     * @return {@link CompletableFuture} 表示异步结果
     * @throws IllegalArgumentException 当 {@code specification} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to deleteRangeAsync must not be null");
        }
        return CompletableFuture.runAsync(() -> {
            Collection<T> entities = findRange(specification);
            if (entities != null && !entities.isEmpty()) {
                entities.forEach(this::deleteAtomic);
            }
        }, executor());
    }

    /**
     * 内部：逐 key 读取 HASH entries 的帮助方法（可被 pipeline 优化以提升性能）。
     *
     * @param keyList 需要读取的实体 key 列表
     * @return 与 keyList 顺序对应的 Map 列表（不存在的 key 对应 null）
     * @since 1.0
     */
    private List<Map<Object, Object>> multiGetHashes(List<String> keyList) {
        final List<Map<Object, Object>> list = new ArrayList<>(keyList.size());
        for (String k : keyList) {
            final Map<Object, Object> m = redisTemplate.opsForHash().entries(k);
            list.add(m.isEmpty() ? null : m);
        }
        return list;
    }

    /**
     * 根据 redis HASH key 获取 fields 并转换为实体。
     * @param redisKey HASH key
     * @param builder  实体构建器
     * @return 实体对象或 null
     */
    private T mapHashToEntity(String redisKey, EntityBuilder<T> builder) {
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(redisKey);
        if (fields.isEmpty()) {
            return null;
        }
        try {
            return builder.buildEntity(fields);
        } catch (Exception ex) {
            logger.warning("Failed to build entity from hash fields for key: " + redisKey + ", " + ex.getMessage());
            return null;
        }
    }

    private T findOneWithBatch(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findOneWithBatch must not be null");
        }
        if (getConfig().getBatchSize() <= 0) {
            throw new IllegalStateException("batchSize must be > 0");
        }

        long start = 0L;
        long end = getConfig().getBatchSize() - 1L;
        final String zKey = getConfig().getResolvedZSetKey();

        while (true) {
            final Set<?> ids = redisTemplate.opsForZSet().range(zKey, start, end);
            if (ids == null || ids.isEmpty()) {
                return null;
            }

            final List<String> keyList = ids.stream().map(item -> getKey(item.toString())).collect(Collectors.toList());
            final List<Map<Object, Object>> hashes = multiGetHashes(keyList);
            final EntityBuilder<T> builder = getConfig().getEntityBuilder();

            for (Map<Object, Object> fields : hashes) {
                if (fields == null) {
                    continue;
                }
                try {
                    final T t = builder.buildEntity(fields);
                    if (t != null && specification.isSatisfiedBy(t)) {
                        return t;
                    }
                } catch (Exception ex) {
                    logger.warning("Failed to build entity in findOneWithBatch: " + ex.getMessage());
                }
            }

            start += getConfig().getBatchSize();
            end += getConfig().getBatchSize();
        }
    }

    private Collection<T> findRangeWithBatch(ISpecification<T> specification, long start, long end) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findRangeWithBatch must not be null");
        }
        if (getConfig().getBatchSize() <= 0) {
            throw new IllegalStateException("batchSize must be > 0");
        }

        final List<T> results = new ArrayList<>();
        long currentStart = start;
        final String zKey = getConfig().getResolvedZSetKey();

        while (true) {
            final long fetchEnd = (end == -1) ? (currentStart + getConfig().getBatchSize() - 1) : Math.min(end, currentStart + getConfig().getBatchSize() - 1);
            final Set<?> ids = redisTemplate.opsForZSet().range(zKey, currentStart, fetchEnd);
            if (ids == null || ids.isEmpty()) {
                break;
            }

            final List<String> keyList = ids.stream().map(item -> getKey(item.toString())).collect(Collectors.toList());
            final List<Map<Object, Object>> hashes = multiGetHashes(keyList);
            final EntityBuilder<T> builder = getConfig().getEntityBuilder();

            for (Map<Object, Object> fields : hashes) {
                if (fields == null) {
                    continue;
                }
                try {
                    final T t = builder.buildEntity(fields);
                    if (t != null && specification.isSatisfiedBy(t)) {
                        results.add(t);
                    }
                } catch (Exception ex) {
                    logger.warning("Failed to build entity in findRangeWithBatch: " + ex.getMessage());
                }
            }

            if (end != -1 && fetchEnd >= end) {
                break;
            }
            currentStart += getConfig().getBatchSize();
            if (end == -1 && ids.size() < getConfig().getBatchSize()) {
                break;
            }
        }
        return results;
    }

    /**
     * 异步根据规格查找实体集合。
     *
     * @param specification 规格，不得为 {@code null}
     * @return 返回包装了查找结果的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code specification} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Collection<T>> findRangeAsync(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to findRangeAsync must not be null");
        }
        return CompletableFuture.supplyAsync(() -> findRange(specification), executor());
    }

    /**
     * 根据规格统计实体数量（同步）。
     *
     * @param specification 规格，不得为 {@code null}
     * @return 满足规格的实体数量
     * @throws IllegalArgumentException 当 {@code specification} 为 {@code null}
     * @throws IllegalStateException    当 {@code batchSize} 配置不正确
     * @since 1.0
     */
    @Override
    public long count(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to count must not be null");
        }
        if (getConfig().getBatchSize() <= 0) {
            throw new IllegalStateException("batchSize must be > 0");
        }

        final String zKey = getConfig().getResolvedZSetKey();
        long start = 0L;
        long end = getConfig().getBatchSize() - 1L;
        long result = 0L;

        while (true) {
            final Set<?> ids = redisTemplate.opsForZSet().range(zKey, start, end);
            if (ids == null || ids.isEmpty()) {
                return result;
            }
            final List<String> keyList = ids.stream().map(item -> getKey(item.toString())).collect(Collectors.toList());
            final EntityBuilder<T> builder = getConfig().getEntityBuilder();
            long matched = 0L;
            for (String key : keyList) {
                T t = mapHashToEntity(key, builder);
                if (t != null && specification.isSatisfiedBy(t)) {
                    matched++;
                }
            }

            result += matched;
            start += getConfig().getBatchSize();
            end += getConfig().getBatchSize();
        }
    }

    /**
     * 异步统计满足规格的实体数量。
     *
     * @param specification 规格，不得为 {@code null}
     * @return 返回包装了统计结果的 {@link CompletableFuture}
     * @throws IllegalArgumentException 当 {@code specification} 为 {@code null}
     * @since 1.0
     */
    @Override
    public CompletableFuture<Long> countAsync(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification to countAsync must not be null");
        }
        return CompletableFuture.supplyAsync(() -> count(specification), executor());
    }

    /**
     * 分页查询。
     *
     * @param specification 规格，不得为 {@code null}
     * @param pageQuery     分页查询参数，不得为 {@code null}
     * @return 分页结果，包含当前页数据及总记录数
     * @throws IllegalArgumentException 当参数不合法时抛出（例如 page 或 pageSize 小于 1）
     * @since 1.0
     */
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

        final String key = config.getResolvedZSetKey();
        final long start = (long) (pageQuery.getPage() - 1) * pageQuery.getPageSize();
        final long end = start + pageQuery.getPageSize() - 1;
        final Collection<T> entities = findRangeWithBatch(specification, start, end);

        Long total = redisTemplate.opsForZSet().size(key);
        if (total == null) {
            total = 0L;
        }
        return new RedisPageResultAdapter<>(entities, pageQuery, total);
    }

    @SuppressWarnings("unused")
    protected final String serialize(T entity) {
        try {
            return getConfig().getSerializer().serialize(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Serialization failed", e);
        }
    }

    @SuppressWarnings("unused")
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
    private Executor executor() {
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
        String prefix = getConfig().getResolvedHashKeyPrefix();
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalStateException("resolvedHashKeyPrefix in config must not be null or empty");
        }
        return prefix + id;
    }

    private String serializeFieldValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return String.valueOf(value);
        }
        // Prefer a configured FieldValueSerializer if provided
        final FieldValueSerializer fvs = getConfig().getFieldValueSerializer();
        if (fvs != null) {
            try {
                String s = fvs.serialize(value);
                if (s != null) {
                    return s;
                }
            } catch (Exception ex) {
                logger.warning("Failed to serialize field value with FieldValueSerializer: " + ex.getMessage());
                // fallback to default JsonUtil below
            }
        }
        try {
            return JsonUtil.serialize(value);
        } catch (Exception ex) {
            logger.warning("Failed to serialize field value with JsonUtil: " + ex.getMessage());
            // Last resort: use toString()
            return String.valueOf(value);
        }
    }

    /**
     * 统计所有实体数量（无条件）。
     *
     * @return 当前仓储所有实体数量
     * @author Jayden.Liang
     * @since 1.0
     */
    public long count() {
        Long total = redisTemplate.opsForZSet().size(zSetKey());
        return total == null ? 0L : total;
    }
}

