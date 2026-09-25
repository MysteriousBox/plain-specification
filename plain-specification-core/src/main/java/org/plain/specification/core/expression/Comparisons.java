package org.plain.specification.core.expression;

import java.util.Arrays;
import java.util.Collection;

/**
 * 比较操作符辅助方法，支持静态导入。
 * <p>
 * 推荐使用字段优先链式 API（参见 {@link org.plain.specification.core.Specification#where}）：
 * <pre>
 * Specification.where(User::getAge).gt(18)
 * Specification.where(User::getName).like("%Alice%")
 * Specification.where(User::getRole).in("ADMIN", "MANAGER")
 * </pre>
 * <p>
 * 本类提供独立的比较操作符封装，可用于自定义构建场景。
 *
 * @author Jayden.Liang
 */
public final class Comparisons {

    private Comparisons() {
    }

    public static <V> Comparison<V> eq(V value) {
        return new Comparison<>("eq", value);
    }

    public static <V> Comparison<V> neq(V value) {
        return new Comparison<>("neq", value);
    }

    public static <V extends Comparable<V>> Comparison<V> gt(V value) {
        return new Comparison<>("gt", value);
    }

    public static <V extends Comparable<V>> Comparison<V> gte(V value) {
        return new Comparison<>("gte", value);
    }

    public static <V extends Comparable<V>> Comparison<V> lt(V value) {
        return new Comparison<>("lt", value);
    }

    public static <V extends Comparable<V>> Comparison<V> lte(V value) {
        return new Comparison<>("lte", value);
    }

    public static <V extends Comparable<V>> BetweenComparison<V> between(V lower, V upper) {
        return new BetweenComparison<>(lower, upper);
    }

    public static Comparison<String> like(String pattern) {
        return new Comparison<>("like", pattern);
    }

    @SafeVarargs
    public static <V> Comparison<Collection<V>> in(V... values) {
        return new Comparison<>("in", Arrays.asList(values));
    }

    public static <V> Comparison<Collection<V>> in(Collection<V> values) {
        return new Comparison<>("in", values);
    }

    public static <V> Comparison<V> isNull() {
        return new Comparison<>("isNull", null);
    }

    public static <V> Comparison<V> isNotNull() {
        return new Comparison<>("isNotNull", null);
    }

    /**
     * 比较操作符封装。
     *
     * @param <V> 值类型
     * @author Jayden.Liang
     */
    public static class Comparison<V> {
        private final String operator;
        private final V value;

        Comparison(String operator, V value) {
            this.operator = operator;
            this.value = value;
        }

        public String getOperator() {
            return operator;
        }

        public V getValue() {
            return value;
        }
    }

    /**
     * Between 比较操作符封装。
     *
     * @param <V> 值类型
     * @author Jayden.Liang
     */
    public static class BetweenComparison<V extends Comparable<V>> extends Comparison<V> {
        private final V lower;
        private final V upper;

        BetweenComparison(V lower, V upper) {
            super("between", lower);
            this.lower = lower;
            this.upper = upper;
        }

        public V getLower() {
            return lower;
        }

        public V getUpper() {
            return upper;
        }
    }
}
