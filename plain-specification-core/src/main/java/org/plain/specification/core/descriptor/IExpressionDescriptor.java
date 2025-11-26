package org.plain.specification.core.descriptor;

import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.visitor.IExpressionVisitor;

public interface IExpressionDescriptor<T> {

    <R> R func(IExpressionVisitor<T,R> visitor);

    Expressions<T> getExceptions();
}
