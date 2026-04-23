package org.plain.specification.redis;

/**
 * 从实体中提取主键 ID
 *
 * @param <T>   实体类型
 * @param <TID> 主键类型
 * @author Jayden.Liang
 * @since 1.0
 */
/**
 * Interface IdExtractor.
 *
 * @author Jayden.Liang
 */
@FunctionalInterface
public interface IdExtractor<T, TID> {
    TID getId(T entity);
}

