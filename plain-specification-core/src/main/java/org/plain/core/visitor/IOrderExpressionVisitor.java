package org.plain.core.visitor;

import org.plain.core.expression.OrderExpression;

public interface IOrderExpressionVisitor<T,R> {

    <V extends Comparable<V>> R visitOrder(OrderExpression<T,V> expression);
}
