package org.plain.specification.core.evaluate;

import org.plain.specification.core.ISpecification;

import java.util.Collection;

public interface ISpecificationEvaluator {

    <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification);
}
