package org.plain.specification.core.visitor;


import org.plain.specification.core.expression.AndExpression;
import org.plain.specification.core.expression.NotExpression;
import org.plain.specification.core.expression.OrExpression;

/**
 * Interface ILogicalExpressionVisitor.
 *
 * @author Jayden.Liang
 */


public interface ILogicalExpressionVisitor<T, R> {

    /**
     * 访问 AndExpression 表达式。
     * @param expression 要访问的 AndExpression 表达式
     * @return 访问结果
     */
    R visitAnd(AndExpression<T> expression);

    /**
    * 访问 OrExpression 表达式。
    * @param expression 要访问的 OrExpression 表达式
    * @return 访问结果
    */
    R visitOr(OrExpression<T> expression);

    /**
     * 访问 NotExpression 表达式。
     * @param expression 要访问的 NotExpression 表达式
     * @return 访问结果
     */
    R visitNot(NotExpression<T> expression);
}
