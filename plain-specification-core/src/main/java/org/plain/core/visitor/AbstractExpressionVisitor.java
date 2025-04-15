package org.plain.core.visitor;

import org.plain.core.expression.*;

import java.util.Collection;
import java.util.function.Predicate;

public abstract class AbstractExpressionVisitor<T,R> implements IExpressionVisitor<T,R> {

    @Override
    public  R visitAnd(AndExpression<T> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitOr(OrExpression<T> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitNot(NotExpression<T> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitEqual(EqualExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitGt(GreaterThanExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitLt(LessThanExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitGte(GreaterThanOrEqualExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitLte(LessThanOrEqualExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitNotEqual(NotEqualExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitIn(InExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitNot(NotInExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visitBetween(BetweenExpression<T, V> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitLike(LikeExpression<T> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitIsNull(IsNullExpression<T> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public R visitIsNotNull(IsNotNullExpression<T> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <E> R visitExists(ExistsExpression<T, E> expression) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <E> R visitNotExists(NotExistsExpression<T, E> expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends Comparable<V>> R visit(OrderExpression<T, V> expression) {
        return null;
    }


}
