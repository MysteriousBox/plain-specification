package org.plain.specification.core.visitor;


import org.plain.specification.core.expression.AndExpression;
import org.plain.specification.core.expression.NotExpression;
import org.plain.specification.core.expression.OrExpression;

public interface ILogicalExpressionVisitor<T, R> {

    R visitAnd(AndExpression<T> expression);

    R visitOr(OrExpression<T> expression);

    R visitNot(NotExpression<T> expression);
}
