package org.plain.specification.redis;

import org.plain.specification.core.ISpecification;
import org.plain.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

/**
 * Helper class for Redis HASH read/write mapping operations.
 *
 * <p>Extracted from {@link BaseRedisRepository} to reduce its size.
 * Handles fetching hash entries, building entities, filtering, and field value serialization.</p>
 *
 * @param <T>   entity type
 * @param <TID> entity primary key type
 * @author Jayden.Liang
 * @since 1.0
 */
@Slf4j
class RedisHashMapper<T, TID> {

    private final StringRedisTemplate redisTemplate;
    private final RedisRepositoryConfig<T, TID> config;

    RedisHashMapper(StringRedisTemplate redisTemplate, RedisRepositoryConfig<T, TID> config) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    /**
     * Batch-read HASH entries for the given keys (pipeline).
     *
     * @param keyList entity key list
     * @return list of field maps (null for missing keys)
     */
    List<Map<Object, Object>> fetchHashes(List<String> keyList) {
        if (keyList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> results = redisTemplate.executePipelined(
            new org.springframework.data.redis.core.SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> Object execute(org.springframework.data.redis.core.RedisOperations<K, V> operations) {
                    for (String k : keyList) {
                        operations.opsForHash().entries((K) k);
                    }
                    return null;
                }
            });
        List<Map<Object, Object>> list = new ArrayList<>(results.size());
        for (Object result : results) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> m = (Map<Object, Object>) result;
            list.add(m == null || m.isEmpty() ? null : m);
        }
        return list;
    }

    /**
     * Read a single HASH and convert to entity.
     *
     * @param redisKey HASH key
     * @param builder  entity builder
     * @return entity or null
     */
    T mapHashToEntity(String redisKey, EntityBuilder<T> builder) {
        final String actualRedisKey = Objects.requireNonNull(redisKey, "redisKey must not be null");
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(actualRedisKey);
        if (fields.isEmpty()) {
            return null;
        }
        try {
            return builder.buildEntity(fields);
        } catch (Exception ex) {
            log.warn("Failed to build entity from hash fields for key: {}, {}", redisKey, ex.getMessage());
            return null;
        }
    }

    /**
     * Build entities from a list of hash field maps.
     *
     * @param hashes list of field maps
     * @return list of successfully built entities
     */
    List<T> buildEntities(List<Map<Object, Object>> hashes) {
        final EntityBuilder<T> builder = config.getEntityBuilder();
        final List<T> results = new ArrayList<>(hashes.size());
        for (Map<Object, Object> fields : hashes) {
            if (fields == null) {
                continue;
            }
            try {
                final T entity = builder.buildEntity(fields);
                if (entity != null) {
                    results.add(entity);
                }
            } catch (Exception ex) {
                log.warn("Failed to build entity: {}", ex.getMessage());
            }
        }
        return results;
    }

    /**
     * Filter entities by specification.
     *
     * @param entities      candidate entities
     * @param specification filter specification
     * @return matching entities
     */
    List<T> filterMatchingEntities(List<T> entities, ISpecification<T> specification) {
        final List<T> matches = new ArrayList<>();
        for (T entity : entities) {
            if (Boolean.TRUE.equals(specification.isSatisfiedBy(entity))) {
                matches.add(entity);
            }
        }
        return matches;
    }

    /**
     * Serialize a single field value for HASH storage.
     *
     * @param value field value
     * @return serialized string
     */
    String serializeFieldValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return String.valueOf(value);
        }
        // Prefer a configured FieldValueSerializer if provided
        final FieldValueSerializer fvs = config.getFieldValueSerializer();
        if (fvs != null) {
            try {
                String s = fvs.serialize(value);
                if (s != null) {
                    return s;
                }
            } catch (Exception ex) {
                log.warn("Failed to serialize field value with FieldValueSerializer: {}", ex.getMessage());
                // fallback to default JsonUtil below
            }
        }
        try {
            return JsonUtil.serialize(value);
        } catch (Exception ex) {
            log.warn("Failed to serialize field value with JsonUtil: {}", ex.getMessage());
            // Last resort: use toString()
            return String.valueOf(value);
        }
    }
}
