package org.plain.specification.redis;

import java.util.Map;

/**
 * 字段提取器接口，用于从实体中提取字段映射。
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 */
@FunctionalInterface
public interface FieldExtractor<T> {


    /**
     * 从实体中提取字段映射，键为字段名，值为字段值
     * @param entity 实体对象
     * @return 字段映射
     */
    Map<String, Object> extractFields(T entity);
}

