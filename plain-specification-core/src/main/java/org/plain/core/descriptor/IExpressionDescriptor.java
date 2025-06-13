package org.plain.core.descriptor;

import org.plain.core.expression.ExpressionOperatorEnum;
import org.plain.core.visitor.IExpressionVisitor;

public interface IExpressionDescriptor<T> {

    <R> R func(IExpressionVisitor<T,R> visitor);
}
