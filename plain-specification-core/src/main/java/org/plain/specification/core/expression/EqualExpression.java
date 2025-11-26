package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 等于号 表达式，比较 两边的值是否相等
 * @author Hugh
 * @param <T> 实体类型
 */
@Getter
public class EqualExpression<T,V extends Comparable<V>> extends BinaryExpression<T, V>{


    public EqualExpression(SFunction<T, V> left, V right) {
        super(left, ExpressionOperatorEnum.EQUAL, right);
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
       return visitor.visitEqual(this);
    }
}
