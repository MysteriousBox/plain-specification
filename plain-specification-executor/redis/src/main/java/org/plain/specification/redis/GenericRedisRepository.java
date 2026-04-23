package org.plain.specification.redis;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 基于策略的通用 Redis 仓储实现。子类可以直接继承或使用工厂构造。
 *
 * @param <T>   实体类型
 * @param <TID> 主键类型
 * @author Jayden.Liang
 * @since 1.0
 */
public class GenericRedisRepository<T, TID> extends RedisBaseRepository<T, TID> {

    /**
     * 使用现成的配置构造仓储
     *
     * @param redisTemplate RedisTemplate 实例
     * @param cfg           完整配置，不能为 null
     */
    public GenericRedisRepository(StringRedisTemplate redisTemplate, RedisRepositoryConfig<T, TID> cfg) {
        super(redisTemplate, cfg);
    }

    /**
     * 便捷构造器：基于实体类型和 zSetKey 构建默认配置（使用项目默认的 JsonUtil + 反射 id 提取）
     *
     * @param redisTemplate RedisTemplate 实例
     * @param entityClass   实体 Class
     * @param zSetKey       ZSet 的 key
     */
    public GenericRedisRepository(StringRedisTemplate redisTemplate, Class<T> entityClass, String zSetKey) {
        this(redisTemplate, buildDefaultConfig(entityClass, zSetKey));
    }

    private static <T, TID> RedisRepositoryConfig<T, TID> buildDefaultConfig(Class<T> entityClass, String zSetKey) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass must not be null");
        }
        if (zSetKey == null || zSetKey.isEmpty()) {
            throw new IllegalArgumentException("zSetKey must not be null or empty");
        }
        RedisRepositoryConfig.Builder<T, TID> b = new RedisRepositoryConfig.Builder<>();
        b.resolvedZsetKey(zSetKey)
         .resolvedHashKeyPrefix("")
         .serializer(DefaultStrategies.jacksonSerializer())
         .deserializer(DefaultStrategies.jacksonDeserializer(entityClass))
         .idExtractor(DefaultStrategies.reflectionIdExtractor(entityClass, null))
         .scoreProvider(DefaultStrategies.defaultScoreProvider())
         .ttlProvider(DefaultStrategies.defaultTtlProvider())
         .fieldExtractor(DefaultStrategies.reflectionFieldExtractor())
         .entityBuilder(DefaultStrategies.reflectionEntityBuilder(entityClass));
        return b.build();
    }


}
