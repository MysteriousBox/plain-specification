package org.plain.core.expression;

import org.plain.core.visitor.IExpressionVisitor;

/**
 * 表达式 接口
 * @param <T> 类型
 */
public interface IExpression<T> {



    /**
     * 接受访问者
     * @param visitor 访问者
     */
    <R> R accept(IExpressionVisitor<T,R> visitor);
}
