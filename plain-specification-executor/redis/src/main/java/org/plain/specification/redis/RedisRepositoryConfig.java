package org.plain.specification.redis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * 仓储配置（不可变），通过 Builder 构建。
 * <p>包含序列化/反序列化、id 提取、score、ttl 等策略接口，和常用的默认值。</p>
 *
 * @param <T>   实体类型
 * @param <TID> 主键类型
 * @author Jayden.Liang
 * @since 1.0
 */
@Getter
@EqualsAndHashCode
@ToString
@SuppressWarnings("unused")
public final class RedisRepositoryConfig<T, TID> {
    private final long defaultTtl;
    private final int batchSize;
    private final String keyPrefix;
    private final String zSetKey;
    private final Serializer<T> serializer;
    private final Deserializer<T> deserializer;
    private final IdExtractor<T, TID> idExtractor;
    private final ScoreProvider<T> scoreProvider;
    private final TtlProvider<T> ttlProvider;
    private final Executor asyncExecutor;
    private final FieldExtractor<T> fieldExtractor;
    private final FieldValueSerializer fieldValueSerializer;
    private final EntityBuilder<T> entityBuilder;
    private final String hashKey;
    // new provider strategy for computing per-entity key prefix
    private final KeyPrefixProvider keyPrefixProvider;
    private final String defaultZSetName;

    private RedisRepositoryConfig(Builder<T, TID> b) {
        this.defaultTtl = b.defaultTtl;
        this.batchSize = b.batchSize;
        this.keyPrefix = b.keyPrefix;
        this.zSetKey = b.zSetKey;
        this.serializer = b.serializer;
        this.deserializer = b.deserializer;
        this.idExtractor = b.idExtractor;
        this.scoreProvider = b.scoreProvider;
        this.ttlProvider = b.ttlProvider;
        this.asyncExecutor = b.asyncExecutor;
        this.fieldExtractor = b.fieldExtractor;
        this.fieldValueSerializer = b.fieldValueSerializer;
        this.entityBuilder = b.entityBuilder;
        this.hashKey = b.hashKey;
        this.keyPrefixProvider = b.keyPrefixProvider;
        this.defaultZSetName = b.defaultZsetName;
    }

    /**
     * Default ZSet name used when no explicit zSetKey is provided.
     */
    @SuppressWarnings("unused")
    public String getDefaultZSetName() {
        return this.defaultZSetName;
    }

    /**
     * Key prefix provider strategy: given an entity class and the configured global prefix,
     * produce the actual per-entity setex prefix (should include trailing separator if desired).
     */
    @FunctionalInterface
    public interface KeyPrefixProvider {
        String provide(Class<?> entityClass, String globalPrefix);
    }

    /**
     * 创建 Builder 的入口方法。
     * <p>对外公开作为库的工厂方法（可能由外部模块使用），因此保留为 public。</p>
     */
    @SuppressWarnings("unused")
    public static <T, TID> Builder<T, TID> builder() {
        return new Builder<>();
    }


    /**
     * Builder 用于逐项设置配置并最终构建不可变的 {@link RedisRepositoryConfig}。
     * <p>此类采用链式调用风格，故部分 IDE 可能提示“返回值未被使用”，这是正常的。
     * 可以在外部以方法链方式构建配置，例如：
     * <pre>
     * RedisRepositoryConfig.builder()
     *     .zSetKey("users:zset")
     *     .serializer(...)
     *     .deserializer(...)
     *     .build();
     * </pre>
     * </p>
     */
    public static class Builder<T, TID> {
        private long defaultTtl = 60 * 60; // 1 hour
        private int batchSize = 100;
        private String keyPrefix = "entity:";
        private String zSetKey;
        private Serializer<T> serializer;
        private Deserializer<T> deserializer;
        private IdExtractor<T, TID> idExtractor;
        private ScoreProvider<T> scoreProvider = (e) -> 0d;
        private TtlProvider<T> ttlProvider = (e) -> null;
        private Executor asyncExecutor = null;
        private FieldExtractor<T> fieldExtractor;
        private FieldValueSerializer fieldValueSerializer;
        private EntityBuilder<T> entityBuilder;
        private String hashKey;
        // default provider: append entity simple name to global prefix, safe fallbacks
        private KeyPrefixProvider keyPrefixProvider = (clazz, globalPrefix) -> {
            String gp = globalPrefix == null ? "" : globalPrefix;
            final String entityName;
            if (clazz == null) {
                entityName = "Entity";
            } else {
                String simple = clazz.getSimpleName();
                entityName = simple.isEmpty() ? clazz.getName().replace('.', '_') : simple;
            }
            StringBuilder sb = new StringBuilder();
            if (!gp.isEmpty()) {
                sb.append(gp);
                if (!gp.endsWith(":")) {
                    sb.append(":");
                }
            }
            sb.append(entityName).append(":");
            return sb.toString();
        };
        private String defaultZsetName = "index";

