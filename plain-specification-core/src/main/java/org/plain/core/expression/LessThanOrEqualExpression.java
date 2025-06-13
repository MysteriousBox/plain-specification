package org.plain.core.expression;

import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Function;

public class LessThanOrEqualExpression <T,V extends Comparable<V>> extends BinaryExpression<T, V> {

    public LessThanOrEqualExpression(SFunction<T, V> left,V right) {
        super(left, ExpressionOperatorEnum.LESS_THAN_OR_EQUAL, right);
    }
    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitLte(this);
    }
}
