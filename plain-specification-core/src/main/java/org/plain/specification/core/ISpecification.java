package org.plain.specification.core;

import org.plain.specification.core.builder.ISpecificationBuilder;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Specification interface
 * @param <T>
 * @author Jayden.Liang
 */
public interface ISpecification<T> {

    /**
     * Returns the query builder for this specification.
     *
     * @return specification builder
     */
    ISpecificationBuilder<T> query();

    /**
     * Evaluates the specification against the provided entities.
     *
     * @param entities entities to evaluate
     * @return filtered entities
     */
    Collection<T> evaluate(Collection<T> entities);

    /**
     * Checks whether the entity satisfies the specification.
     *
     * @param entity entity instance
     * @return true if satisfied
     */
    Boolean isSatisfiedBy(T entity);

    /**
     * Compiles a list from the specification using the visitor.
     *
     * @param visitor expression visitor
     * @param <R> result type
     * @return compiled result list
     */
    <R> List<R> selectCompiler(IExpressionVisitor<T,R> visitor);

    /**
     * Returns the post-processing action for query results.
     *
     * @return post-processing function or null
     */
    UnaryOperator<Collection<T>> postProcessingAction();

    /**
     * Returns current where expressions.
     *
     * @return where expressions
     */
    Iterable<IExpressionDescriptor<T>> getWhereExpressions();

    /**
     * Returns current order expressions.
     *
     * @return order expressions
     */
    Iterable<OrderExpressionInfo<T>> getOrderExpressions();

}
