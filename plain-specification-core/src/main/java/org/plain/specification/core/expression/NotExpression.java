package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 非 逻辑运算符
 * @author Jayden.Liang
 */
/**
 * Class NotExpression.
 *
 * @author Jayden.Liang
 */
@Getter
public class NotExpression<T> implements IExpression<T> {

    private final IExpression<T> expression;
    public NotExpression(IExpression<T> expression) {
        this.expression = expression;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitNot(this);
    }
}
