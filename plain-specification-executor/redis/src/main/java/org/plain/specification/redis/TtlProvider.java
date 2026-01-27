package org.plain.specification.redis;

/**
 * 提供实体的 TTL，单位为秒；返回 null 表示使用全局默认 TTL
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 * @since 1.0
 */
@FunctionalInterface
public interface TtlProvider<T> {
    Long getTtl(T entity);
}

