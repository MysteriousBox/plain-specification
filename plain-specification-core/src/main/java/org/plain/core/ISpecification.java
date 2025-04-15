package org.plain.core;

import org.plain.core.expression.OrderExpressionInfo;
import org.plain.core.expression.WhereExpressionInfo;

import java.util.Collection;
import java.util.function.Function;

/**
 * Specification interface
 * @param <T>
 */
public interface ISpecification<T> {

    ISpecificationBuilder<T> query();

    Collection<T> evaluate(Collection<T> entities);

    Boolean isSatisfiedBy(T entity);

    Function<Collection<T>, Collection<T>> postProcessingAction();

    Iterable<WhereExpressionInfo<T>> getWhereExpressionInfos();

    Iterable<OrderExpressionInfo<T>> getOrderExpressionInfos();
}
