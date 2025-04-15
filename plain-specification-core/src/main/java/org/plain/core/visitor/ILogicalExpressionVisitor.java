package org.plain.core.visitor;


import org.plain.core.expression.AndExpression;
import org.plain.core.expression.NotExpression;
import org.plain.core.expression.OrExpression;

public interface ILogicalExpressionVisitor<T, R> {

    R visitAnd(AndExpression<T> expression);

    R visitOr(OrExpression<T> expression);

    R visitNot(NotExpression<T> expression);
}
