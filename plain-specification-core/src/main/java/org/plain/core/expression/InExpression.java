package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * IN 表达式
 * @author Hugh
 */
@Getter
public class InExpression <T,V extends Comparable<V>> implements IExpression<T>{

    private final SFunction<T, V> left;
    private final Collection<V> right;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.IN;

    public InExpression(SFunction<T, V> left, Collection<V> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitIn(this);
    }

}