        /**
         * 设置默认 TTL（秒），必须大于 0。
         *
         * @param seconds 秒数，must > 0
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> defaultTtl(long seconds) {
            if (seconds <= 0) {
                throw new IllegalArgumentException("defaultTtl must be > 0");
            }
            this.defaultTtl = seconds;
            return this;
        }

        /**
         * 设置批量大小，必须大于 0。
         *
         * @param size 批量大小
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> batchSize(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("batchSize must be > 0");
            }
            this.batchSize = size;
            return this;
        }

        /**
         * 设置 setex key 前缀，不能为空。
         *
         * @param prefix 前缀
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> keyPrefix(String prefix) {
            this.keyPrefix = Objects.requireNonNull(prefix, "keyPrefix must not be null");
            return this;
        }

        /**
         * 指定 ZSet 的 key（必需）。
         *
         * @param zSetKey zset key
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> zSetKey(String zSetKey) {
            this.zSetKey = Objects.requireNonNull(zSetKey, "zSetKey must not be null");
            if (zSetKey.isEmpty()) {
                throw new IllegalArgumentException("zSetKey must not be empty");
            }
            return this;
        }

        /**
         * 指定序列化器（必需）。
         *
         * @param s 序列化器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> serializer(Serializer<T> s) {
            this.serializer = Objects.requireNonNull(s, "serializer must not be null");
            return this;
        }

        /**
         * 指定反序列化器（必需）。
         *
         * @param d 反序列化器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> deserializer(Deserializer<T> d) {
            this.deserializer = Objects.requireNonNull(d, "deserializer must not be null");
            return this;
        }

        /**
         * 指定 ID 提取器（必需）。
         *
         * @param idExtractor id 提取器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> idExtractor(IdExtractor<T, TID> idExtractor) {
            this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor must not be null");
            return this;
        }

        /**
         * 指定 score 提供器，默认为 0。
         *
         * @param sp score 提供器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> scoreProvider(ScoreProvider<T> sp) {
            this.scoreProvider = Objects.requireNonNull(sp, "scoreProvider must not be null");
            return this;
        }

        /**
         * 指定 TTL 提供器，返回 null 则使用默认 TTL。
         *
         * @param tp ttl 提供器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> ttlProvider(TtlProvider<T> tp) {
            this.ttlProvider = Objects.requireNonNull(tp, "ttlProvider must not be null");
            return this;
        }

        /**
         * 可选的异步执行器。
         *
         * @param executor 异步执行器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> asyncExecutor(Executor executor) {
            this.asyncExecutor = executor;
            return this;
        }

        /**
         * 指定字段提取器（可选）。
         *
         * @param fieldExtractor 字段提取器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> fieldExtractor(FieldExtractor<T> fieldExtractor) {
            this.fieldExtractor = Objects.requireNonNull(fieldExtractor, "fieldExtractor must not be null");
            return this;
        }

        /**
         * 指定实体构建器（可选）。
         *
         * @param entityBuilder 实体构建器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> entityBuilder(EntityBuilder<T> entityBuilder) {
            this.entityBuilder = Objects.requireNonNull(entityBuilder, "entityBuilder must not be null");
            return this;
        }

        /**
         * 可选：为单个字段值指定自定义序列化器（用于写入 per-entity HASH）。
         * 如果未配置，仓储将使用默认的 JsonUtil 序列化策略。
         *
         * @param serializer 字段值序列化器
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> fieldValueSerializer(FieldValueSerializer serializer) {
            this.fieldValueSerializer = Objects.requireNonNull(serializer, "fieldValueSerializer must not be null");
            return this;
        }

        /**
         * 指定用于存放实体 payload 的单个 HASH 的 key（必需）。
         * @param hashKey hash key
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> hashKey(String hashKey) {
            this.hashKey = Objects.requireNonNull(hashKey, "hashKey must not be null");
            return this;
        }

        /**
         * 指定自定义 KeyPrefixProvider，用于在构建 repository 时生成 per-entity setex key prefix。
         * 如果未设置，Builder 使用默认策略：globalPrefix + EntitySimpleName + ':'
         *
         * @param provider provider 实现
         * @return builder
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> keyPrefixProvider(KeyPrefixProvider provider) {
            this.keyPrefixProvider = Objects.requireNonNull(provider, "keyPrefixProvider must not be null");
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder<T, TID> defaultZsetName(String name) {
            this.defaultZsetName = Objects.requireNonNull(name, "defaultZSetName must not be null");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("defaultZSetName must not be empty");
            }
            return this;
        }

        /**
         * 构建不可变的 {@link RedisRepositoryConfig} 实例。
         *
         * @return 不可变配置实例
         */
        public RedisRepositoryConfig<T, TID> build() {
             Objects.requireNonNull(zSetKey, "zSetKey must be provided");
             Objects.requireNonNull(serializer, "serializer must be provided");
             Objects.requireNonNull(deserializer, "deserializer must be provided");
             Objects.requireNonNull(idExtractor, "idExtractor must be provided");
             // For HASH per-entity storage we require FieldExtractor and EntityBuilder to map fields <-> entity
             Objects.requireNonNull(fieldExtractor, "fieldExtractor must be provided for per-entity hash storage");
             Objects.requireNonNull(entityBuilder, "entityBuilder must be provided for per-entity hash storage");
             if (batchSize <= 0) {
                 throw new IllegalArgumentException("batchSize must be > 0");
             }
             if (defaultTtl <= 0) {
                 throw new IllegalArgumentException("defaultTtl must be > 0");
             }
             return new RedisRepositoryConfig<>(this);
         }
     }
 }
