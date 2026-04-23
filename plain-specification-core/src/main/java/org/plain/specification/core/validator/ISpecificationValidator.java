package org.plain.specification.core.validator;

import org.plain.specification.core.ISpecification;

/**
 * Interface ISpecificationValidator.
 *
 * @author Jayden.Liang
 */


public interface ISpecificationValidator {

    <T> Boolean isValid(T entity, ISpecification<T> specification);
}
