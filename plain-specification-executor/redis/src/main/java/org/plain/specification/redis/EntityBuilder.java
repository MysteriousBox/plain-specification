package org.plain.specification.redis;

import java.util.Map;

/**
 * 实体构建器接口，用于从字段映射构建实体。
 *
 * @author Jayden.Liang
 * @param <T> 实体类型
 */
@FunctionalInterface
public interface EntityBuilder<T> {
    T buildEntity(Map<Object, Object> fields);
}

