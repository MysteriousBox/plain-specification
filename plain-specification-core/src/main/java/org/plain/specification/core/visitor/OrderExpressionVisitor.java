package org.plain.specification.core.visitor;

import org.plain.specification.core.expression.OrderExpression;

import java.util.Comparator;

/**
 * Class OrderExpressionVisitor.
 *
 * @author Jayden.Liang
 */


public class OrderExpressionVisitor<T> extends AbstractExpressionVisitor<T, Comparator<T>> {

    @Override
    public <V extends Comparable<V>> Comparator<T> visitOrder(OrderExpression<T, V> expression) {
        if (expression.getComparator() != null){
            return Comparator.comparing(t -> expression.getLeft().apply(t), expression.getComparator());
        }
        return Comparator.comparing(t -> expression.getLeft().apply(t));
    }
}
