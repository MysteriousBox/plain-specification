package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Comparator;

@Getter
public class OrderExpression<T,V extends Comparable<V>> implements IExpression<T> {

    private final SFunction<T, V> left;
    private final Comparator<V> comparator;

    public OrderExpression(SFunction<T, V> left, Comparator<V> comparator) {
        this.left = left;
        this.comparator = comparator;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T, R> visitor) {
        return visitor.visitOrder(this);
    }
}
