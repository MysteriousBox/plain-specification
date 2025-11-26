package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.visitor.AbstractExpressionVisitor;

import java.util.Comparator;

public class MybatisplusExpressionVisitor<T> extends AbstractExpressionVisitor<T, QueryWrapper<T>> {

//    private Class<T> entityClass;
    private final QueryWrapper<T> wrapper ;

    public MybatisplusExpressionVisitor() {
        super();
        wrapper = Wrappers.query();
    }

    public MybatisplusExpressionVisitor(QueryWrapper<T> wrapper) {
        super();
        this.wrapper = wrapper;
    }


    @Override
    public QueryWrapper<T> visitAnd(AndExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(new MybatisplusExpressionVisitor<>(left));
        });
        wrapper.nested(right->{
            expression.getRight().accept(new MybatisplusExpressionVisitor<>(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<T> visitOr(OrExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(new MybatisplusExpressionVisitor<>(left));
        });
        wrapper.or();
        wrapper.nested(right->{
            expression.getRight().accept(new MybatisplusExpressionVisitor<>(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<T> visitNot(NotExpression<T> expression) {
        return wrapper.not(inner->expression.getExpression().accept(new MybatisplusExpressionVisitor<>(inner)));
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
        Comparator<V> comparator = expression.getComparator();
        boolean isAsc = !isReverseOrder(comparator);
        wrapper.orderBy( true,isAsc, MpFieldNameResolver.resolve(expression.getLeft()));
        return wrapper;
    }

    private static <V extends Comparable<V>> boolean isReverseOrder(Comparator<V> comparator) {
        if (comparator == null) return false;

        V a = (V) "a"; // 注意：仅用于测试，要求类型支持字符串比较
        V b = (V) "b";

        int expected = Comparator.<V>naturalOrder().reversed().compare(a, b);
        int actual = comparator.compare(a, b);

        return actual == expected;
    }
}
