package org.plain.specification.core.expression;

import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 大于等于（>=）比较表达式。
 *
 * @author Jayden.Liang
 */
public class GreaterThanOrEqualExpression<T, V extends Comparable<V>> extends AbstractBinaryExpression<T, V> {
    public GreaterThanOrEqualExpression(SFunction<T, V> left, V right) {
        super(left, ExpressionOperatorEnum.GREATER_THAN_OR_EQUAL, right);
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitGte(this);
    }
}
