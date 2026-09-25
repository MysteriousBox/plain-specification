package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;


/**
 * IS NOT NULL 非空判断表达式。
 *
 * @author Jayden.Liang
 */
@Getter
public class IsNotNullExpression <T,  V> implements IExpression<T> {

    private final SFunction<T, V> left;

    public IsNotNullExpression(SFunction<T, V> left) {
        this.left = left;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitIsNotNull(this);
    }
}
