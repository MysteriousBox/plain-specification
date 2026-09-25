package org.plain.specification.redis;

import org.plain.utils.JsonUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认策略实现集合，使用项目封装的 JsonUtil 进行序列化/反序列化，使用反射尝试获取 id 字段/方法。
 *
 * @author Jayden.Liang
 * @since 1.0
 */
public final class DefaultStrategies {

    private static final String CLAZZ_NOT_NULL_MESSAGE = "clazz must not be null";

    private DefaultStrategies() {
        // no-op
    }

    public static <T> Serializer<T> jacksonSerializer() {
        return entity -> {
            if (entity == null) {
                return null;
            }
            try {
                return JsonUtil.serialize(entity);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize entity via JsonUtil: " + entity, e);
            }
        };
    }

    public static <T> Deserializer<T> jacksonDeserializer(Class<T> clazz) {
        Objects.requireNonNull(clazz, CLAZZ_NOT_NULL_MESSAGE);
        return serialized -> {
            if (serialized == null) {
                return null;
            }
            try {
                return JsonUtil.deserialize(serialized, clazz);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize into " + clazz + ", source: " + serialized, e);
            }
        };
    }

    /**
     * 通过反射提取实体的ID。委托给 {@link ReflectionIdExtractor}。
     *
     * @param clazz 实体类型
     * @param idFieldOrGetter 指定的getter方法名或字段名，可为null
     * @return IdExtractor 实例
     * @see ReflectionIdExtractor#reflectionIdExtractor(Class, String)
     */
    public static <T, TID> IdExtractor<T, TID> reflectionIdExtractor(Class<T> clazz, String idFieldOrGetter) {
        return ReflectionIdExtractor.reflectionIdExtractor(clazz, idFieldOrGetter);
    }

    /**
     * 通用 ScoreProvider：优先常用字段、Map，找不到则返回0。
     */
    public static <T> ScoreProvider<T> genericScoreProvider() {
        return entity -> {
            if (entity == null) {
                return 0d;
            }
            Double score = getScoreFromEntity(entity);
            if (score != null) {
                return score;
            }
            if (entity instanceof Map) {
                Object v = ((Map<?, ?>) entity).get("score");
                if (v != null) {
                    return Double.parseDouble(v.toString());
                }
            }
            return 0d;
        };
    }

    private static Double getScoreFromEntity(Object entity) {
        Double score = getScoreFromGetter(entity, "getScore");
        if (score != null) {
            return score;
        }
        score = getScoreFromGetter(entity, "isScore");
        if (score != null) {
            return score;
        }
        return getScoreFromPublicField(entity, "score");
    }

    private static Double getScoreFromGetter(Object entity, String methodName) {
        try {
            Method method = entity.getClass().getMethod(methodName);
            Object v = method.invoke(entity);
            if (v != null) {
                return Double.parseDouble(v.toString());
            }
        } catch (Exception e) {
            // ignore
        } 
        return null;
    }

