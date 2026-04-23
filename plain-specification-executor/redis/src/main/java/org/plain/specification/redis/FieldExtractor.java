package org.plain.specification.redis;

import java.util.Map;

/**
 * 字段提取器接口，用于从实体中提取字段映射。
 *
 * @param <T> 实体类型
 */
/**
 * Interface FieldExtractor.
 *
 * @author Jayden.Liang
 */
@FunctionalInterface
public interface FieldExtractor<T> {
    Map<String, Object> extractFields(T entity);
}

