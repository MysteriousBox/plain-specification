package org.plain.specification.core.expression;

import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * Class LessThanOrEqualExpression.
 *
 * @author Jayden.Liang
 */
public class LessThanOrEqualExpression <T,V extends Comparable<V>> extends BinaryExpression<T, V> {

    public LessThanOrEqualExpression(SFunction<T, V> left,V right) {
        super(left, ExpressionOperatorEnum.LESS_THAN_OR_EQUAL, right);
    }
    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitLte(this);
    }
}
