package org.plain.specification.core.builder;

import org.plain.specification.core.expression.Expressions;

/**
 * Ordered specification builder contract.
 *
 * @param <T> entity type
 * @author Jayden.Liang
 */
public interface IOrderedSpecificationBuilder<T> extends ISpecificationBuilder<T> {

    /**
     * Adds a then-by expression for secondary ordering.
     *
     * @param expression order expression
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression);

    /**
     * Adds a then-by expression for secondary ordering when condition is true.
     *
     * @param expression order expression
     * @param condition execution condition
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression, boolean condition);

    /**
     * Adds a then-by descending expression for secondary ordering.
     *
     * @param expression order expression
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression);

    /**
     * Adds a then-by descending expression for secondary ordering when condition is true.
     *
     * @param expression order expression
     * @param condition execution condition
     * @return ordered builder instance
     */
    IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression, boolean condition);
}
