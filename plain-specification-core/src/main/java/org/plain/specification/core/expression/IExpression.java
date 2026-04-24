package org.plain.specification.core.expression;

import org.plain.specification.core.visitor.IExpressionVisitor;

/**
 * 表达式 接口
 * @param <T> 类型
 * @author Jayden.Liang
 */
public interface IExpression<T> {



    /**
     * 接受访问者
     * @param visitor 访问者
     * @param <R> 访问者返回类型
     * @return 访问者返回结果
     */
    <R> R accept(IExpressionVisitor<T,R> visitor);
}
