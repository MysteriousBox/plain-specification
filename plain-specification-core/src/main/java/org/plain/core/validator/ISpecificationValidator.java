package org.plain.core.validator;

import org.plain.core.ISpecification;
import org.plain.core.Specification;

public interface ISpecificationValidator {

    <T> Boolean isValid(T entity, ISpecification<T> specification);
}
