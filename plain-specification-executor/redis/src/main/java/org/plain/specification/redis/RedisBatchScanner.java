package org.plain.specification.redis;

import org.plain.specification.core.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class for batch-scanning Redis ZSET ranges and filtering entities in memory.
 *
 * <p>Extracted from {@link BaseRedisRepository} to reduce its size.
 * Contains the batch-scan implementations for findOne, findRange, and count operations.</p>
 *
 * @param <T>   entity type
 * @param <TID> entity primary key type
 * @author Jayden.Liang
 * @since 1.0
 */
@Slf4j
class RedisBatchScanner<T, TID> {

    private static final String ERROR_ID_FROM_ZSET_RANGE_MUST_NOT_BE_NULL = "id from ZSet range must not be null";

    private final StringRedisTemplate redisTemplate;
    private final RedisRepositoryConfig<T, TID> config;
    private final RedisHashMapper<T, TID> hashMapper;
    /** Overridable hash fetcher (delegates to BaseRedisRepository.fetchHashes for test compatibility). */
    private final Function<List<String>, List<Map<Object, Object>>> hashFetcher;

    RedisBatchScanner(StringRedisTemplate redisTemplate, RedisRepositoryConfig<T, TID> config,
                      RedisHashMapper<T, TID> hashMapper,
                      Function<List<String>, List<Map<Object, Object>>> hashFetcher) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.hashMapper = Objects.requireNonNull(hashMapper, "hashMapper must not be null");
        this.hashFetcher = Objects.requireNonNull(hashFetcher, "hashFetcher must not be null");
    }

    /**
     * Check whether the specification contains WHERE filter expressions.
     */
    boolean hasWhereFilters(ISpecification<T> specification) {
        return specification.getWhereExpressions().iterator().hasNext();
    }

    /**
     * Validate specification and batch configuration.
     */
    void validateSpecificationAndBatch(ISpecification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("specification must not be null");
        }
        if (config.getBatchSize() <= 0) {
            throw new IllegalStateException("batchSize must be > 0");
        }
    }

    /**
     * Calculate the end index for the next batch fetch.
     */
    long calculateFetchEnd(long requestedEnd, long currentStart) {
        return requestedEnd == -1 ? currentStart + config.getBatchSize() - 1 : Math.min(requestedEnd, currentStart + config.getBatchSize() - 1);
    }

    /**
     * Determine whether the batch scan loop should stop.
     */
    boolean shouldStopFetching(long requestedEnd, long fetchEnd, int fetchedSize) {
        if (requestedEnd != -1 && fetchEnd >= requestedEnd) {
            return true;
        }
        return requestedEnd == -1 && fetchedSize < config.getBatchSize();
    }

    /**
     * Find a single entity matching the specification by scanning in batches.
     *
     * @param specification filter specification
     * @return first matching entity, or null
     */
    T findOneWithBatch(ISpecification<T> specification) {
        validateSpecificationAndBatch(specification);

        long start = 0L;
        long end = config.getBatchSize() - 1L;
        final String zKey = requireZSetKey();

        while (true) {
            final Set<?> ids = redisTemplate.opsForZSet().range(zKey, start, end);
            if (ids == null || ids.isEmpty()) {
                return null;
            }

            final List<String> keyList = mapIdsToKeys(ids);
            final List<Map<Object, Object>> hashes = hashFetcher.apply(keyList);
            T found = hashMapper.filterMatchingEntities(hashMapper.buildEntities(hashes), specification).stream()
                    .findFirst()
                    .orElse(null);
            if (found != null) {
                return found;
            }

            start += config.getBatchSize();
            end += config.getBatchSize();
        }
    }

    /**
     * Find all entities matching the specification by scanning in batches.
     *
     * @param specification filter specification
     * @param start         start index in ZSET
     * @param end           end index in ZSET (-1 for unbounded)
     * @return matching entities
     */
    Collection<T> findRangeWithBatch(ISpecification<T> specification, long start, long end) {
        validateSpecificationAndBatch(specification);

        final List<T> results = new ArrayList<>();
        long currentStart = start;
        final String zKey = requireZSetKey();

        boolean finished = false;
        while (!finished) {
            final long fetchEnd = calculateFetchEnd(end, currentStart);
            final Set<?> ids = redisTemplate.opsForZSet().range(zKey, currentStart, fetchEnd);
            if (ids == null || ids.isEmpty()) {
                finished = true;
            } else {
                final List<String> keyList = mapIdsToKeys(ids);
                final List<Map<Object, Object>> hashes = hashFetcher.apply(keyList);
                results.addAll(hashMapper.filterMatchingEntities(hashMapper.buildEntities(hashes), specification));

                if (shouldStopFetching(end, fetchEnd, ids.size())) {
                    finished = true;
                } else {
                    currentStart += config.getBatchSize();
                }
            }
        }
        return results;
    }

    /**
     * Optimized count: uses ZCARD when no WHERE filters, otherwise batch-scans.
     *
     * @param specification filter specification
     * @return count of matching entities
     */
    long countOptimized(ISpecification<T> specification) {
        if (!hasWhereFilters(specification)) {
            final Long size = redisTemplate.opsForZSet().size(requireZSetKey());
            return size == null ? 0L : size;
        }

        if (config.getBatchSize() <= 0) {
            throw new IllegalStateException("batchSize must be > 0");
        }

        final String zKey = requireZSetKey();
        long start = 0L;
        long end = config.getBatchSize() - 1L;
        long result = 0L;

        while (true) {
            final Set<?> ids = redisTemplate.opsForZSet().range(zKey, start, end);
            if (ids == null || ids.isEmpty()) {
                return result;
            }
            final List<String> keyList = mapIdsToKeys(ids);
            result += countMatchingEntities(keyList, specification);
            start += config.getBatchSize();
            end += config.getBatchSize();
        }
    }

    /**
     * Count entities matching the specification within the given key list.
     * Extracted from countOptimized to reduce nesting.
     *
     * @param keyList       entity keys to check
     * @param specification filter specification
     * @return number of matching entities
     */
    long countMatchingEntities(List<String> keyList, ISpecification<T> specification) {
        final EntityBuilder<T> builder = config.getEntityBuilder();
        long matched = 0L;
        for (String key : keyList) {
            T t = hashMapper.mapHashToEntity(key, builder);
            if (t != null && specification.isSatisfiedBy(t)) {
                matched++;
            }
        }
        return matched;
    }

    // ---- internal helpers ----

    private List<String> mapIdsToKeys(Set<?> ids) {
        return ids.stream()
                .map(item -> Objects.requireNonNull(item, ERROR_ID_FROM_ZSET_RANGE_MUST_NOT_BE_NULL))
                .map(item -> {
                    String itemId = Objects.requireNonNull(item.toString(), ERROR_ID_FROM_ZSET_RANGE_MUST_NOT_BE_NULL);
                    return requireKey(itemId);
                })
                .collect(Collectors.toList());
    }

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
}
