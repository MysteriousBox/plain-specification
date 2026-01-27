package org.plain.specification.redis;

/**
 * 提供实体在 ZSet 中的 score
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 * @since 1.0
 */
@FunctionalInterface
public interface ScoreProvider<T> {
    double getScore(T entity);
}

