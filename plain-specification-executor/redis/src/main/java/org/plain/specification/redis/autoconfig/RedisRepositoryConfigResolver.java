package org.plain.specification.redis.autoconfig;

import org.plain.specification.redis.*;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

/**
 * Resolve and merge global and per-entity RedisRepositoryConfig.
 * - Uses KeyNamingUtils to compute prefix and zset key.
 * - Falls back to DefaultStrategies for missing components.
 * - Caches resolved configs by entity Class and full ZSet key when no per-entity overrides are provided.
 *
 * @author Jayden.Liang
 */
public class RedisRepositoryConfigResolver {

    private final RedisRepositoryConfig<Object, Object> globalConfig;

    // 缓存 globalConfig 相关的 prefix 计算参数，减少重复访问
    private final RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider;
    private final String defaultZsetName;

    // outer map keyed by entity Class; inner map keyed by fullZsetKey
    private final ConcurrentMap<Class<?>, ConcurrentMap<String, RedisRepositoryConfig<?, ?>>> cache = new ConcurrentHashMap<>();

    public RedisRepositoryConfigResolver(RedisRepositoryConfig<Object, Object> globalConfig) {
        this.globalConfig = Objects.requireNonNull(globalConfig, "globalConfig must not be null");
        this.keyPrefixProvider = globalConfig.getKeyPrefixProvider();
        this.defaultZsetName = globalConfig.getDefaultZSetName();
    }

    public <T, TID> RedisRepositoryConfig<T, TID> resolve(Class<T> entityClass, String zSetKey,
                                                          @Nullable RedisRepositoryConfig<T, TID> overrides) {
        Objects.requireNonNull(entityClass, "entityClass must not be null");

        // compute prefix and full zset key up front so cache key matches built config
        String prefix = KeyNamingUtils.computePrefix(keyPrefixProvider, entityClass, null);
        String fullZsetKey = KeyNamingUtils.computeZsetKey(prefix, zSetKey, defaultZsetName);

        if (overrides == null) {
            // get or create per-class inner map
            ConcurrentMap<String, RedisRepositoryConfig<?, ?>> inner = cache.computeIfAbsent(entityClass, c -> new ConcurrentHashMap<>());
            // compute once per (entityClass, fullZsetKey)
            RedisRepositoryConfig<?, ?> cached = inner.computeIfAbsent(fullZsetKey, k -> buildConfig(entityClass, zSetKey, null));
            return castConfig(cached);
        } else {
            // do not cache overrides; build a fresh merged config
            return buildConfig(entityClass, zSetKey, overrides);
        }
    }

    /**
     * Convenience overload: resolve config for an entity using default zSet key and global settings.
     */
    public <T, TID> RedisRepositoryConfig<T, TID> resolve(Class<T> entityClass) {
        return resolve(entityClass, null, null);
    }

    private <T, TID> RedisRepositoryConfig<T, TID> buildConfig(Class<T> entityClass, String zSetKey,
                                                               @Nullable RedisRepositoryConfig<T, TID> overrides) {
        // compute resolved values
        String resolvedKeyPrefix = KeyNamingUtils.computePrefix(keyPrefixProvider, entityClass, null);
        String resolvedZsetKey = KeyNamingUtils.computeZsetKey(resolvedKeyPrefix, zSetKey, defaultZsetName);

        RedisRepositoryConfig.Builder<T, TID> b = new RedisRepositoryConfig.Builder<>();

        // choose between override (if provided) and global config
        long defaultTtl = overrides != null ? overrides.getDefaultTtl() : globalConfig.getDefaultTtl();
        int batchSize = overrides != null ? overrides.getBatchSize() : globalConfig.getBatchSize();

        Serializer<T> serializer = overrides != null && overrides.getSerializer() != null
                ? overrides.getSerializer()
                : castSerializer(globalConfig.getSerializer());

        Deserializer<T> deserializer = overrides != null && overrides.getDeserializer() != null
                ? overrides.getDeserializer()
                : DefaultStrategies.jacksonDeserializer(entityClass);

        IdExtractor<T, TID> idExtractor = overrides != null && overrides.getIdExtractor() != null
                ? overrides.getIdExtractor()
                : DefaultStrategies.reflectionIdExtractor(entityClass, null);

        ScoreProvider<T> scoreProvider = overrides != null && overrides.getScoreProvider() != null
                ? overrides.getScoreProvider()
                : castScoreProvider(globalConfig.getScoreProvider());

        TtlProvider<T> ttlProvider = overrides != null && overrides.getTtlProvider() != null
                ? overrides.getTtlProvider()
                : castTtlProvider(globalConfig.getTtlProvider());

        FieldValueSerializer fieldValueSerializer = overrides != null && overrides.getFieldValueSerializer() != null
                ? overrides.getFieldValueSerializer()
                : globalConfig.getFieldValueSerializer();

        FieldExtractor<T> fieldExtractor = overrides != null && overrides.getFieldExtractor() != null
                ? overrides.getFieldExtractor()
                : castFieldExtractor(globalConfig.getFieldExtractor());

        EntityBuilder<T> entityBuilder = overrides != null && overrides.getEntityBuilder() != null
                ? overrides.getEntityBuilder()
                : castEntityBuilder(globalConfig.getEntityBuilder());

        Executor asyncExecutor = overrides != null && overrides.getAsyncExecutor() != null
                ? overrides.getAsyncExecutor()
                : globalConfig.getAsyncExecutor();

        b.resolvedZsetKey(resolvedZsetKey)
                .resolvedHashKeyPrefix(resolvedKeyPrefix)
                .defaultTtl(defaultTtl)
                .batchSize(batchSize)
                .serializer(serializer)
                .deserializer(deserializer)
                .idExtractor(idExtractor)
                .scoreProvider(scoreProvider)
                .ttlProvider(ttlProvider)
                .fieldValueSerializer(fieldValueSerializer)
                .fieldExtractor(fieldExtractor)
                .entityBuilder(entityBuilder)
                .asyncExecutor(asyncExecutor)
                .keyPrefixProvider(keyPrefixProvider)
                .defaultZsetName(defaultZsetName);

        return b.build();
    }

    // --- helper cast methods: localize unchecked casts and document why they're safe ---

    @SuppressWarnings("unchecked")
    private static <T, TID> RedisRepositoryConfig<T, TID> castConfig(RedisRepositoryConfig<?, ?> cfg) {
        // The cache is now keyed by entity Class and fullZsetKey. Values stored for a
        // given Class were constructed by buildConfig(entityClass,...), so the runtime
        // type parameters correspond to that entity. Therefore this unchecked cast is
        // safe in the current design; we keep it localized to this helper for auditing.
        return (RedisRepositoryConfig<T, TID>) cfg;
    }

    @SuppressWarnings("unchecked")
    private static <T> Serializer<T> castSerializer(Serializer<?> s) {
        return (Serializer<T>) s;
    }

    @SuppressWarnings("unchecked")
    private static <T> ScoreProvider<T> castScoreProvider(ScoreProvider<?> p) {
        return (ScoreProvider<T>) p;
    }

    @SuppressWarnings("unchecked")
    private static <T> TtlProvider<T> castTtlProvider(TtlProvider<?> p) {
        return (TtlProvider<T>) p;
    }

    @SuppressWarnings("unchecked")
    private static <T> FieldExtractor<T> castFieldExtractor(FieldExtractor<?> f) {
        return (FieldExtractor<T>) f;
    }

    @SuppressWarnings("unchecked")
    private static <T> EntityBuilder<T> castEntityBuilder(EntityBuilder<?> b) {
        return (EntityBuilder<T>) b;
    }
}
