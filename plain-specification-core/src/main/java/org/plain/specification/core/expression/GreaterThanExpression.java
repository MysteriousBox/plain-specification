package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 大于表达式。
 *
 * @author Jayden.Liang
 * @param <T> 实体类型
 * @param <V> 值类型
 */
@Getter
public class GreaterThanExpression<T, V extends Comparable<V>> extends AbstractBinaryExpression<T, V> {

    public GreaterThanExpression(SFunction<T, V> left,  V right) {
        super(left, ExpressionOperatorEnum.GREATER_THAN, right);
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitGt(this);
    }
}
