package org.plain.core.visitor;

import org.plain.core.expression.*;

public interface IComparisonExpressionVisitor <T, R> {


    <V extends Comparable<V>> R visitEqual(EqualExpression<T, V> expression);

    <V extends Comparable<V>> R visitGt(GreaterThanExpression<T,V> expression);

    <V extends Comparable<V>> R visitLt(LessThanExpression<T,V> expression);

    <V extends Comparable<V>> R visitGte(GreaterThanOrEqualExpression<T,V> expression);

    <V extends Comparable<V>> R visitLte(LessThanOrEqualExpression<T,V> expression);

    <V extends Comparable<V>> R visitNotEqual(NotEqualExpression<T,V> expression);

    <V extends Comparable<V>> R visitIn(InExpression<T,V> expression);

    <V extends Comparable<V>> R visitNot(NotInExpression<T,V> expression);

    <V extends Comparable<V>> R visitBetween(BetweenExpression<T,V> expression);

}
