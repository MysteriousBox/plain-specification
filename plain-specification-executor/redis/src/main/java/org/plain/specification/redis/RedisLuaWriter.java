package org.plain.specification.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Helper class for atomic Lua-script write operations on Redis.
 *
 * <p>Extracted from {@link BaseRedisRepository} to reduce its size.
 * Contains all Lua script constants and the atomic save/delete methods.</p>
 *
 * @param <T>   entity type
 * @param <TID> entity primary key type
 * @author Jayden.Liang
 * @since 1.0
 */
@Slf4j
class RedisLuaWriter<T, TID> {

    private static final String LUA_SAVE_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL = "LUA_SAVE_PER_ENTITY_SCRIPT must not be null";
    private static final String LUA_DELETE_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL = "LUA_DELETE_PER_ENTITY_SCRIPT must not be null";
    private static final String LUA_DELETE_BATCH_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL = "LUA_DELETE_BATCH_PER_ENTITY_SCRIPT must not be null";

    /**
     * Lua script: atomic save to ZSET + per-entity HASH, clearing fields whose value is null.
     * <p>
     * A null reference has no HASH representation at all: the key is removed rather than written as
     * an empty string, so an unset field and a field holding "" stay two different things and the
     * read path rebuilds null as null.
     * <p>
     * KEYS: [zsetKey, entityKey]
     * ARGV: [score, id, ttl, pairCount, field1, value1, ..., nullCount, nullField1, ...]
     */
    private static final RedisScript<Long> LUA_SAVE_PER_ENTITY_SCRIPT = Objects.requireNonNull(RedisScript.<Long>of(
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])\n" +
                    "local ttl = tonumber(ARGV[3])\n" +
                    "local idx = 4\n" +
                    "local pairs = tonumber(ARGV[idx])\n" +
                    "idx = idx + 1\n" +
                    "for i = 1, pairs do\n" +
                    "  redis.call('HSET', KEYS[2], ARGV[idx], ARGV[idx + 1])\n" +
                    "  idx = idx + 2\n" +
                    "end\n" +
                    "local nulls = tonumber(ARGV[idx])\n" +
                    "for i = 1, nulls do\n" +
                    "  redis.call('HDEL', KEYS[2], ARGV[idx + i])\n" +
                    "end\n" +
                    "if ttl and ttl > 0 then\n" +
                    "  redis.call('EXPIRE', KEYS[2], ttl)\n" +
                    "end\n" +
                    "return 1", Long.class), LUA_SAVE_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL);

    private static final RedisScript<Long> LUA_DELETE_PER_ENTITY_SCRIPT = Objects.requireNonNull(RedisScript.<Long>of(
            "redis.call('ZREM', KEYS[1], ARGV[1])\n" +
                    "redis.call('DEL', KEYS[2])\n" +
                    "return 1", Long.class), LUA_DELETE_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL);

    private static final RedisScript<Long> LUA_DELETE_BATCH_PER_ENTITY_SCRIPT = Objects.requireNonNull(RedisScript.<Long>of(
            "redis.call('ZREM', KEYS[1], unpack(ARGV))\n" +
                    "for i = 2, #KEYS do\n" +
                    "  redis.call('DEL', KEYS[i])\n" +
                    "end\n" +
                    "return 1", Long.class), LUA_DELETE_BATCH_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL);

    private final StringRedisTemplate redisTemplate;
    private final RedisRepositoryConfig<T, TID> config;
    private final RedisHashMapper<T, TID> hashMapper;

    RedisLuaWriter(StringRedisTemplate redisTemplate, RedisRepositoryConfig<T, TID> config,
                   RedisHashMapper<T, TID> hashMapper) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.hashMapper = Objects.requireNonNull(hashMapper, "hashMapper must not be null");
    }

    /**
     * Atomically save entity (ZADD + HSET + EXPIRE via Lua).
     *
     * @param entity entity to save
     * @return the saved entity
     */
    T saveAtomic(T entity) {
        final String zKey = requireZSetKey();
        final double score = config.getScoreProvider().getScore(entity);
        final TID rawId = requireId(entity);
        final String id = Objects.requireNonNull(rawId, "id extracted from entity must not be null").toString();
        final Long entityTtl = config.getTtlProvider().getTtl(entity);
        final long ttl = entityTtl == null ? config.getDefaultTtl() : entityTtl;

        final FieldExtractor<T> fe = config.getFieldExtractor();
        if (fe == null) {
            throw new IllegalStateException("FieldExtractor must be configured for per-entity HASH storage");
        }

        final Map<String, Object> fields = fe.extractFields(entity);
        if (fields == null || fields.isEmpty()) {
            throw new IllegalStateException("FieldExtractor returned empty field map");
        }

        final String entityKey = requireKey(id);
        final List<String> nullFields = new ArrayList<>();
        final List<String> valuePairs = new ArrayList<>(fields.size() * 2);
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            if (e.getValue() == null) {
                nullFields.add(e.getKey());
                continue;
            }
            valuePairs.add(e.getKey());
            valuePairs.add(hashMapper.serializeFieldValue(e.getValue()));
        }

        final List<String> argsList = new ArrayList<>(5 + valuePairs.size() + nullFields.size());
        argsList.add(String.valueOf(score));
        argsList.add(id);
        argsList.add(String.valueOf(ttl));
        argsList.add(String.valueOf(valuePairs.size() / 2));
        argsList.addAll(valuePairs);
        argsList.add(String.valueOf(nullFields.size()));
        argsList.addAll(nullFields);

        final RedisScript<Long> script = Objects.requireNonNull(LUA_SAVE_PER_ENTITY_SCRIPT, LUA_SAVE_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL);
        final List<String> keys = new ArrayList<>(2);
        keys.add(Objects.requireNonNull(zKey));
        keys.add(Objects.requireNonNull(entityKey));
        final Object[] args = Objects.requireNonNull(argsList.toArray(new Object[0]));
        redisTemplate.execute(script, keys, args);
        return entity;
    }

    /**
     * Atomically delete entity (ZREM + DEL via Lua).
     *
     * @param entity entity to delete
     */
    void deleteAtomic(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity to deleteAtomic must not be null");
        }
        final String id = Objects.requireNonNull(requireId(entity), "entity id must not be null").toString();
        final String zKey = requireZSetKey();
        final String entityKey = requireKey(id);
        final RedisScript<Long> script = Objects.requireNonNull(LUA_DELETE_PER_ENTITY_SCRIPT, LUA_DELETE_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL);
        final List<String> keys = new ArrayList<>(2);
        keys.add(Objects.requireNonNull(zKey));
        keys.add(Objects.requireNonNull(entityKey));
        redisTemplate.execute(script, keys, Objects.requireNonNull(id));
    }

    /**
     * Delete entity by id (ZREM + DEL via Lua).
     *
     * @param id entity id
     */
    void deleteById(TID id) {
        final String zKey = requireZSetKey();
        final String idStr = String.valueOf(id);
        final String entityKey = requireKey(idStr);
        final RedisScript<Long> script = Objects.requireNonNull(LUA_DELETE_PER_ENTITY_SCRIPT, LUA_DELETE_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL);
        final List<String> keys = new ArrayList<>(2);
        keys.add(Objects.requireNonNull(zKey));
        keys.add(Objects.requireNonNull(entityKey));
        redisTemplate.execute(script, keys, Objects.requireNonNull(idStr));
    }

    /**
     * Batch delete entities by id collection (atomic ZREM + DEL via Lua).
     *
     * @param ids entity ids
     */
    void deleteByIds(Collection<TID> ids) {
        final String zKey = Objects.requireNonNull(config.getResolvedZSetKey(), "resolvedZSetKey must not be null");
        final List<String> keys = new ArrayList<>();
        keys.add(zKey);
        final List<String> idStrings = new ArrayList<>();
        for (TID id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("id in ids must not be null");
            }
            final String s = String.valueOf(id);
            idStrings.add(s);
            keys.add(requireKey(s));
        }
        final RedisScript<Long> script = Objects.requireNonNull(LUA_DELETE_BATCH_PER_ENTITY_SCRIPT, LUA_DELETE_BATCH_PER_ENTITY_SCRIPT_MUST_NOT_BE_NULL);
        final Object[] args = Objects.requireNonNull(idStrings.toArray(new Object[0]));
        redisTemplate.execute(script, keys, args);
    }

    // ---- internal helpers ----

    private String requireZSetKey() {
        String k = config.getResolvedZSetKey();
        if (k == null || k.isEmpty()) {
            throw new IllegalStateException("resolvedZSetKey must be configured and not empty");
        }
        return k;
    }

    private String requireKey(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String prefix = config.getResolvedHashKeyPrefix();
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalStateException("resolvedHashKeyPrefix in config must not be null or empty");
        }
        return prefix + id;
    }

    private TID requireId(T entity) {
        try {
            return config.getIdExtractor().getId(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract id", e);
        }
    }
}
