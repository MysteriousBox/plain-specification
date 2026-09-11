package org.plain.specification.core.descriptor;

import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * Interface IExpressionDescriptor.
 *
 * @author Jayden.Liang
 */
public interface IExpressionDescriptor<T> {

    /**
     * Applies the descriptor function to the visitor.
     *
     * @param visitor expression visitor
     * @param <R> result type
     * @return visitor result
     */
    <R> R func(IExpressionVisitor<T,R> visitor);

    /**
     * Returns the expression wrapper for this descriptor.
     *
     * @return expression wrapper
     */
    Expressions<T> getExpressions();
}
