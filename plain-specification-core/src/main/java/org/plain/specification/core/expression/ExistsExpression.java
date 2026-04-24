package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Collection;


/**
 * Class ExistsExpression.
 *
 * @author Jayden.Liang
 */
@Getter
public class ExistsExpression <T,E> implements IExpression<T> {

    private final SFunction<T, Collection<E>> left;
    private final IExpression<E> right;

    public ExistsExpression(SFunction<T, Collection<E>> left, IExpression<E> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitExists(this);
    }
}
