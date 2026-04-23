package org.plain.specification.core.visitor;


import org.plain.specification.core.expression.*;

/**
 * 表达式访问者
 * @author Jayden.Liang
 * @param <T>
 */
public interface IExpressionVisitor<T,R> extends ILogicalExpressionVisitor<T,R>, IOrderExpressionVisitor<T,R>,IComparisonExpressionVisitor <T, R>  {

    R visitLike(LikeExpression<T> expression);

    <V> R visitIsNull(IsNullExpression<T,V> expression);

    <V> R visitIsNotNull(IsNotNullExpression<T,V> expression);

    <E> R visitExists(ExistsExpression<T,E> expression);

    <E> R visitNotExists(NotExistsExpression<T,E> expression);


}
