package org.plain.specification.core.evaluate;

import org.plain.specification.core.ISpecification;

import java.util.Collection;

/**
 * Interface ISpecificationEvaluator.
 *
 * @author Jayden.Liang
 */
public interface ISpecificationEvaluator {

    /**
     * Evaluates a collection of entities against the given specification.
     *
     * @param entities entities to evaluate
     * @param specification specification used for evaluation
     * @param <T> entity type
     * @return evaluated entities
     */
    <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification);
}
