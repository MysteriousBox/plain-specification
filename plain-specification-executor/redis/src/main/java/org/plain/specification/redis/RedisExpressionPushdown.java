package org.plain.specification.redis;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.redis.pushdown.NarrowPlan;
import org.plain.specification.redis.pushdown.RedisNarrowScanner;
import org.plain.specification.redis.pushdown.RedisNarrowingVisitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Read path that evaluates the pushable shadow of a specification inside Redis.
 * <p>
 * The server-side narrowing is a <em>necessary</em> condition only, so every surviving id is still
 * read as a full HASH and re-checked with the original {@link ISpecification#isSatisfiedBy(Object)}.
 * That is what makes the path equivalent to the batch scan it replaces: the narrowing can only cost
 * performance when it is imprecise, never correctness.
 * <p>
 * What is saved is transfer and materialisation: candidate entries that the narrowing rules out
 * never ship their HASH and never go through {@link EntityBuilder}. The index walk itself is still
 * a full pass — skipping it would need secondary indexes, i.e. a change to the storage layout.
 */
@Slf4j
class RedisExpressionPushdown<T, TID> {

    /** Unbounded scan. */
    static final long NO_LIMIT = -1L;

    private final RedisRepositoryConfig<T, TID> config;
    private final RedisHashMapper<T, TID> hashMapper;
    private final Function<List<String>, List<Map<Object, Object>>> hashFetcher;
    private final RedisNarrowingVisitor<T> visitor;
    private final RedisNarrowScanner scanner;

    RedisExpressionPushdown(StringRedisTemplate redisTemplate,
                            RedisRepositoryConfig<T, TID> config,
                            RedisHashMapper<T, TID> hashMapper,
                            Function<List<String>, List<Map<Object, Object>>> hashFetcher) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.hashMapper = Objects.requireNonNull(hashMapper, "hashMapper must not be null");
        this.hashFetcher = Objects.requireNonNull(hashFetcher, "hashFetcher must not be null");
        this.visitor = new RedisNarrowingVisitor<>();
        this.scanner = config.isExpressionPushdownEnabled() ? createScanner(redisTemplate, config) : null;
    }

    /**
     * A repository whose keys are not usable for a scan keeps the batch path instead of failing at
     * construction time.
     */
    private static <T, TID> RedisNarrowScanner createScanner(StringRedisTemplate redisTemplate,
                                                             RedisRepositoryConfig<T, TID> config) {
        final String zSetKey = config.getResolvedZSetKey();
        final String prefix = config.getResolvedHashKeyPrefix();
        if (zSetKey == null || zSetKey.isEmpty() || prefix == null || prefix.isEmpty()) {
            log.debug("Expression pushdown requested but zSetKey/hashKeyPrefix is unusable; batch scan stays in charge");
            return null;
        }
        return new RedisNarrowScanner(redisTemplate, zSetKey, prefix);
    }

    /**
     * @return the expression visitor used to compile plans, for {@code getVisitor()}
     */
    RedisNarrowingVisitor<T> getVisitor() {
        return visitor;
    }

    /**
     * Compile the specification, deciding whether the narrowing is worth a server-side pass.
     *
     * @param specification query specification
     * @return the plan, or {@code null} when the caller must use the batch scan
     */
    NarrowPlan planOf(ISpecification<T> specification) {
        if (scanner == null || specification == null) {
            return null;
        }
        if (!specification.getWhereExpressions().iterator().hasNext()) {
            return null;
        }
        NarrowPlan plan = NarrowPlan.trivial();
        for (IExpressionDescriptor<T> descriptor : specification.getWhereExpressions()) {
            final NarrowPlan part;
            try {
                part = descriptor.func(visitor);
            } catch (RuntimeException ex) {
                log.debug("Pushdown planning failed, falling back to batch scan: {}", ex.getMessage());
                return null;
            }
            if (part != null) {
                plan = NarrowPlan.and(plan, part);
            }
        }
        return plan.isTrivial() ? null : plan;
    }

    /**
     * Scan for matches, stopping as soon as {@code limit} of them are collected.
     * <p>
     * With a limit the survivors of one window are verified immediately so the scan can stop early.
     * Without one, survivor ids are buffered and verified in full batches: the ids are small, and
     * holding them turns several HASH round trips into one.
     *
     * @param specification query specification
     * @param plan          compiled narrowing plan
     * @param limit         maximum number of entities, or {@link #NO_LIMIT}
     * @param descending    true to walk the index high-score first
     * @return matching entities in index order
     */
    List<T> findRange(final ISpecification<T> specification, final NarrowPlan plan, final long limit,
                      boolean descending) {
        final List<T> result = new ArrayList<T>();
        scan(specification, plan, descending, new Sink<T>() {
            @Override
            public boolean add(List<T> verified) {
                for (T entity : verified) {
                    if (limit > 0 && result.size() >= limit) {
                        return false;
                    }
                    result.add(entity);
                }
                return limit <= 0 || result.size() < limit;
            }
        }, limit > 0);
        return result;
    }

    /**
     * Count matches. Every survivor still has to be verified, so the full index is walked.
     *
     * @param specification query specification
     * @param plan          compiled narrowing plan
     * @return number of matching entities
     */
    long count(ISpecification<T> specification, NarrowPlan plan) {
        final long[] matched = new long[1];
        scan(specification, plan, false, new Sink<T>() {
            @Override
            public boolean add(List<T> verified) {
                matched[0] += verified.size();
                return true;
            }
        }, false);
        return matched[0];
    }

    /**
     * @param verifyEachWindow true when the caller can stop mid-way, so a window's survivors are
     *                         verified as soon as they come back instead of being buffered
     */
    private void scan(ISpecification<T> specification, NarrowPlan plan, boolean descending,
                      Sink<T> sink, boolean verifyEachWindow) {
        final int window = windowSize();
        final long total = scanner.indexSize();
        final List<String> pending = new ArrayList<String>(window);
        long start = 0L;
        while (start < total) {
            final long end = Math.min(start + window - 1L, total - 1L);
            pending.addAll(scanner.scanWindow(plan, start, end, descending));
            start = end + 1;
            if (!pending.isEmpty() && (verifyEachWindow || pending.size() >= window)) {
                if (!flush(pending, specification, sink)) {
                    return;
                }
            }
        }
        if (!pending.isEmpty()) {
            flush(pending, specification, sink);
        }
    }

    /**
     * @return false when the consumer no longer wants more results
     */
    private boolean flush(List<String> pending, ISpecification<T> specification, Sink<T> consumer) {
        final List<T> verified = verify(pending, specification);
        pending.clear();
        return consumer.add(verified);
    }

    /** Receiving side of the scan, so findRange and count share one index walk. */
    private interface Sink<T> {
        /**
         * @param verified entities of one verified batch, in index order
         * @return false to stop the scan
         */
        boolean add(List<T> verified);
    }

    // ---- internals ----

    private List<T> verify(List<String> ids, ISpecification<T> specification) {
        final String prefix = scanner.hashKeyPrefix();
        final List<String> keys = new ArrayList<String>(ids.size());
        for (String id : ids) {
            keys.add(prefix + id);
        }
        final List<Map<Object, Object>> hashes = hashFetcher.apply(keys);
        final List<T> entities = hashMapper.buildEntities(hashes);
        return hashMapper.filterMatchingEntities(entities, specification);
    }

    private int windowSize() {
        final int window = config.getBatchSize();
        if (window <= 0) {
            throw new IllegalStateException("batchSize must be > 0");
        }
        return window;
    }
}
