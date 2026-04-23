package org.plain.specification.core.builder;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.expression.Expressions;

/**
 * Specification builder contract.
 *
 * @param <T> entity type
 * @author Jayden.Liang
 */
public interface ISpecificationBuilder<T> {

    /**
     * Returns the built specification.
     *
     * @return specification
     */
    ISpecification<T> getSpecification();

    /**
     * Adds a where expression to the specification.
     *
     * @param expression where expression
     * @return builder instance
     */
    ISpecificationBuilder<T> where(Expressions<T> expression);

    /**
     * Adds a where expression to the specification when condition is true.
     *
     * @param expression where expression
     * @param condition execution condition
     * @return builder instance
     */
    ISpecificationBuilder<T> where(Expressions<T> expression, boolean condition);

    /**
     * Adds an order-by expression to the specification.
     *
     * @param expression order expression
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression);

    /**
     * Adds an order-by expression to the specification when condition is true.
     *
     * @param expression order expression
     * @param condition execution condition
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression, boolean condition);

    /**
     * Adds an order-by descending expression to the specification.
     *
     * @param expression order expression
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression);

    /**
     * Adds an order-by descending expression to the specification when condition is true.
     *
     * @param expression order expression
     * @param condition execution condition
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression, boolean condition);

}
