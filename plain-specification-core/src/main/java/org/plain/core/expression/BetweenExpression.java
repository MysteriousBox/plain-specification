package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Function;

/**
 * between expression
 * @author Hugh
 * @param <T>
 * @param <V>
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
