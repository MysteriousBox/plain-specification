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


    /**
    * 获取实体在 ZSet 中的 score
    * @param entity 实体对象
    * @return score 值
    */
    double getScore(T entity);
}

