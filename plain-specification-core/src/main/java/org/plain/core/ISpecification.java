package org.plain.core;

import org.plain.core.builder.ISpecificationBuilder;
import org.plain.core.descriptor.IExpressionDescriptor;
import org.plain.core.expression.OrderExpressionInfo;
import org.plain.core.expression.WhereExpressionInfo;
import org.plain.core.visitor.IExpressionVisitor;

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
