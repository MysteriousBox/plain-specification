package org.plain.specification.redis.autoconfig;

import org.plain.specification.redis.DefaultStrategies;
import org.plain.specification.redis.RedisRepositoryConfig;
import org.plain.specification.redis.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 自动装配：提供全局 RedisRepositoryConfig 构建器和默认策略注入点
 *
 * 使用本模块的 RedisRepositoryFactory（避免与 Spring Data 的同名类冲突）
 *
 * @author Jayden.Liang
 */
@Configuration
@EnableConfigurationProperties(RedisRepositoryProperties.class)
public class PlainRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantProvider tenantProvider() {
        // 默认实现返回 null，业务可自定义覆盖
        return () -> null;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider(RedisRepositoryProperties props, TenantProvider tenantProvider) {
        return DefaultStrategies.enterpriseKeyPrefixProvider(props.getEnv(), tenantProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public FieldValueSerializer fieldValueSerializer() {
        return DefaultStrategies.defaultFieldValueSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public Serializer<Object> serializer() {
        return DefaultStrategies.jacksonSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public Deserializer<Object> deserializer() {
        return DefaultStrategies.jacksonDeserializer(Object.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdExtractor<Object, Object> idExtractor() {
        return DefaultStrategies.reflectionIdExtractor(Object.class, null);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScoreProvider<Object> scoreProvider() {
        return DefaultStrategies.genericScoreProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public TtlProvider<Object> ttlProvider() {
        return DefaultStrategies.genericTtlProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public FieldExtractor<Object> fieldExtractor() {
        return DefaultStrategies.reflectionFieldExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityBuilder<Object> entityBuilder() {
        return DefaultStrategies.reflectionEntityBuilder(Object.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRepositoryConfig<Object, Object> globalRedisRepositoryConfig(RedisRepositoryProperties props,
                                                                                 ObjectProvider<Executor> executorProvider,
                                                                                 RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider,
                                                                                 FieldValueSerializer fieldValueSerializer,
                                                                                 Serializer<Object> serializer,
                                                                                 Deserializer<Object> deserializer,
                                                                                 IdExtractor<Object, Object> idExtractor,
                                                                                 ScoreProvider<Object> scoreProvider,
                                                                                 TtlProvider<Object> ttlProvider,
                                                                                 FieldExtractor<Object> fieldExtractor,
                                                                                 EntityBuilder<Object> entityBuilder) {
        Objects.requireNonNull(props, "RedisRepositoryProperties must not be null");
        RedisRepositoryConfig.Builder<Object, Object> b = new RedisRepositoryConfig.Builder<>();

        b.keyPrefix(props.getKeyPrefix())
                .keyPrefixProvider(keyPrefixProvider)
                .fieldValueSerializer(fieldValueSerializer)
                .defaultZsetName(props.getDefaultZsetName())
                .defaultTtl(props.getDefaultTtl())
                .batchSize(props.getBatchSize())
                .serializer(serializer)
                .deserializer(deserializer)
                .idExtractor(idExtractor)
                .scoreProvider(scoreProvider)
                .ttlProvider(ttlProvider)
                .fieldExtractor(fieldExtractor)
                .entityBuilder(entityBuilder);
        Executor exe = executorProvider.getIfAvailable(ForkJoinPool::commonPool);
        b.asyncExecutor(exe);
        return b.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRepositoryFactory redisRepositoryFactory(StringRedisTemplate redisTemplate,
                                                         RedisRepositoryConfig<Object, Object> globalConfig) {
        Objects.requireNonNull(redisTemplate, "StringRedisTemplate must not be null");
        Objects.requireNonNull(globalConfig, "globalConfig must not be null");
        return new RedisRepositoryFactory(redisTemplate, globalConfig);
    }
}
