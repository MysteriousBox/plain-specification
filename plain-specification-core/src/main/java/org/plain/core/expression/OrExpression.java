package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.List;

/**
 * 或 逻辑运算符表达式
 * @author Hugh
 * @param <T>
 */
@Getter
public class OrExpression <T>  implements IExpression<T> {
    private final IExpression<T> left;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.OR;
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
