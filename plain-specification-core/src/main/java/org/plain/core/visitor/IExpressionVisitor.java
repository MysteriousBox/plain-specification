package org.plain.core.visitor;

import org.plain.core.expression.*;

/**
 * 表达式访问者
 * @author Hugh
 * @param <T>
 */
public interface IExpressionVisitor<T,R> extends ILogicalExpressionVisitor<T,R>, IOrderExpressionVisitor<T,R>,IComparisonExpressionVisitor <T, R>  {

    R visitLike(LikeExpression<T> expression);

    R visitIsNull(IsNullExpression<T> expression);

    R visitIsNotNull(IsNotNullExpression<T> expression);

    <E> R visitExists(ExistsExpression<T,E> expression);

    <E> R visitNotExists(NotExistsExpression<T,E> expression);


}
