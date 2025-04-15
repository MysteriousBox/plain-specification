package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.function.Function;

@Getter
public class NotInExpression <T,V extends Comparable<V>> implements IExpression<T>{

    private final Function<T, V> left;
    private final Collection<V> right;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.NOT_IN;

    public NotInExpression(Function<T, V> left, Collection<V> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitNot(this);
    }
}
