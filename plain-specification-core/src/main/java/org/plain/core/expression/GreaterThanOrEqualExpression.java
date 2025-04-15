package org.plain.core.expression;

import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Function;

public class GreaterThanOrEqualExpression<T,V extends Comparable<V>> extends BinaryExpression<T, V>{
    public GreaterThanOrEqualExpression(Function<T, V> left, V right) {
        super(left, ExpressionOperatorEnum.GREATER_THAN_OR_EQUAL, right);
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitGte(this);
    }
}
