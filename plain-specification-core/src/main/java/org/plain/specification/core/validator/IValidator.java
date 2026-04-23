package org.plain.specification.core.validator;


import org.plain.specification.core.ISpecification;

/**
 * Interface IValidator.
 *
 * @author Jayden.Liang
 */


public interface IValidator {

    /**
     * Validates the entity against the specification.
     *
     * @param entity entity instance
     * @param specification specification to validate against
     * @param <T> entity type
     * @return validation result
     */
    <T> Boolean isValid(T entity, ISpecification<T> specification);

}
