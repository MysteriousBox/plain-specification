package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.visitor.AbstractExpressionVisitor;
import org.plain.utils.converter.IConverter;

import java.lang.invoke.SerializedLambda;
import java.util.Comparator;

public class MybatisplusEntityToPoVisitor <T,PO> extends AbstractExpressionVisitor<T, QueryWrapper<PO>> {


    private final QueryWrapper<PO> wrapper ;

    public MybatisplusEntityToPoVisitor() {
        super();

        wrapper = Wrappers.query();
    }

    public MybatisplusEntityToPoVisitor(QueryWrapper<PO> wrapper) {
        super();
        this.wrapper = wrapper;
    }


    @Override
    public QueryWrapper<PO> visitAnd(AndExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(new MybatisplusEntityToPoVisitor<>(left));
        });
        wrapper.nested(right->{
            expression.getRight().accept(new MybatisplusEntityToPoVisitor<>(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<PO> visitOr(OrExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(new MybatisplusEntityToPoVisitor<>(left));
        });
        wrapper.or();
        wrapper.nested(right->{
            expression.getRight().accept(new MybatisplusEntityToPoVisitor<>(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<PO> visitNot(NotExpression<T> expression) {
        return wrapper.not(inner->expression.getExpression().accept(new MybatisplusEntityToPoVisitor<>(inner)));
    }


    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitEqual(EqualExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.eq(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitNotEqual(NotEqualExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.ne(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitGt(GreaterThanExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.gt(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitGte(GreaterThanOrEqualExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.ge(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitLt(LessThanExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.lt(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitLte(LessThanOrEqualExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.le(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitIn(InExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.in(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitNot(NotInExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.notIn(column, expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitBetween(BetweenExpression<T, R> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.between(column, expression.getLowerBound(), expression.getUpperBound());
    }

    @Override
    public QueryWrapper<PO> visitLike(LikeExpression<T> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.like(column, expression.getRight());
    }

    @Override
    public <V> QueryWrapper<PO> visitIsNull(IsNullExpression<T,V> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.isNull(column);
    }

    @Override
    public <V>  QueryWrapper<PO> visitIsNotNull(IsNotNullExpression<T, V> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        return wrapper.isNotNull(column);
    }

    @Override
    public <V extends Comparable<V>> QueryWrapper<PO> visitOrder(OrderExpression<T, V> expression) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(expression.getLeft());
        String column = FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), MpFieldNameResolver.resolve(serialize));
        Comparator<V> comparator = expression.getComparator();
        boolean isAsc = !isReverseOrder(comparator);
        wrapper.orderBy( true,isAsc,column);
        return wrapper;
    }

}
