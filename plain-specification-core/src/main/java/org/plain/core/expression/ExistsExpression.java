package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.function.Function;

@Getter
public class ExistsExpression <T,E> implements IExpression<T> {

    private final SFunction<T, Collection<E>> left;
    private final IExpression<E> right;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.EXISTS;

    public ExistsExpression(SFunction<T, Collection<E>> left, IExpression<E> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitExists(this);
    }
}
