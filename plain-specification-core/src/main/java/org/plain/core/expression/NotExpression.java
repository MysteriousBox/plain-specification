package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.List;

/**
 * 非 逻辑运算符
 * @author Hugh
 */
@Getter
public class NotExpression<T> implements IExpression<T> {

    private final IExpression<T> expression;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.NOT;
    public NotExpression(IExpression<T> expression) {
        this.expression = expression;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitNot(this);
    }
}
