package org.plain.specification.core.builder;

import org.plain.specification.core.expression.SFunction;

import java.util.Arrays;

/**
 * 字段条件构建器，持有字段引用并提供类型安全的比较方法。
 * <p>
 * 通过 {@link org.plain.specification.core.Specification#where(SFunction)} 或
 * {@link QuickSpecificationBuilder#and(SFunction)} / {@link QuickSpecificationBuilder#or(SFunction)}
 * 获取实例。值类型 V 从字段引用自动推断，无需显式类型参数。
 * </p>
 * <p>
 * 用法示例：
 * <pre>
 * Specification.where(User::getName).eq("Alice")
 *     .and(User::getAge).gt(18)
 *     .or(User::getStatus).in("ACTIVE", "VIP")
 *     .orderBy(User::getAge)
 *     .build();
 * </pre>
 *
 * @param <T> 实体类型
 * @param <V> 字段值类型
 * @author Jayden.Liang
 */
public class FieldCondition<T, V extends Comparable<V>> {

    private final QuickSpecificationBuilder<T> builder;
    private final SFunction<T, V> field;
    private final boolean orCondition;

    public FieldCondition(QuickSpecificationBuilder<T> builder, SFunction<T, V> field, boolean orCondition) {
        this.builder = builder;
        this.field = field;
        this.orCondition = orCondition;
    }

    /**
     * 等于条件。
     *
     * @param value 比较值
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> eq(V value) {
        builder.applyEqual(field, value, orCondition);
        return builder;
    }

    /**
     * 不等于条件。
     *
     * @param value 比较值
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> neq(V value) {
        builder.applyNotEqual(field, value, orCondition);
        return builder;
    }

    /**
     * 大于条件。
     *
     * @param value 比较值
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> gt(V value) {
        builder.applyGreaterThan(field, value, orCondition);
        return builder;
    }

    /**
     * 大于等于条件。
     *
     * @param value 比较值
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> gte(V value) {
        builder.applyGreaterThanOrEqual(field, value, orCondition);
        return builder;
    }

    /**
     * 小于条件。
     *
     * @param value 比较值
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> lt(V value) {
        builder.applyLessThan(field, value, orCondition);
        return builder;
    }

    /**
     * 小于等于条件。
     *
     * @param value 比较值
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> lte(V value) {
        builder.applyLessThanOrEqual(field, value, orCondition);
        return builder;
    }

    /**
     * 区间条件。
     *
     * @param lower 下界
     * @param upper 上界
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> between(V lower, V upper) {
        builder.applyBetween(field, lower, upper, orCondition);
        return builder;
    }

    /**
     * 模糊匹配条件（仅适用于 String 字段）。
     * <p>
     * 注意：由于 Java 泛型限制，此方法在非 String 字段上也能编译通过，
     * 但运行时会抛出 {@link ClassCastException}。请确保仅在 String 类型字段上调用。
     * </p>
     *
     * @param pattern 匹配模式（支持 % 和 _ 通配符）
     * @return 构建器实例
     * @throws ClassCastException 如果字段类型不是 String
     */
    @SuppressWarnings("unchecked")
    public QuickSpecificationBuilder<T> like(String pattern) {
        builder.applyLike((SFunction<T, String>) field, pattern, orCondition);
        return builder;
    }

    /**
     * 包含条件。
     *
     * @param values 值集合
     * @return 构建器实例
     */
    @SafeVarargs
    public final QuickSpecificationBuilder<T> in(V... values) {
        builder.applyIn(field, Arrays.asList(values), orCondition);
        return builder;
    }

    /**
     * 空值条件。
     *
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> isNull() {
        builder.applyIsNull(field, orCondition);
        return builder;
    }

    /**
     * 非空值条件。
     *
     * @return 构建器实例
     */
    public QuickSpecificationBuilder<T> isNotNull() {
        builder.applyIsNotNull(field, orCondition);
        return builder;
    }
}
