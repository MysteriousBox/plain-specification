package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * between expression
 * @author Jayden.Liang
 * @param <T>
 * @param <V>
 */
/**
 * Class BetweenExpression.
 *
 * @author Jayden.Liang
 */
@Getter
public class BetweenExpression <T, V extends Comparable<V>> implements IExpression<T>{

    private final SFunction<T, V> left;
    private final V lowerBound;
    private final V upperBound;

    public BetweenExpression(SFunction<T, V> left, V lowerBound, V upperBound) {
        this.left = left;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T,R> visitor) {
        return visitor.visitBetween(this);
    }
}
