package org.plain.specification.core.visitor;

import org.plain.specification.core.expression.*;

/**
 * Interface IComparisonExpressionVisitor.
 *
 * @author Jayden.Liang
 */
public interface IComparisonExpressionVisitor <T, R> {


    /**
     * 访问 EqualExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 EqualExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitEqual(EqualExpression<T, V> expression);

    /**
     * 访问 GreaterThanExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 GreaterThanExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitGt(GreaterThanExpression<T,V> expression);

    /**
     * 访问 LessThanExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 LessThanExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitLt(LessThanExpression<T,V> expression);

    /**
     * 访问 GreaterThanOrEqualExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 GreaterThanOrEqualExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitGte(GreaterThanOrEqualExpression<T,V> expression);

    /**
     * 访问 LessThanOrEqualExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 LessThanOrEqualExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitLte(LessThanOrEqualExpression<T,V> expression);

    /**
     * 访问 NotEqualExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 NotEqualExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitNotEqual(NotEqualExpression<T,V> expression);

    /**
     * 访问 InExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 InExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitIn(InExpression<T,V> expression);

    /**
     * 访问 NotInExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 NotInExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitNot(NotInExpression<T,V> expression);

    /**
     * 访问 BetweenExpression 表达式。
     * @param <V> 表达式的值类型
     * @param expression 要访问的 BetweenExpression 表达式
     * @return 访问结果
     */
    <V extends Comparable<V>> R visitBetween(BetweenExpression<T,V> expression);


}