    private static Double getScoreFromPublicField(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getField(fieldName);
            Object v = field.get(entity);
            if (v != null) {
                return Double.parseDouble(v.toString());
            }
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
            // ignore
        }
        return null;
    }

    /**
     * 通用 TtlProvider：仅支持 Map 类型的 "ttl" 字段，普通实体一律返回 null（用全局默认）。
     */
    public static <T> TtlProvider<T> genericTtlProvider() {
        return entity -> {
            if (entity == null) {
                return null;
            }
            // 仅 Map 类型支持 "ttl" 字段
            if (entity instanceof Map) {
                Object v = ((Map<?, ?>) entity).get("ttl");
                if (v != null) {
                    return Long.parseLong(v.toString());
                }
            }
            // 默认：无过期，交给全局 defaultTtl
            return null;
        };
    }

    public static <T> ScoreProvider<T> defaultScoreProvider() {
        return entity -> 0d;
    }

    public static <T> TtlProvider<T> defaultTtlProvider() {
        return entity -> null;
    }

    /**
     * Payload helpers for hash 'payload' storage.
     *
     * @param <T> entity type
     * @return field extractor that stores the entity payload under a single key
     */
    public static <T> FieldExtractor<T> payloadFieldExtractor() {
        return entity -> {
            if (entity == null) {
                return Collections.emptyMap();
            }
            try {
                String payload = JsonUtil.serialize(entity);
                return Collections.singletonMap(RedisConstants.PAYLOAD_FIELD, payload);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize entity for payload: " + entity, e);
            }
        };
    }

    public static <T> EntityBuilder<T> payloadEntityBuilder(Class<T> clazz) {
        Objects.requireNonNull(clazz, CLAZZ_NOT_NULL_MESSAGE);
        return fields -> {
            if (fields == null || fields.isEmpty()) {
                return null;
            }
            Object payload = fields.get(RedisConstants.PAYLOAD_FIELD);
            if (payload == null) {
                return null;
            }
            try {
                return JsonUtil.deserialize(String.valueOf(payload), clazz);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize payload into " + clazz + ", source: " + payload, e);
            }
        };
    }

    
    /**
     * Creates a field extractor that converts the entity to a map via Jackson reflection.
     *
     * @param <T> entity type
     * @return field extractor that converts the entity into string-keyed fields
     */
    public static <T> FieldExtractor<T> reflectionFieldExtractor() {
        return entity -> {
            if (entity == null) {
                return Collections.emptyMap();
            }
            try {
                // serialize via JsonUtil then deserialize into a Map to reuse project's JSON config
                String json = JsonUtil.serialize(entity);
               
                Map<?, ?> map = JsonUtil.deserializeBuilder().registerModule(new JavaTimeModule()).deserialize(json, Map.class);
                // JsonUtil may deserialize JSON `null` values into map entries with null values或
                // even produce entries with null keys (malformed input). Collectors.toMap(...) and
                // some downstream code may throw on null keys — be defensive: skip null keys and
                // preserve null values.
                if (map == null) {
                    return Collections.emptyMap();
                }
                Map<String, Object> result = new HashMap<>(map.size());
                for (Entry<?, ?> e : map.entrySet()) {
                    if (e == null || e.getKey() == null) {
                        continue;
                    }
                    result.put((String) e.getKey(), e.getValue());
                }
                return result;
             } catch (Exception e) {
                 throw new IllegalStateException("Failed to convert entity to map via JsonUtil: " + entity, e);
             }
         };
    }

    private static final ConcurrentHashMap<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Creates a fast field extractor that uses direct Java reflection instead of JSON round-trip.
     * <p>
     * Compared to {@link #reflectionFieldExtractor()}, this is significantly faster but does not
     * support Jackson getter-based properties ({@code @JsonGetter}). It does respect
     * {@code @JsonProperty} for field naming. Fields are cached per class for performance.
     * </p>
     *
     * @param <T> entity type
     * @return field extractor that uses direct reflection
     */
    public static <T> FieldExtractor<T> fastReflectionFieldExtractor() {
        return entity -> {
            if (entity == null) {
                return Collections.emptyMap();
            }
            final Class<?> clazz = entity.getClass();
            final List<Field> fields = FIELD_CACHE.computeIfAbsent(clazz, DefaultStrategies::collectFields);
            final Map<String, Object> result = new HashMap<>(fields.size());
            for (Field field : fields) {
                try {
                    final String name = resolveFieldName(field);
                    final Object value = field.get(entity);
                    result.put(name, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to access field: " + field.getName(), e);
                }
            }
            return result;
        };
    }

    private static List<Field> collectFields(Class<?> clazz) {
        final Map<String, Field> fieldMap = new LinkedHashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                final int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                fieldMap.putIfAbsent(field.getName(), field);
            }
            current = current.getSuperclass();
        }
        final List<Field> fields = new ArrayList<>(fieldMap.values());
        for (Field field : fields) {
            field.setAccessible(true);
        }
        return fields;
    }

    private static String resolveFieldName(Field field) {
        final JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null) {
            final String value = jsonProperty.value();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return field.getName();
    }

    /**
     * Provides an EntityBuilder that converts a Map<Object,Object> (from redis entries) to target class.
     *
     * @param <T> entity type
     * @param clazz target entity type
     * @return builder that converts field maps into entity instances
     */
    public static <T> EntityBuilder<T> reflectionEntityBuilder(Class<T> clazz) {
        return fields -> {
            if (fields == null) {
                return null;
            }
            // convert Map<Object,Object> to Map<String,Object>
            Map<String, Object> stringKeyed = new HashMap<>(fields.size());
            for (Map.Entry<Object, Object> e : fields.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                stringKeyed.put(String.valueOf(e.getKey()), e.getValue());
            }
            try {
                String json = JsonUtil.serialize(stringKeyed);
                return JsonUtil.deserialize(json, clazz);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to convert fields into " + clazz + " via JsonUtil: " + stringKeyed, e);
            }
        };
    }

    /**
     * Default KeyPrefixProvider: append entity simple name to global prefix (legacy behaviour).
     * Delegates to {@link DefaultKeyPrefixProvider}.
     *
     * @see DefaultKeyPrefixProvider#defaultKeyPrefixProvider()
     */
    public static RedisRepositoryConfig.KeyPrefixProvider defaultKeyPrefixProvider() {
        return DefaultKeyPrefixProvider.defaultKeyPrefixProvider();
    }

    /**
     * 企业级 KeyPrefixProvider 工厂：支持 env + tenantProvider + globalPrefix + entity。
     * Delegates to {@link DefaultKeyPrefixProvider}.
     *
     * @param env 环境标识
     * @param tenantProvider 租户ID提供器
     * @return KeyPrefixProvider 实例
     * @see DefaultKeyPrefixProvider#enterpriseKeyPrefixProvider(String, TenantProvider)
     */
    public static RedisRepositoryConfig.KeyPrefixProvider enterpriseKeyPrefixProvider(String env, TenantProvider tenantProvider) {
        return DefaultKeyPrefixProvider.enterpriseKeyPrefixProvider(env, tenantProvider);
    }

    /**
     * Default FieldValueSerializer that uses JsonUtil for non-primitive values.
     */
    public static FieldValueSerializer defaultFieldValueSerializer() {
        return value -> {
            if (value == null) {
                return "";
            }
            if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
                return String.valueOf(value);
            }
            try {
                return JsonUtil.serialize(value);
            } catch (Exception e) {
                return String.valueOf(value);
            }
        };
    }
}
