package org.plain.specification.core.expression;

import lombok.Getter;



/**
 * 二元比较表达式基类。
 *
 * @author Jayden.Liang
 * @param <T> 实体类型
 * @param <V> 值类型
 */
@Getter
public abstract class AbstractBinaryExpression<T, V extends Comparable<V>> implements IExpression<T> {

    private final SFunction<T, V> left;

    private final ExpressionOperatorEnum operator;

    private final V right;

    public AbstractBinaryExpression(SFunction<T, V> left, ExpressionOperatorEnum operator, V right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

}
