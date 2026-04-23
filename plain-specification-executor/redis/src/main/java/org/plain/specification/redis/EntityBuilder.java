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


    /**
    * 从字段映射构建实体。
    *
    * @param fields 字段映射，键为字段名，值为字段值
    * @return 构建的实体对象
    */
    T buildEntity(Map<Object, Object> fields);
}

