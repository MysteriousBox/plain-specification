package org.plain.specification.core.visitor;

import org.plain.specification.core.expression.OrderExpression;

import java.util.Comparator;

/**
 * 排序表达式访问器，收集排序条件并生成排序配置。
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
