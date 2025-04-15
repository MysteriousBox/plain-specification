package org.plain.core.expression;


import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Function;

/**
 * 不等于表达式
 * @author Hugh
 * @param <T>
 * @param <V>
 */
public class NotEqualExpression <T,V extends Comparable<V>> extends BinaryExpression<T, V>{
    public NotEqualExpression(Function<T, V> left,  V right) {
        super(left, ExpressionOperatorEnum.NOT_EQUAL, right);
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitNotEqual(this);
    }
}
