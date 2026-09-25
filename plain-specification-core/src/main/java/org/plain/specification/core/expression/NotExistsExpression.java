package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Collection;

@Getter

/**
 * NOT EXISTS 子查询排除表达式。
 *
 * @author Jayden.Liang
 */

public class NotExistsExpression<T,E> implements IExpression<T> {

    private final SFunction<T, Collection<E>> left;
    private final IExpression<E> expression;
    public NotExistsExpression(SFunction<T, Collection<E>> left, IExpression<E> expression) {
        this.left = left;
        this.expression = expression;
    }
    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitNotExists(this);
    }
}
