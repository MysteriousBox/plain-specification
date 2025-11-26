package org.plain.specification.core.expression;

import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 小于 表达式
 * @author Hugh
 * @param <T>
 * @param <V>
 */
public class LessThanExpression<T, V extends Comparable<V>> extends BinaryExpression<T, V>{
    public LessThanExpression(SFunction<T, V> left, V right) {
        super(left, ExpressionOperatorEnum.LESS_THAN, right);
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitLt(this);
    }
}
