package org.plain.core.evaluate;

import org.plain.core.ISpecification;

import java.util.Collection;

public interface IEvaluator {
    Boolean isCriteriaEvaluator();

    <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification);
}
