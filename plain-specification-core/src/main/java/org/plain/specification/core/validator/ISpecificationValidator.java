package org.plain.specification.core.validator;

import org.plain.specification.core.ISpecification;

public interface ISpecificationValidator {

    <T> Boolean isValid(T entity, ISpecification<T> specification);
}
