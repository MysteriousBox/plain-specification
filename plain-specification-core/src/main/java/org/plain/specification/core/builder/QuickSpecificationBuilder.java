package org.plain.specification.core.builder;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.expression.SFunction;

import java.util.Collection;

/**
 * 快捷查询构建器，支持字段优先链式调用。
 * <p>
 * 通过 {@link FieldCondition} 提供类型安全的比较方法，值类型从字段引用自动推断。
 * </p>
 * <p>
 * 用法示例：
 * <pre>
 * Specification.where(User::getName).eq("Alice")
 *     .and(User::getAge).gt(18)
 *     .or(User::getStatus).in("ACTIVE", "VIP")
 *     .orderBy(User::getAge)
 *     .thenByDescending(User::getName)
 *     .build();
 * </pre>
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 */
public class QuickSpecificationBuilder<T> {

    private final Specification<T> spec;
    private Expressions<T> currentExpr;
    private boolean hasWhere;
    private IOrderedSpecificationBuilder<T> orderedBuilder;

    public QuickSpecificationBuilder(Specification<T> spec) {
        this.spec = spec;
        this.currentExpr = Expressions.create();
        this.hasWhere = false;
    }

    /**
     * 添加 AND 条件，返回字段条件构建器。
     *
     * @param field 字段引用
     * @param <V>   值类型
     * @return 字段条件构建器
     */
    public <V extends Comparable<V>> FieldCondition<T, V> and(SFunction<T, V> field) {
        return new FieldCondition<>(this, field, false);
    }

    /**
     * 添加 OR 条件，返回字段条件构建器。
     *
     * @param field 字段引用
     * @param <V>   值类型
     * @return 字段条件构建器
     */
    public <V extends Comparable<V>> FieldCondition<T, V> or(SFunction<T, V> field) {
        return new FieldCondition<>(this, field, true);
    }

    /**
     * 添加升序排序。
     *
     * @param field 字段引用
     * @param <V>   值类型
     * @return 构建器实例
     */
    public <V extends Comparable<V>> QuickSpecificationBuilder<T> orderBy(SFunction<T, V> field) {
        orderedBuilder = spec.query().orderBy(Expressions.<T>create().orderBy(field));
        return this;
    }

    /**
     * 添加降序排序。
     *
     * @param field 字段引用
     * @param <V>   值类型
     * @return 构建器实例
     */
    public <V extends Comparable<V>> QuickSpecificationBuilder<T> orderByDescending(
            SFunction<T, V> field) {
        orderedBuilder = spec.query().orderByDescending(Expressions.<T>create().orderByDescending(field));
        return this;
    }

    /**
     * 添加二级升序排序（必须在 orderBy/orderByDescending 之后调用）。
     *
     * @param field 字段引用
     * @param <V>   值类型
     * @return 构建器实例
     * @throws IllegalStateException 如果未在 orderBy/orderByDescending 之后调用
     */
    public <V extends Comparable<V>> QuickSpecificationBuilder<T> thenBy(SFunction<T, V> field) {
        ensureOrdered();
        orderedBuilder.thenBy(Expressions.<T>create().orderBy(field));
        return this;
    }

    /**
     * 添加二级降序排序（必须在 orderBy/orderByDescending 之后调用）。
     *
     * @param field 字段引用
     * @param <V>   值类型
     * @return 构建器实例
     * @throws IllegalStateException 如果未在 orderBy/orderByDescending 之后调用
     */
    public <V extends Comparable<V>> QuickSpecificationBuilder<T> thenByDescending(
            SFunction<T, V> field) {
        ensureOrdered();
        orderedBuilder.thenByDescending(Expressions.<T>create().orderByDescending(field));
        return this;
    }

    /**
     * 构建并返回 Specification。
     *
     * @return ISpecification 实例
     */
    public ISpecification<T> build() {
        flush();
        return spec;
    }

    // --- Package-private apply methods for FieldCondition ---

    <V extends Comparable<V>> void applyEqual(SFunction<T, V> field, V value, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.equal(field, value);
    }

    <V extends Comparable<V>> void applyNotEqual(SFunction<T, V> field, V value, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.notEqual(field, value);
    }

    <V extends Comparable<V>> void applyGreaterThan(SFunction<T, V> field, V value, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.greaterThan(field, value);
    }

    <V extends Comparable<V>> void applyGreaterThanOrEqual(SFunction<T, V> field, V value, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.greaterThanOrEqual(field, value);
    }

    <V extends Comparable<V>> void applyLessThan(SFunction<T, V> field, V value, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.lessThan(field, value);
    }

    <V extends Comparable<V>> void applyLessThanOrEqual(SFunction<T, V> field, V value, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.lessThanOrEqual(field, value);
    }

    <V extends Comparable<V>> void applyBetween(SFunction<T, V> field, V lower, V upper, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.between(field, lower, upper);
    }

    void applyLike(SFunction<T, String> field, String pattern, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.like(field, pattern);
    }

    <V extends Comparable<V>> void applyIn(SFunction<T, V> field, Collection<V> values, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.in(field, values);
    }

    <V extends Comparable<V>> void applyIsNull(SFunction<T, V> field, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.isNull(field);
    }

    <V extends Comparable<V>> void applyIsNotNull(SFunction<T, V> field, boolean isOr) {
        prepareExpression(isOr);
        currentExpr.notNull(field);
    }

    // --- Private helpers ---

    private void prepareExpression(boolean isOr) {
        if (isOr && hasWhere) {
            currentExpr.or();
        }
        hasWhere = true;
    }

    private void flush() {
        if (hasWhere) {
            spec.query().where(currentExpr);
            currentExpr = Expressions.create();
            hasWhere = false;
        }
    }

    private void ensureOrdered() {
        if (orderedBuilder == null) {
            throw new IllegalStateException("thenBy/thenByDescending must be called after orderBy/orderByDescending");
        }
    }
}
