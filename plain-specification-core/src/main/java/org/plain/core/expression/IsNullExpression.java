package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Function;

/**
 * 是不是 null 值
 * @param <T>
 */
@Getter
public class IsNullExpression<T> implements IExpression<T>{

    private final Function<T, Boolean> left;

    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.IS_NULL;

    public IsNullExpression(Function<T, Boolean> left) {
        this.left = left;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitIsNull(this);
    }
}
