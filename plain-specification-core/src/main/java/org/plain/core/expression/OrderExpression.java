package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.Comparator;
import java.util.function.Function;

@Getter
public class OrderExpression<T,V extends Comparable<V>> implements IExpression<T> {

    private final Function<T, V> left;
    private final Comparator<V> comparator;
    private final V right;

    public OrderExpression(Function<T, V> left, Comparator<V> comparator, V right) {
        this.left = left;
        this.comparator = comparator;
        this.right = right;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T, R> visitor) {
        return visitor.visit(this);
    }
}
