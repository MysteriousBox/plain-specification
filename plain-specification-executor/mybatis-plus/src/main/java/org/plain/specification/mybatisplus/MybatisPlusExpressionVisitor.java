package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.visitor.AbstractExpressionVisitor;

/**
 * MyBatis-Plus QueryWrapper 表达式访问器，将规格表达式转换为 QueryWrapper 条件。
 *
 * @author Jayden.Liang
 */


@SuppressWarnings("squid:S1602")
public class MybatisPlusExpressionVisitor<T> extends AbstractExpressionVisitor<T, QueryWrapper<T>> {

    /**
     * MyBatis-Plus query wrapper.
     */
    private final QueryWrapper<T> wrapper ;

    public MybatisPlusExpressionVisitor() {
        super();
        wrapper = Wrappers.query();
    }

    public MybatisPlusExpressionVisitor(QueryWrapper<T> wrapper) {
        super();
        this.wrapper = wrapper;
    }


    @Override
    public QueryWrapper<T> visitAnd(AndExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(new MybatisPlusExpressionVisitor<>(left));
        });
        wrapper.nested(right->{
            expression.getRight().accept(new MybatisPlusExpressionVisitor<>(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<T> visitOr(OrExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(new MybatisPlusExpressionVisitor<>(left));
        });
        wrapper.or();
        wrapper.nested(right->{
            expression.getRight().accept(new MybatisPlusExpressionVisitor<>(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<T> visitNot(NotExpression<T> expression) {
        return wrapper.not(inner->expression.getExpression().accept(new MybatisPlusExpressionVisitor<>(inner)));
    }


    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitEqual(EqualExpression<T, R> expression) {
        return wrapper.eq(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitNotEqual(NotEqualExpression<T, R> expression) {

        return wrapper.ne(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitGt(GreaterThanExpression<T, R> expression) {

        return wrapper.gt(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitGte(GreaterThanOrEqualExpression<T, R> expression) {

        return wrapper.ge(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitLt(LessThanExpression<T, R> expression) {

        return wrapper.lt(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitLte(LessThanOrEqualExpression<T, R> expression) {

        return wrapper.le(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitIn(InExpression<T, R> expression) {
        return wrapper.in(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitNot(NotInExpression<T, R> expression) {
        return wrapper.notIn(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<T> visitBetween(BetweenExpression<T, R> expression) {

        return wrapper.between(MpFieldNameResolver.resolve(expression.getLeft()), expression.getLowerBound(), expression.getUpperBound());
    }

    @Override
    public QueryWrapper<T> visitLike(LikeExpression<T> expression) {
        return wrapper.like(MpFieldNameResolver.resolve(expression.getLeft()), expression.getRight());
    }

    @Override
    public <V> QueryWrapper<T> visitIsNull(IsNullExpression<T,V> expression) {
        return wrapper.isNull(MpFieldNameResolver.resolve(expression.getLeft()));
    }

    @Override
    public <V>  QueryWrapper<T> visitIsNotNull(IsNotNullExpression<T, V> expression) {
        return wrapper.isNotNull(MpFieldNameResolver.resolve(expression.getLeft()));
    }


    @Override
    public <V extends Comparable<V>> QueryWrapper<T> visitOrder(OrderExpression<T, V> expression) {
        wrapper.orderBy(true, expression.isAscending(), MpFieldNameResolver.resolve(expression.getLeft()));
        return wrapper;
    }
}
