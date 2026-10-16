package org.plain.specification.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON codec used for the entity-to-HASH round trip.
 * <p>
 * It is deliberately the shared {@link org.plain.utils.JsonUtil} configuration plus three
 * settings the storage round trip needs: {@code java.time} support, ISO-8601 rendering so a
 * temporal value survives being stored as a HASH string, and tolerance for HASH fields that no
 * setter accepts (a derived getter is written by the getter-based extractor but has no counterpart
 * on the way back). Without that tolerance the whole entity fails to build and disappears from
 * queries instead of simply losing the computed value.
 * <p>
 * For entities that already round-tripped, the produced JSON is byte-identical to
 * {@code JsonUtil.serialize}.
 */
final class EntityJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private EntityJson() {
        // no instances
    }

    /**
     * @param value object to serialize
     * @return JSON text
     * @throws IllegalArgumentException when the value cannot be serialized
     */
    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot serialize " + describe(value), ex);
        }
    }

    /**
     * @param json JSON text
     * @param type target type
     * @param <T>  target type
     * @return the deserialized value
     * @throws IllegalArgumentException when the text cannot be read
     */
    static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot deserialize into " + type + ", source: " + json, ex);
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
