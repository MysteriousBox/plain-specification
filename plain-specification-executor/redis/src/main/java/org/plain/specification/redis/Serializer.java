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

    /**
     * 将实体序列化为字符串
     * @param entity 实体对象
     * @return 序列化后的字符串
     */
    String serialize(T entity);
}

