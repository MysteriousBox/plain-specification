package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.function.Function;

@Getter
public class LikeExpression<T> implements IExpression<T>{

    private final Function<T, String> left;
    private final String right;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.LIKE;
    public LikeExpression(Function<T, String> left, String right) {
        this.left = left;
        this.right = right;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitLike(this);
    }
}
