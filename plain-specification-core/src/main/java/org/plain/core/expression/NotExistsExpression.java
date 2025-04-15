package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.function.Function;

@Getter
public class NotExistsExpression<T,E> implements IExpression<T> {

    private final Function<T, Collection<E>> left;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.NOT_EXISTS;
    private final IExpression<E> expression;
    public NotExistsExpression(Function<T, Collection<E>> left, IExpression<E> expression) {
        this.left = left;
        this.expression = expression;
    }
    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitNotExists(this);
    }
}
