package org.plain.specification.redis;

import org.plain.utils.JsonUtil;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * 默认策略实现集合，使用项目封装的 JsonUtil 进行序列化/反序列化，使用反射尝试获取 id 字段/方法。
 *
 * @author Jayden.Liang
 * @since 1.0
 */
public final class DefaultStrategies {

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final int GET_PREFIX_LENGTH = GET_PREFIX.length();
    private static final int IS_PREFIX_LENGTH = IS_PREFIX.length();
    private static final String GET_ID_METHOD = "getId";
    private static final String GET_UUID_METHOD = "getUuid";
    private static final String ID_FIELD = "id";
    private static final String COLON = ":";
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
     * 通过反射提取实体的ID。优先使用指定的getter或字段名，否则依次尝试 getId()、getUuid()、id 字段。
     * 如果都找不到，则返回 null。若指定的 getter/field 不存在，则抛出详细异常。
     *
     * @param clazz 实体类型
     * @param idFieldOrGetter 指定的getter方法名或字段名，可为null
     * @return IdExtractor 实例
     */
    public static <T, TID> IdExtractor<T, TID> reflectionIdExtractor(Class<T> clazz, String idFieldOrGetter) {
        Objects.requireNonNull(clazz, CLAZZ_NOT_NULL_MESSAGE);
        return entity -> entity == null ? null : extractId(clazz, entity, idFieldOrGetter);
    }

    private static <T, TID> TID extractId(Class<T> clazz, T entity, String idFieldOrGetter) {
        if (hasText(idFieldOrGetter)) {
            return extractIdBySpecifiedName(clazz, entity, idFieldOrGetter);
        }
        TID id = tryGetIdByJacksonAnnotationSafe(clazz, entity);
        if (id != null) {
            return id;
        }
        id = tryGetIdSafe(clazz, entity, GET_ID_METHOD, true);
        if (id != null) {
            return id;
        }
        id = tryGetIdSafe(clazz, entity, GET_UUID_METHOD, true);
        if (id != null) {
            return id;
        }
        return tryGetIdSafe(clazz, entity, ID_FIELD, false);
    }

