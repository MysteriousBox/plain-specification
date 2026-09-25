package org.plain.specification.redis.autoconfig;

import org.plain.specification.redis.DefaultStrategies;
import org.plain.specification.redis.RedisRepositoryConfig;
import org.plain.specification.redis.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Redis 模块自动配置。
 * <p>
 * 提供全局 RedisRepositoryConfig 构建器和默认策略注入点。
 * 当 classpath 存在 StringRedisTemplate 时生效，否则静默跳过。
 * </p>
 *
 * @author Jayden.Liang
 */
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
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
    public RedisRepositoryStrategies redisRepositoryStrategies(
            RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider,
            FieldValueSerializer fieldValueSerializer,
            Serializer<Object> serializer,
            Deserializer<Object> deserializer,
            IdExtractor<Object, Object> idExtractor,
            ScoreProvider<Object> scoreProvider,
            TtlProvider<Object> ttlProvider,
            FieldExtractor<Object> fieldExtractor,
            EntityBuilder<Object> entityBuilder) {
        return new RedisRepositoryStrategies(keyPrefixProvider, fieldValueSerializer,
                serializer, deserializer, idExtractor, scoreProvider, ttlProvider,
                fieldExtractor, entityBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRepositoryConfig<Object, Object> globalRedisRepositoryConfig(
            RedisRepositoryProperties props,
            ObjectProvider<Executor> executorProvider,
            RedisRepositoryStrategies strategies) {
        Objects.requireNonNull(props, "RedisRepositoryProperties must not be null");
        RedisRepositoryConfig.Builder<Object, Object> b = new RedisRepositoryConfig.Builder<>();

        b.keyPrefixProvider(strategies.keyPrefixProvider())
                .defaultZsetName(props.getDefaultZsetName())
                .defaultTtl(props.getDefaultTtl())
                .batchSize(props.getBatchSize())
                .serializer(strategies.serializer())
                .deserializer(strategies.deserializer())
                .idExtractor(strategies.idExtractor())
                .scoreProvider(strategies.scoreProvider())
                .ttlProvider(strategies.ttlProvider())
                .fieldExtractor(strategies.fieldExtractor())
                .entityBuilder(strategies.entityBuilder())
                .fieldValueSerializer(strategies.fieldValueSerializer());
        Executor exe = executorProvider.getIfUnique();
        if (exe == null) {
            exe = ForkJoinPool.commonPool();
        }
        b.asyncExecutor(exe);
        return b.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRepositoryConfigResolver redisRepositoryConfigResolver(RedisRepositoryConfig<Object, Object> globalConfig) {
        return new RedisRepositoryConfigResolver(globalConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRepositoryFactory redisRepositoryFactory(StringRedisTemplate redisTemplate,
                                                         RedisRepositoryConfigResolver resolver) {
        Objects.requireNonNull(redisTemplate, "StringRedisTemplate must not be null");
        // use two-arg constructor for compatibility with available constructor overloads
        return new RedisRepositoryFactory(redisTemplate, resolver);
    }
}
