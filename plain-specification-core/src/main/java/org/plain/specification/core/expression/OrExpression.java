package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * OR 逻辑组合表达式。
 *
 * @author Jayden.Liang
 * @param <T> 实体类型
 */
@Getter
public class OrExpression <T>  implements IExpression<T> {
    private final IExpression<T> left;
    private final IExpression<T> right;

    public OrExpression(IExpression<T> left, IExpression<T> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public <R> R  accept(IExpressionVisitor<T,R> visitor) {
       return visitor.visitOr(this);
    }
}
