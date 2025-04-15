package org.plain.core.visitor;

import org.plain.core.expression.OrderExpression;

import java.util.Comparator;

public class OrderExpressionVisitor<T> extends AbstractExpressionVisitor<T, Comparator<T>> {

    @Override
    public <V extends Comparable<V>> Comparator<T> visit(OrderExpression<T, V> expression) {
        return Comparator.comparing(t -> expression.getComparator().compare(expression.getLeft().apply(t), expression.getRight()));
    }
}
