package org.plain.core.expression;

import lombok.Getter;

import java.util.function.Function;

@Getter
public abstract class BinaryExpression<T,V extends Comparable<V>> implements IExpression<T>{

    private final Function<T, V> left;

    private final ExpressionOperatorEnum operator;

    private final V right;

    public BinaryExpression(Function<T, V> left, ExpressionOperatorEnum operator, V right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

}
