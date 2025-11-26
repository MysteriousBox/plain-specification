package org.plain.specification.core;

import org.plain.specification.core.builder.ISpecificationBuilder;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Specification interface
 * @param <T>
 */
public interface ISpecification<T> {

    ISpecificationBuilder<T> query();

    Collection<T> evaluate(Collection<T> entities);

    Boolean isSatisfiedBy(T entity);

    <R> List<R> selectCompiler(IExpressionVisitor<T,R> visitor);

    Function<Collection<T>, Collection<T>> postProcessingAction();

    Iterable<IExpressionDescriptor<T>> getWhereExpressions();

    Iterable<OrderExpressionInfo<T>> getOrderExpressions();


}
