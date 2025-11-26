package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

@Getter
public class LikeExpression<T> implements IExpression<T>{

    private final SFunction<T, String> left;
    private final String right;
    private final ExpressionOperatorEnum operator = ExpressionOperatorEnum.LIKE;
    public LikeExpression(SFunction<T, String> left, String right) {
        this.left = left;
        this.right = right;
    }


    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitLike(this);
    }
}
