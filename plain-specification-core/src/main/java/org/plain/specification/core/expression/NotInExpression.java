package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Collection;

@Getter
public class NotInExpression <T,V extends Comparable<V>> implements IExpression<T>{

    private final SFunction<T, V> left;
    private final Collection<V> right;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.NOT_IN;

    public NotInExpression(SFunction<T, V> left, Collection<V> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitNot(this);
    }
}
