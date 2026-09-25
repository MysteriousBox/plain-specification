package org.plain.specification.redis.autoconfig;

import org.plain.specification.redis.*;

/**
 * Aggregates all Redis repository strategy beans for injection into config assembly.
 *
 * @author Jayden.Liang
 * @since 1.0
 */
class RedisRepositoryStrategies {

    private final RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider;
    private final FieldValueSerializer fieldValueSerializer;
    private final Serializer<Object> serializer;
    private final Deserializer<Object> deserializer;
    private final IdExtractor<Object, Object> idExtractor;
    private final ScoreProvider<Object> scoreProvider;
    private final TtlProvider<Object> ttlProvider;
    private final FieldExtractor<Object> fieldExtractor;
    private final EntityBuilder<Object> entityBuilder;

    RedisRepositoryStrategies(RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider,
                              FieldValueSerializer fieldValueSerializer,
                              Serializer<Object> serializer,
                              Deserializer<Object> deserializer,
                              IdExtractor<Object, Object> idExtractor,
                              ScoreProvider<Object> scoreProvider,
                              TtlProvider<Object> ttlProvider,
                              FieldExtractor<Object> fieldExtractor,
                              EntityBuilder<Object> entityBuilder) {
        this.keyPrefixProvider = keyPrefixProvider;
        this.fieldValueSerializer = fieldValueSerializer;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.idExtractor = idExtractor;
        this.scoreProvider = scoreProvider;
        this.ttlProvider = ttlProvider;
        this.fieldExtractor = fieldExtractor;
        this.entityBuilder = entityBuilder;
    }

    RedisRepositoryConfig.KeyPrefixProvider keyPrefixProvider() { return keyPrefixProvider; }
    FieldValueSerializer fieldValueSerializer() { return fieldValueSerializer; }
    Serializer<Object> serializer() { return serializer; }
    Deserializer<Object> deserializer() { return deserializer; }
    IdExtractor<Object, Object> idExtractor() { return idExtractor; }
    ScoreProvider<Object> scoreProvider() { return scoreProvider; }
    TtlProvider<Object> ttlProvider() { return ttlProvider; }
    FieldExtractor<Object> fieldExtractor() { return fieldExtractor; }
    EntityBuilder<Object> entityBuilder() { return entityBuilder; }
}
