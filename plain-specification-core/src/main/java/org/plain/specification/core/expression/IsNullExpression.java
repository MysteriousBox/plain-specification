package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 是不是 null 值
 * @param <T>
 * @param <V>
 * @author Jayden.Liang
 */
@Getter
public class IsNullExpression<T, V> implements IExpression<T>{

    private final SFunction<T, V> left;

    public IsNullExpression(SFunction<T, V> left) {
        this.left = left;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitIsNull(this);
    }
}
