package org.plain.specification.redis.autoconfig;

import org.junit.jupiter.api.Test;
import org.plain.specification.redis.DefaultStrategies;
import org.plain.specification.redis.RedisRepositoryConfig;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resolver rebuilds the config object, so an opt-in flag set on the entity or global config has
 * to survive that rebuild or the feature is unreachable through the autoconfiguration path.
 */
class RedisRepositoryConfigResolverPushdownTest {

    static class Account {
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    private static RedisRepositoryConfig<Object, Object> global(boolean pushdown) {
        RedisRepositoryConfig.Builder<Object, Object> builder = new RedisRepositoryConfig.Builder<Object, Object>()
                .resolvedZsetKey("account:index")
                .resolvedHashKeyPrefix("account:")
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Object.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Object.class, null))
                .fieldValueSerializer(DefaultStrategies.defaultFieldValueSerializer())
                .fieldExtractor(entity -> {
                    Map<String, Object> fields = new LinkedHashMap<String, Object>();
                    fields.put("id", String.valueOf(entity));
                    return fields;
                })
                .entityBuilder(fields -> new Account())
                .fieldValueSerializer(DefaultStrategies.defaultFieldValueSerializer());
        if (pushdown) {
            builder.expressionPushdown(true);
        }
        return builder.build();
    }

    @Test
    void enabledFlagSurvivesResolution() {
        RedisRepositoryConfigResolver resolver = new RedisRepositoryConfigResolver(global(true));
        assertTrue(resolver.resolve(Account.class).isExpressionPushdownEnabled());
    }

    @Test
    void disabledByDefaultSurvivesResolution() {
        RedisRepositoryConfigResolver resolver = new RedisRepositoryConfigResolver(global(false));
        assertFalse(resolver.resolve(Account.class).isExpressionPushdownEnabled());
    }

    @Test
    void perEntityOverrideWinsOverGlobal() {
        RedisRepositoryConfigResolver resolver = new RedisRepositoryConfigResolver(global(false));
        @SuppressWarnings("unchecked")
        RedisRepositoryConfig<Account, String> overrides =
                (RedisRepositoryConfig<Account, String>) (RedisRepositoryConfig<?, ?>) global(true);
        assertTrue(resolver.resolve(Account.class, "account:custom", overrides).isExpressionPushdownEnabled());
    }
}
