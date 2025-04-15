package org.plain.core.validator;


import org.plain.core.ISpecification;

public interface IValidator {

    <T>  Boolean isValid(T entity, ISpecification<T> specification);

}
