package org.plain.specification.redis;

/**
 * 实体反序列化接口
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 * @since 1.0
 */
@FunctionalInterface
public interface Deserializer<T> {
    T deserialize(String serialized);
}

