package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.visitor.AbstractExpressionVisitor;

import java.lang.invoke.SerializedLambda;
import java.util.Map;

/**
 * 实体到 PO 转换访问器，将领域对象字段映射为 MyBatis-Plus 持久化对象。
 *
 * @author Jayden.Liang
 */
@SuppressWarnings("squid:S1602")
public class MybatisPlusEntityToPoVisitor <T,PO> extends AbstractExpressionVisitor<T, QueryWrapper<PO>> {


    private final QueryWrapper<PO> wrapper ;

    /**
     * Specification 的实体类型。方法引用指向继承自基类的 getter（如 {@code Role::getId}）时，
     * lambda 里只能看到声明类 {@code AbstractEntity}，注册表按具体实体注册，必须靠它定位映射。
     * 为 null 时退回按 lambda 声明类查找。
     */
    private final Class<?> domainClass;

    public MybatisPlusEntityToPoVisitor() {
        this(Wrappers.query(), null);
    }

    public MybatisPlusEntityToPoVisitor(QueryWrapper<PO> wrapper) {
        this(wrapper, null);
    }

    public MybatisPlusEntityToPoVisitor(Class<?> domainClass) {
        this(Wrappers.query(), domainClass);
    }

    public MybatisPlusEntityToPoVisitor(QueryWrapper<PO> wrapper, Class<?> domainClass) {
        super();
        this.wrapper = wrapper;
        this.domainClass = domainClass;
    }

    private String resolveColumn(java.io.Serializable lambda) {
        SerializedLambda serialize = MpFieldNameResolver.serialize(lambda);
        String property = MpFieldNameResolver.resolve(serialize);
        if (domainClass != null) {
            Map<String, String> mapping = FieldMappingRegistry.getFieldMapping(domainClass);
            if (mapping != null) {
                String column = mapping.get(property);
                if (column != null) {
                    return column;
                }
            }
        }
        return FieldMappingRegistry.getColumn(MpFieldNameResolver.getDomainClass(serialize), property);
    }

    private MybatisPlusEntityToPoVisitor<T, PO> nested(QueryWrapper<PO> innerWrapper) {
        return new MybatisPlusEntityToPoVisitor<>(innerWrapper, domainClass);
    }

    @Override
    public QueryWrapper<PO> visitAnd(AndExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(nested(left));
        });
        wrapper.nested(right->{
            expression.getRight().accept(nested(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<PO> visitOr(OrExpression<T> expression) {
        wrapper.nested(left -> {
            expression.getLeft().accept(nested(left));
        });
        wrapper.or();
        wrapper.nested(right->{
            expression.getRight().accept(nested(right));
        });
        return wrapper;
    }

    @Override
    public QueryWrapper<PO> visitNot(NotExpression<T> expression) {
        return wrapper.not(inner->expression.getExpression().accept(nested(inner)));
    }


    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitEqual(EqualExpression<T, R> expression) {
        return wrapper.eq(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitNotEqual(NotEqualExpression<T, R> expression) {
        return wrapper.ne(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitGt(GreaterThanExpression<T, R> expression) {
        return wrapper.gt(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitGte(GreaterThanOrEqualExpression<T, R> expression) {
        return wrapper.ge(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitLt(LessThanExpression<T, R> expression) {
        return wrapper.lt(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitLte(LessThanOrEqualExpression<T, R> expression) {
        return wrapper.le(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitIn(InExpression<T, R> expression) {
        return wrapper.in(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitNot(NotInExpression<T, R> expression) {
        return wrapper.notIn(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <R extends Comparable<R>> QueryWrapper<PO> visitBetween(BetweenExpression<T, R> expression) {
        return wrapper.between(resolveColumn(expression.getLeft()), expression.getLowerBound(), expression.getUpperBound());
    }

    @Override
    public QueryWrapper<PO> visitLike(LikeExpression<T> expression) {
        return wrapper.like(resolveColumn(expression.getLeft()), expression.getRight());
    }

    @Override
    public <V> QueryWrapper<PO> visitIsNull(IsNullExpression<T,V> expression) {
        return wrapper.isNull(resolveColumn(expression.getLeft()));
    }

    @Override
    public <V>  QueryWrapper<PO> visitIsNotNull(IsNotNullExpression<T, V> expression) {
        return wrapper.isNotNull(resolveColumn(expression.getLeft()));
    }

    @Override
    public <V extends Comparable<V>> QueryWrapper<PO> visitOrder(OrderExpression<T, V> expression) {
        wrapper.orderBy(true, expression.isAscending(), resolveColumn(expression.getLeft()));
        return wrapper;
    }

}
