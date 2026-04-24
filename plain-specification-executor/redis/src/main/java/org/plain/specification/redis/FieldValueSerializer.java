package org.plain.specification.redis;

/**
 * 可选的字段值序列化器，用于将单个字段值序列化为 Redis 中存储的字符串。
 * 这个接口与 {@link Serializer} 不同，Serializer 是针对整个实体的；此接口用于按字段自定义序列化策略。
 *
 * 实现类应保证序列化与反序列化（在 EntityBuilder 中）的一致性
 * @see Serializer
 * @see EntityBuilder
 * @author Jayden.Liang
 * @since 1.0
 */
@FunctionalInterface
public interface FieldValueSerializer {

    /**
    * 将单个字段值序列化为字符串
    * @param value 字段值
    * @return 序列化后的字符串
    */
    String serialize(Object value);
}

