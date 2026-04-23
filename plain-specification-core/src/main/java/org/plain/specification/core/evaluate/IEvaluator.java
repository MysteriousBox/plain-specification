package org.plain.specification.core.evaluate;

import org.plain.specification.core.ISpecification;

import java.util.Collection;

/**
 * Interface IEvaluator.
 *
 * @author Jayden.Liang
 */


public interface IEvaluator {

    /**
     * Determines whether this evaluator can evaluate criteria directly.
     *
     * @return true if criteria evaluator
     */
    Boolean isCriteriaEvaluator();

    /**
     * Evaluates the entities against the specified specification.
     *
     * @param entities entities to evaluate
     * @param specification specification used for evaluation
     * @param <T> entity type
     * @return filtered entities
     */
    <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification);
}
