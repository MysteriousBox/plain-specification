package org.plain.specification.redis;

/**
 * 从实体中提取主键 ID
 *
 * @param <T>   实体类型
 * @param <TID> 主键类型
 * @author Jayden.Liang
 * @since 1.0
 */
@FunctionalInterface
public interface IdExtractor<T, TID> {

    /**
     * 从实体中提取主键 ID
     * @param entity 实体对象
     * @return 主键 ID
     */
    TID getId(T entity);
}

