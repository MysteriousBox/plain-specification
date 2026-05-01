package org.plain.specification.core.expression;

import lombok.Getter;



/**
 * Class BinaryExpression.
 * @author Jayden.Liang
 * @param <T> 类型
 * @param <V> 值类型
 */
@Getter
@SuppressWarnings({"AbstractClassShouldStartWithAbstractNamingRule"})
public abstract class BinaryExpression<T,V extends Comparable<V>> implements IExpression<T>{

    private final SFunction<T, V> left;

    private final ExpressionOperatorEnum operator;

    private final V right;

    public BinaryExpression(SFunction<T, V> left, ExpressionOperatorEnum operator, V right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

}
