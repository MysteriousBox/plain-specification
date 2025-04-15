package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.List;

/**
 * 与 逻辑表达式
 * @author Hugh
 * @param <T>
 */
@Getter
public class AndExpression <T> implements IExpression<T>  {

    private final IExpression<T> left;
    private final IExpression<T> right;

    public AndExpression(IExpression<T> left, IExpression<T> right) {
        this.left = left;
        this.right = right;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitAnd(this);
    }
}