    private static <T, TID> TID extractIdBySpecifiedName(Class<T> clazz, T entity, String idFieldOrGetter) {
        Objects.requireNonNull(clazz, CLAZZ_NOT_NULL_MESSAGE);
        boolean isMethod = idFieldOrGetter.startsWith(GET_PREFIX) || idFieldOrGetter.startsWith(IS_PREFIX);
        try {
            return tryGetId(clazz, entity, idFieldOrGetter, isMethod);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new IllegalStateException("指定的 idFieldOrGetter '" + idFieldOrGetter + "' 在 " + clazz.getName() + " 中不存在", e);
        } catch (Exception e) {
            throw new IllegalStateException("反射获取 id 失败: " + idFieldOrGetter + " in " + clazz.getName(), e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static <T, TID> TID tryGetIdSafe(Class<T> clazz, T entity, String name, boolean isMethod) {
        try {
            return tryGetId(clazz, entity, name, isMethod);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static <T, TID> TID tryGetIdByJacksonAnnotationSafe(Class<T> clazz, T entity) {
        try {
            return tryGetIdByJacksonAnnotation(clazz, entity);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Reflectively get an ID value from a field or no-arg getter on the class or its superclasses.
     *
     * @param clazz entity class to inspect
     * @param entity entity instance
     * @param name field or method name
     * @param isMethod true for method lookup, false for field lookup
     * @return extracted id value
     * @throws Exception when the field/method is missing or reflection invocation fails
     */
    @SuppressWarnings({"squid:S3011", "unchecked"})
    private static <T, TID> TID tryGetId(Class<T> clazz, T entity, String name, boolean isMethod) throws ReflectiveOperationException {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            if (isMethod) {
                try {
                    Method method = current.getDeclaredMethod(name);
                    method.setAccessible(true);
                    return (TID) method.invoke(entity);
                } catch (NoSuchMethodException e) {
                    // try superclass
                }
            } else {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return (TID) field.get(entity);
                } catch (NoSuchFieldException e) {
                    // try superclass
                }
            }
            current = current.getSuperclass();
        }
        if (isMethod) {
            throw new NoSuchMethodException(name);
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Check whether the given property name represents an ID.
     *
     * @param name property name
     * @return true if the name equals "id" (case-insensitive)
     */
    private static boolean isIdPropertyName(String name) {
        return ID_FIELD.equalsIgnoreCase(name);
    }

    /**
     * Derive the property name from a JavaBean getter.
     *
     * @param method getter method
     * @return property name inferred from getter signature
     */
    private static String getterToPropertyName(Method method) {
        String name = method.getName();
        if (name.startsWith(GET_PREFIX) && name.length() > GET_PREFIX_LENGTH) {
            return Character.toLowerCase(name.charAt(GET_PREFIX_LENGTH)) + name.substring(GET_PREFIX_LENGTH + 1);
        }
        if (name.startsWith(IS_PREFIX) && name.length() > IS_PREFIX_LENGTH) {
            return Character.toLowerCase(name.charAt(IS_PREFIX_LENGTH)) + name.substring(IS_PREFIX_LENGTH + 1);
        }
        return name;
    }

    /**
     * Determine whether a field is annotated as an ID via Jackson annotations.
     *
     * @param field field to inspect
     * @return true if JsonProperty/JsonAlias marks it as "id"
     */
    private static boolean hasIdAnnotation(Field field) {
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null) {
            String value = jsonProperty.value();
            String name = (value == null || value.isEmpty()) ? field.getName() : value;
            return isIdPropertyName(name);
        }
        JsonAlias jsonAlias = field.getAnnotation(JsonAlias.class);
        if (jsonAlias != null) {
            for (String alias : jsonAlias.value()) {
                if (isIdPropertyName(alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine whether a no-arg method is annotated as an ID via Jackson annotations.
     *
     * @param method method to inspect
     * @return true if JsonProperty/JsonGetter/JsonAlias marks it as "id"
     */
    private static boolean hasIdAnnotation(Method method) {
        JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
        if (jsonProperty != null) {
            String value = jsonProperty.value();
            String name = (value == null || value.isEmpty()) ? getterToPropertyName(method) : value;
            return isIdPropertyName(name);
        }
        JsonGetter jsonGetter = method.getAnnotation(JsonGetter.class);
        if (jsonGetter != null) {
            String value = jsonGetter.value();
            String name = (value == null || value.isEmpty()) ? getterToPropertyName(method) : value;
            return isIdPropertyName(name);
        }
        JsonAlias jsonAlias = method.getAnnotation(JsonAlias.class);
        if (jsonAlias != null) {
            for (String alias : jsonAlias.value()) {
                if (isIdPropertyName(alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Try extracting the ID by scanning Jackson-annotated fields/getters in the class hierarchy.
     *
     * @param clazz entity class to inspect
     * @param entity entity instance
     * @return extracted id value or null if not found
     * @throws Exception when reflection access fails
     */
    @SuppressWarnings({"squid:S3011", "unchecked"})
    private static <T, I> I tryGetIdByJacksonAnnotation(Class<T> clazz, T entity) throws ReflectiveOperationException {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!hasIdAnnotation(field)) {
                    continue;
                }
                field.setAccessible(true);
                return (I) field.get(entity);
            }
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 0 || !hasIdAnnotation(method)) {
                    continue;
                }
                method.setAccessible(true);
                return (I) method.invoke(entity);
            }
            current = current.getSuperclass();
        }
        return null;
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
     */
    public static RedisRepositoryConfig.KeyPrefixProvider defaultKeyPrefixProvider() {
        return (clazz, globalPrefix) -> buildKeyPrefix(null, null, clazz, globalPrefix);
    }

    /**
     * 企业级 KeyPrefixProvider 工厂：支持 env + tenantProvider + globalPrefix + entity。
     * @param env 环境标识
     * @param tenantProvider 租户ID提供器
     * @return KeyPrefixProvider 实例
     */
    public static RedisRepositoryConfig.KeyPrefixProvider enterpriseKeyPrefixProvider(String env, TenantProvider tenantProvider) {
        return (clazz, globalPrefix) -> buildKeyPrefix(env, tenantProvider == null ? null : tenantProvider.getTenantId(), clazz, globalPrefix);
    }

    private static String buildKeyPrefix(String env, String tenant, Class<?> clazz, String globalPrefix) {
        StringBuilder sb = new StringBuilder();
        appendSegment(sb, env);
        appendSegment(sb, tenant);
        appendGlobalPrefix(sb, globalPrefix);
        String entityName = (clazz == null) ? "Entity" : clazz.getSimpleName();
        sb.append(entityName).append(COLON);
        return sb.toString();
    }

    private static void appendSegment(StringBuilder sb, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(value).append(COLON);
        }
    }

    private static void appendGlobalPrefix(StringBuilder sb, String globalPrefix) {
        if (globalPrefix != null && !globalPrefix.isEmpty()) {
            sb.append(globalPrefix);
            if (!globalPrefix.endsWith(COLON)) {
                sb.append(COLON);
            }
        }
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
