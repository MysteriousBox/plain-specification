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

    /**
     * Provider used to compute key prefixes from the global Redis repository configuration.
     */
    private final RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider;

    /**
     * Default ZSet name supplied by the global Redis repository configuration.
     */
    private final String defaultZsetName;

    /**
     * Cache keyed by entity class and full ZSet key for resolved configuration.
     */
    private final ConcurrentMap<Class<?>, ConcurrentMap<String, RedisRepositoryConfig<?, ?>>> cache = new ConcurrentHashMap<>();

    public RedisRepositoryConfigResolver(RedisRepositoryConfig<Object, Object> globalConfig) {
        this.globalConfig = Objects.requireNonNull(globalConfig, "globalConfig must not be null");
        this.keyPrefixProvider = globalConfig.getKeyPrefixProvider();
        this.defaultZsetName = globalConfig.getDefaultZSetName();
    }

    public <T, I> RedisRepositoryConfig<T, I> resolve(Class<T> entityClass, String zSetKey,
                                                          @Nullable RedisRepositoryConfig<T, I> overrides) {
        Objects.requireNonNull(entityClass, "entityClass must not be null");

        // compute prefix and full zset key up front so cache key matches built config
        String prefix = KeyNamingUtils.computePrefix(keyPrefixProvider, entityClass, null);
        String fullZsetKey = KeyNamingUtils.computeZsetKey(prefix, zSetKey, defaultZsetName);

        if (overrides == null) {
            // get or create per-class inner map
            ConcurrentMap<String, RedisRepositoryConfig<?, ?>> inner = cache.computeIfAbsent(entityClass, c -> new ConcurrentHashMap<>(cache.size() / 4));
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
    public <T, I> RedisRepositoryConfig<T, I> resolve(Class<T> entityClass) {
        return resolve(entityClass, null, null);
    }

    private <T, I> RedisRepositoryConfig<T, I> buildConfig(Class<T> entityClass, String zSetKey,
                                                               @Nullable RedisRepositoryConfig<T, I> overrides) {
        String resolvedKeyPrefix = KeyNamingUtils.computePrefix(keyPrefixProvider, entityClass, null);
        String resolvedZsetKey = KeyNamingUtils.computeZsetKey(resolvedKeyPrefix, zSetKey, defaultZsetName);

        RedisRepositoryConfig.Builder<T, I> b = new RedisRepositoryConfig.Builder<>();
        RedisRepositoryConfig<?, ?> effectiveConfig = overrides != null ? overrides : globalConfig;

        long defaultTtl = effectiveConfig.getDefaultTtl();
        int batchSize = effectiveConfig.getBatchSize();

        Serializer<T> serializer = chooseSerializer(overrides);
        Deserializer<T> deserializer = chooseDeserializer(overrides, entityClass);
        IdExtractor<T, I> idExtractor = chooseIdExtractor(overrides, entityClass);
        ScoreProvider<T> scoreProvider = chooseScoreProvider(overrides);
        TtlProvider<T> ttlProvider = chooseTtlProvider(overrides);
        FieldValueSerializer fieldValueSerializer = chooseFieldValueSerializer(overrides);
        FieldExtractor<T> fieldExtractor = chooseFieldExtractor(overrides);
        EntityBuilder<T> entityBuilder = chooseEntityBuilder(overrides);
        Executor asyncExecutor = chooseAsyncExecutor(overrides);

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

    private <T, I> Serializer<T> chooseSerializer(RedisRepositoryConfig<T, I> overrides) {
        return overrides != null && overrides.getSerializer() != null
                ? overrides.getSerializer()
                : castSerializer(globalConfig.getSerializer());
    }

    private <T, I> Deserializer<T> chooseDeserializer(RedisRepositoryConfig<T, I> overrides, Class<T> entityClass) {
        return overrides != null && overrides.getDeserializer() != null
                ? overrides.getDeserializer()
                : DefaultStrategies.jacksonDeserializer(entityClass);
    }

    private <T, I> IdExtractor<T, I> chooseIdExtractor(RedisRepositoryConfig<T, I> overrides, Class<T> entityClass) {
        return overrides != null && overrides.getIdExtractor() != null
                ? overrides.getIdExtractor()
                : DefaultStrategies.reflectionIdExtractor(entityClass, null);
    }

    private <T, I> ScoreProvider<T> chooseScoreProvider(RedisRepositoryConfig<T, I> overrides) {
        return overrides != null && overrides.getScoreProvider() != null
                ? overrides.getScoreProvider()
                : castScoreProvider(globalConfig.getScoreProvider());
    }

    private <T, I> TtlProvider<T> chooseTtlProvider(RedisRepositoryConfig<T, I> overrides) {
        return overrides != null && overrides.getTtlProvider() != null
                ? overrides.getTtlProvider()
                : castTtlProvider(globalConfig.getTtlProvider());
    }

    private <T, I> FieldValueSerializer chooseFieldValueSerializer(RedisRepositoryConfig<T, I> overrides) {
        return overrides != null && overrides.getFieldValueSerializer() != null
                ? overrides.getFieldValueSerializer()
                : globalConfig.getFieldValueSerializer();
    }

    private <T, I> FieldExtractor<T> chooseFieldExtractor(RedisRepositoryConfig<T, I> overrides) {
        return overrides != null && overrides.getFieldExtractor() != null
                ? overrides.getFieldExtractor()
                : castFieldExtractor(globalConfig.getFieldExtractor());
    }

    private <T, I> EntityBuilder<T> chooseEntityBuilder(RedisRepositoryConfig<T, I> overrides) {
        return overrides != null && overrides.getEntityBuilder() != null
                ? overrides.getEntityBuilder()
                : castEntityBuilder(globalConfig.getEntityBuilder());
    }

    private <T, I> Executor chooseAsyncExecutor(RedisRepositoryConfig<T, I> overrides) {
        return overrides != null && overrides.getAsyncExecutor() != null
                ? overrides.getAsyncExecutor()
                : globalConfig.getAsyncExecutor();
    }

    // --- helper cast methods: localize unchecked casts and document why they're safe ---

    private static <T, I> RedisRepositoryConfig<T, I> castConfig(RedisRepositoryConfig<?, ?> cfg) {
        // The cache is now keyed by entity Class and fullZsetKey. Values stored for a
        // given Class were constructed by buildConfig(entityClass,...), so the runtime
        // type parameters correspond to that entity. Therefore this unchecked cast is
        // safe in the current design; we keep it localized to this helper for auditing.
        return (RedisRepositoryConfig<T, I>) cfg;
    }

    private static <T> Serializer<T> castSerializer(Serializer<?> s) {
        return (Serializer<T>) s;
    }

    private static <T> ScoreProvider<T> castScoreProvider(ScoreProvider<?> p) {
        return (ScoreProvider<T>) p;
    }

    private static <T> TtlProvider<T> castTtlProvider(TtlProvider<?> p) {
        return (TtlProvider<T>) p;
    }

    private static <T> FieldExtractor<T> castFieldExtractor(FieldExtractor<?> f) {
        return (FieldExtractor<T>) f;
    }

    private static <T> EntityBuilder<T> castEntityBuilder(EntityBuilder<?> b) {
        return (EntityBuilder<T>) b;
    }
}
