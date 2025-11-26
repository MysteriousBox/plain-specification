package org.plain.specification.core.visitor;

import org.plain.specification.core.expression.OrderExpression;

public interface IOrderExpressionVisitor<T,R> {

    <V extends Comparable<V>> R visitOrder(OrderExpression<T,V> expression);
}
