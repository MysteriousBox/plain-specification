package org.plain.specification.redis.autoconfig;

import lombok.RequiredArgsConstructor;
import org.plain.specification.redis.*;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 简单工厂：基于全局配置构建 GenericRedisRepository 的便捷工厂。
 * 实际项目中可以扩展为 FactoryBean 或基于接口扫描自动创建 Bean。
 *
 * 使用 Lombok 的 @RequiredArgsConstructor 简化构造器
 *
 * @author Jayden.Liang
 */
@RequiredArgsConstructor
public class RedisRepositoryFactory {

    private final StringRedisTemplate redisTemplate;
    private final RedisRepositoryConfig<Object, Object> globalConfig;

    @SuppressWarnings("unchecked")
    public <T, TID> GenericRedisRepository<T, TID> create(Class<T> entityClass, String zSetKey) {
        RedisRepositoryConfig.Builder<T, TID> b = new RedisRepositoryConfig.Builder<>();

        // compute prefix using centralized util
        String prefix = KeyNamingUtils.computePrefix(globalConfig.getKeyPrefixProvider(), entityClass, globalConfig.getKeyPrefix());
        String fullZsetKey = KeyNamingUtils.computeZsetKey(prefix, zSetKey, globalConfig.getDefaultZSetName());

        b.zSetKey(fullZsetKey)
                .keyPrefix(prefix)
                .defaultTtl(globalConfig.getDefaultTtl())
                .batchSize(globalConfig.getBatchSize())
                // use entityClass-specific deserializer and idExtractor to match types
                .serializer((org.plain.specification.redis.Serializer<T>) globalConfig.getSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(entityClass))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(entityClass, null))
                .scoreProvider((ScoreProvider<T>) globalConfig.getScoreProvider())
                .ttlProvider((TtlProvider<T>) globalConfig.getTtlProvider());
        return new GenericRedisRepository<>(redisTemplate, b.build());
    }
}
