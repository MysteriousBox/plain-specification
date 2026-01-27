package org.plain.specification.redis;

import org.plain.utils.JsonUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 默认策略实现集合，使用项目封装的 JsonUtil 进行序列化/反序列化，使用反射尝试获取 id 字段/方法。
 *
 * @author Jayden.Liang
 * @since 1.0
 */
public final class DefaultStrategies {

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
        Objects.requireNonNull(clazz, "clazz must not be null");
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
        Objects.requireNonNull(clazz, "clazz must not be null");
        return entity -> {
            if (entity == null) {
                return null;
            }
            if (idFieldOrGetter != null && !idFieldOrGetter.isEmpty()) {
                boolean isMethod = idFieldOrGetter.startsWith("get") || idFieldOrGetter.startsWith("is");
                try {
                    return tryGetId(clazz, entity, idFieldOrGetter, isMethod);
                } catch (NoSuchMethodException | NoSuchFieldException e) {
                    throw new IllegalStateException("指定的 idFieldOrGetter '" + idFieldOrGetter + "' 在 " + clazz.getName() + " 中不存在", e);
                } catch (Exception e) {
                    throw new IllegalStateException("反射获取 id 失败: " + idFieldOrGetter + " in " + clazz.getName(), e);
                }
            }
            TID id = null;
            try {
                id = tryGetId(clazz, entity, "getId", true);
            } catch (Exception e) {
                // ignore, 尝试下一个
            }
            if (id != null) {
                return id;
            }
            try {
                id = tryGetId(clazz, entity, "getUuid", true);
            } catch (Exception e) {
                // ignore, 尝试下一个
            }
            if (id != null) {
                return id;
            }
            try {
                id = tryGetId(clazz, entity, "id", false);
            } catch (Exception e) {
                // ignore, 兜底
            }
            return id;
        };
    }

    /**
     * 反射辅助方法，尝试通过方法或字段获取ID。
     *
     * @param clazz 实体类型
     * @param entity 实体对象
     * @param name 方法名或字段名
     * @param isMethod 是否为方法
     * @return 提取到的ID，失败抛出 NoSuchMethodException/NoSuchFieldException
     */
    private static <T, TID> TID tryGetId(Class<T> clazz, T entity, String name, boolean isMethod) throws Exception {
        if (isMethod) {
            Method m = clazz.getMethod(name);
            @SuppressWarnings("unchecked")
            TID id = (TID) m.invoke(entity);
            return id;
        } else {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            TID id = (TID) f.get(entity);
            return id;
        }
    }

    /**
     * 通用 ScoreProvider：优先常用字段、Map，找不到则返回0。
     */
    @SuppressWarnings("unused")
    public static <T> ScoreProvider<T> genericScoreProvider() {
        return entity -> {
            if (entity == null) {
                return 0d;
            }
            // 1. 常用字段
            try {
                Field f = entity.getClass().getDeclaredField("score");
                f.setAccessible(true);
                Object v = f.get(entity);
                if (v != null) {
                    return Double.parseDouble(v.toString());
                }
            } catch (Exception ignore) {
                // ignore
            }
            // 2. Map类型
            if (entity instanceof Map) {
                Object v = ((Map<?, ?>) entity).get("score");
                if (v != null) {
                    return Double.parseDouble(v.toString());
                }
            }
            // 3. 默认
            return 0d;
        };
    }

    /**
     * 通用 TtlProvider：仅支持 Map 类型的 "ttl" 字段，普通实体一律返回 null（用全局默认）。
     */
    @SuppressWarnings("unused")
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

    // payload helpers for hash 'payload' storage
    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static <T> EntityBuilder<T> payloadEntityBuilder(Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
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

    // Provide a FieldExtractor that converts an object to a Map<String,Object> via Jackson
    @SuppressWarnings("unused")
    public static <T> FieldExtractor<T> reflectionFieldExtractor() {
        return entity -> {
            if (entity == null) {
                return new HashMap<>();
            }
            try {
                // serialize via JsonUtil then deserialize into a Map to reuse project's JSON config
                String json = JsonUtil.serialize(entity);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = JsonUtil.deserialize(json, Map.class);
                return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to convert entity to map via JsonUtil: " + entity, e);
            }
        };
    }

    // Provide an EntityBuilder that converts a Map<Object,Object> (from redis entries) to target class
    @SuppressWarnings("unused")
    public static <T> EntityBuilder<T> reflectionEntityBuilder(Class<T> clazz) {
        return fields -> {
            if (fields == null) {
                return null;
            }
            // convert Map<Object,Object> to Map<String,Object>
            Map<String, Object> stringKeyed = new HashMap<>();
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
    @SuppressWarnings("unused")
    public static RedisRepositoryConfig.KeyPrefixProvider defaultKeyPrefixProvider() {
        return (clazz, globalPrefix) -> {
            String gp = globalPrefix == null ? "" : globalPrefix;
            String entityName = (clazz == null) ? "Entity" : clazz.getSimpleName();
            StringBuilder sb = new StringBuilder();
            if (!gp.isEmpty()) {
                sb.append(gp);
                if (!gp.endsWith(":")) {
                    sb.append(":");
                }
            }
            sb.append(entityName).append(":");
            return sb.toString();
        };
    }

    /**
     * 企业级 KeyPrefixProvider 工厂：支持 env + tenantProvider + globalPrefix + entity。
     * @param env 环境标识
     * @param tenantProvider 租户ID提供器
     * @return KeyPrefixProvider 实例
     */
    @SuppressWarnings("unused")
    public static RedisRepositoryConfig.KeyPrefixProvider enterpriseKeyPrefixProvider(String env, TenantProvider tenantProvider) {
        return (clazz, globalPrefix) -> {
            StringBuilder sb = new StringBuilder();
            if (env != null && !env.isEmpty()) {
                sb.append(env).append(":");
            }
            String tenant = tenantProvider == null ? null : tenantProvider.getTenantId();
            if (tenant != null && !tenant.isEmpty()) {
                sb.append(tenant).append(":");
            }
            if (globalPrefix != null && !globalPrefix.isEmpty()) {
                sb.append(globalPrefix);
                if (!globalPrefix.endsWith(":")) {
                    sb.append(":");
                }
            }
            String entityPart = (clazz == null) ? "Entity" : clazz.getSimpleName();
            sb.append(entityPart).append(":");
            return sb.toString();
        };
    }

    /**
     * Default FieldValueSerializer that uses JsonUtil for non-primitive values.
     */
    @SuppressWarnings("unused")
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
