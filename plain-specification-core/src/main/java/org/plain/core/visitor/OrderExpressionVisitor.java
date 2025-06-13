package org.plain.core.visitor;

import org.plain.core.expression.OrderExpression;

import java.util.Comparator;

public class OrderExpressionVisitor<T> extends AbstractExpressionVisitor<T, Comparator<T>> {

    @Override
    public <V extends Comparable<V>> Comparator<T> visitOrder(OrderExpression<T, V> expression) {
        if (expression.getComparator() != null){
            return Comparator.comparing(t -> expression.getLeft().apply(t), expression.getComparator());
        }
        return Comparator.comparing(t -> expression.getLeft().apply(t));
    }
}
