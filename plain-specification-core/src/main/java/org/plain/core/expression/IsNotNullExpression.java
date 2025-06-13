package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Function;

@Getter
public class IsNotNullExpression <T,  V> implements IExpression<T> {

    private final SFunction<T, V> left;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.IS_NOT_NULL;

    public IsNotNullExpression(SFunction<T, V> left) {
        this.left = left;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitIsNotNull(this);
    }
}
