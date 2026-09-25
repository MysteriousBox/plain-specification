package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Comparator;


/**
 * 排序表达式，封装字段引用、比较器和排序方向。
 *
 * @author Jayden.Liang
 */
@Getter
public class OrderExpression<T,V extends Comparable<V>> implements IExpression<T> {

    private final SFunction<T, V> left;
    private final Comparator<V> comparator;
    private final boolean ascending;

    public OrderExpression(SFunction<T, V> left, Comparator<V> comparator, boolean ascending) {
        this.left = left;
        this.comparator = comparator;
        this.ascending = ascending;
    }

    @Override
    public <R> R accept(IExpressionVisitor<T, R> visitor) {
        return visitor.visitOrder(this);
    }
}
