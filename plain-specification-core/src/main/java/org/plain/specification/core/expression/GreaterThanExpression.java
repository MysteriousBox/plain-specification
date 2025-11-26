package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 大于号 表达式
 * @author Hugh
 */
@Getter
public class GreaterThanExpression<T,V extends Comparable<V>> extends BinaryExpression<T, V> {

    public GreaterThanExpression(SFunction<T, V> left,  V right) {
        super(left, ExpressionOperatorEnum.GREATER_THAN, right);
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitGt(this);
    }
}
