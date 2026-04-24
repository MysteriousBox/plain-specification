package org.plain.specification.core.visitor;


import org.plain.specification.core.expression.*;

/**
 * 表达式访问者
 * @author Jayden.Liang
 * @param <T>
 */
public interface IExpressionVisitor<T,R> extends ILogicalExpressionVisitor<T,R>, IOrderExpressionVisitor<T,R>,IComparisonExpressionVisitor <T, R>  {

    /**
     * 访问 LikeExpression 表达式。
     * @param expression 要访问的 LikeExpression 表达式
     * @return 访问结果
     */
    R visitLike(LikeExpression<T> expression);

    /**
     * 访问 IsNullExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 IsNullExpression 表达式
     * @return 访问结果
     */
    <V> R visitIsNull(IsNullExpression<T,V> expression);

    /**
     * 访问 IsNotNullExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 IsNotNullExpression 表达式
     * @return 访问结果
     */
    <V> R visitIsNotNull(IsNotNullExpression<T,V> expression);

    /**
     * 访问 ExistsExpression 表达式。
     * @param <E> 表达式的元素类型
     * @param expression 要访问的 ExistsExpression 表达式
     * @return 访问结果
     */
    <E> R visitExists(ExistsExpression<T,E> expression);

    /**
     * 访问 NotExistsExpression 表达式。
     * @param <E> 表达式的元素类型
     * @param expression 要访问的 NotExistsExpression 表达式
     * @return 访问结果
     */
    <E> R visitNotExists(NotExistsExpression<T,E> expression);


}
