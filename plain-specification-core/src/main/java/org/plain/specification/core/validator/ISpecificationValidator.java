package org.plain.specification.core.validator;

import org.plain.specification.core.ISpecification;

/**
 * Interface ISpecificationValidator.
 *
 * @author Jayden.Liang
 */
public interface ISpecificationValidator {

    /**
     * 验证实体是否满足规范
     * @param <T> 实体类型
     * @param entity 实体对象
     * @param specification 规范对象
     * @return 如果满足规范则返回true，否则返回false
     */
    <T> Boolean isValid(T entity, ISpecification<T> specification);
}
