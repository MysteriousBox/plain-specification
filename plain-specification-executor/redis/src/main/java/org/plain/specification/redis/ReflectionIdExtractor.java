package org.plain.specification.redis;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Utility class that provides reflection-based ID extraction from entities.
 * <p>
 * Tries Jackson annotations first, then falls back to conventional getter/field names
 * ({@code getId()}, {@code getUuid()}, {@code id} field).
 * </p>
 *
 * @author Jayden.Liang
 * @since 1.0
 */
public final class ReflectionIdExtractor {

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final int GET_PREFIX_LENGTH = GET_PREFIX.length();
    private static final int IS_PREFIX_LENGTH = IS_PREFIX.length();
    private static final String GET_ID_METHOD = "getId";
    private static final String GET_UUID_METHOD = "getUuid";
    private static final String ID_FIELD = "id";
    private static final String CLAZZ_NOT_NULL_MESSAGE = "clazz must not be null";

    private ReflectionIdExtractor() {
        // no-op
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
}
