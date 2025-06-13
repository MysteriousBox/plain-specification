package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Function;

/**
 * 是不是 null 值
 * @param <T>
 */
@Getter
public class IsNullExpression<T, V> implements IExpression<T>{

    private final SFunction<T, V> left;

    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.IS_NULL;

    public IsNullExpression(SFunction<T, V> left) {
        this.left = left;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitIsNull(this);
    }
}
