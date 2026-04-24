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

    /**
     * 将字符串反序列化为实体对象
     * @param serialized 序列化后的字符串
     * @return 实体对象
     */
    T deserialize(String serialized);
}

