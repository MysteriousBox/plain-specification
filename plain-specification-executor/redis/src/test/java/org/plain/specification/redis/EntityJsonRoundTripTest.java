package org.plain.specification.redis;

import org.junit.jupiter.api.Test;
import org.plain.utils.JsonUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HASH round trip goes entity -> field map -> HASH strings -> entity. Two shapes used to break
 * it silently: a getter with no setter (the extractor stores it, the builder then rejected the
 * unknown property and the whole entity was dropped), and any {@code java.time} value (the shared
 * JSON mapper had no module for it, so saving threw).
 */
class EntityJsonRoundTripTest {

    public static class Session {
        private String id;
        private String user;
        private LocalDateTime createdAt;
        private Instant touchedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public Instant getTouchedAt() { return touchedAt; }
        public void setTouchedAt(Instant touchedAt) { this.touchedAt = touchedAt; }

        /** Derived: stored by the getter-based extractor, has no setter to read it back. */
        public String getLabel() { return user + "@" + id; }
    }

    private static <T> Map<Object, Object> toStoredHash(T entity, RedisRepositoryConfig<T, ?> config) {
        FieldExtractor<T> extractor = config.getFieldExtractor();
        Map<Object, Object> stored = new LinkedHashMap<Object, Object>();
        for (Map.Entry<String, Object> entry : extractor.extractFields(entity).entrySet()) {
            stored.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return stored;
    }

    private static RedisRepositoryConfig<Session, String> jacksonConfig() {
        return new RedisRepositoryConfig.Builder<Session, String>()
                .resolvedZsetKey("s:index")
                .resolvedHashKeyPrefix("s:")
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Session.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Session.class, null))
                .fieldExtractor(DefaultStrategies.reflectionFieldExtractor())
                .entityBuilder(DefaultStrategies.reflectionEntityBuilder(Session.class))
                .build();
    }

    private static Session sample() {
        Session session = new Session();
        session.setId("abc");
        session.setUser("alice");
        session.setCreatedAt(LocalDateTime.of(2026, 10, 16, 9, 30, 15));
        session.setTouchedAt(Instant.ofEpochSecond(1_700_000_000L));
        return session;
    }

    @Test
    @SuppressWarnings("unchecked")
    void derivedGetterIsToleratedOnRead() {
        RedisRepositoryConfig<Session, String> config = jacksonConfig();
        Session original = sample();

        Map<Object, Object> stored = toStoredHash(original, config);
        assertTrue(stored.containsKey("label"), "the extractor writes derived getters: " + stored.keySet());

        Session rebuilt = config.getEntityBuilder().buildEntity(stored);
        assertNotNull(rebuilt, "a derived getter must not make the whole entity unbuildable");
        assertEquals("abc", rebuilt.getId());
        assertEquals("alice", rebuilt.getUser());
        assertEquals(original.getLabel(), rebuilt.getLabel());
    }

    @Test
    void temporalValuesSurviveTheRoundTrip() {
        RedisRepositoryConfig<Session, String> config = jacksonConfig();
        Session original = sample();

        String serialized = config.getSerializer().serialize(original);
        assertTrue(serialized.contains("2026-10-16T09:30:15"), "temporal values need ISO text: " + serialized);

        Map<Object, Object> stored = toStoredHash(original, config);
        Session rebuilt = config.getEntityBuilder().buildEntity(stored);
        assertEquals(original.getCreatedAt(), rebuilt.getCreatedAt());
        assertEquals(original.getTouchedAt(), rebuilt.getTouchedAt());
    }

    @Test
    void fastExtractorAlsoReadsTemporals() {
        RedisRepositoryConfig<Session, String> config = new RedisRepositoryConfig.Builder<Session, String>()
                .resolvedZsetKey("s:index")
                .resolvedHashKeyPrefix("s:")
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Session.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Session.class, null))
                .fieldExtractor(DefaultStrategies.fastReflectionFieldExtractor())
                .entityBuilder(DefaultStrategies.reflectionEntityBuilder(Session.class))
                .build();
        Session original = sample();
        Map<Object, Object> stored = new LinkedHashMap<Object, Object>();
        for (Map.Entry<String, Object> entry : config.getFieldExtractor().extractFields(original).entrySet()) {
            stored.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        assertEquals("2026-10-16T09:30:15", stored.get("createdAt"));
        assertEquals(original.getCreatedAt(), config.getEntityBuilder().buildEntity(stored).getCreatedAt());
        assertNull(config.getFieldExtractor().extractFields(new Session()).get("createdAt"),
                "an unset temporal extracts as null");
    }

    @Test
    @SuppressWarnings("unchecked")
    void ordinaryPayloadsAreEncodedExactlyAsBefore() {
        Map<String, Object> plain = new LinkedHashMap<String, Object>();
        plain.put("id", "1");
        plain.put("name", "Alice");
        plain.put("age", 30);
        plain.put("tags", java.util.Arrays.asList("a", "b"));
        // the storage codec only adds module + tolerance settings, so pre-existing HASH content stays
        // byte-identical for everything that worked before
        assertEquals(JsonUtil.serialize(plain), EntityJson.write(plain));
        assertEquals(Plain.class.getName(), EntityJson.read("{\"id\":\"1\"}", Plain.class).getClass().getName());
        Map<String, Object> back = EntityJson.read(EntityJson.write(plain), Map.class);
        assertEquals(plain, back);
    }

    public static class Plain {
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    @Test
    void payloadStrategyRoundTrips() {
        RedisRepositoryConfig<Session, String> config = new RedisRepositoryConfig.Builder<Session, String>()
                .resolvedZsetKey("s:index")
                .resolvedHashKeyPrefix("s:")
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Session.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Session.class, null))
                .fieldExtractor(DefaultStrategies.payloadFieldExtractor())
                .entityBuilder(DefaultStrategies.payloadEntityBuilder(Session.class))
                .build();
        Session original = sample();
        Map<String, Object> fields = config.getFieldExtractor().extractFields(original);
        List<Object> values = new java.util.ArrayList<Object>(fields.values());
        Map<Object, Object> stored = new LinkedHashMap<Object, Object>();
        stored.put(RedisConstants.PAYLOAD_FIELD, String.valueOf(values.get(0)));
        Session rebuilt = config.getEntityBuilder().buildEntity(stored);
        assertEquals(original.getCreatedAt(), rebuilt.getCreatedAt());
        assertEquals(original.getUser(), rebuilt.getUser());
    }
}
