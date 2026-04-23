package org.plain.specification.core.visitor;

import org.plain.specification.core.expression.OrderExpression;

/**
 * Interface IOrderExpressionVisitor.
 *
 * @author Jayden.Liang
 */
public interface IOrderExpressionVisitor<T,R> {


    /**
     * 访问 OrderExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 OrderExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitOrder(OrderExpression<T,V> expression);
}
