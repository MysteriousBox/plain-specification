package org.plain.specification.redis;

/**
 * 实体序列化接口
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 * @since 1.0
 */
@FunctionalInterface
public interface Serializer<T> {
    String serialize(T entity);
}

