package org.plain.specification.redis;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.expression.OrderExpression;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.expression.SFunction;
import org.plain.specification.core.visitor.AbstractExpressionVisitor;
import org.plain.specification.redis.pushdown.FieldMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What a specification asks for in terms of row order, translated into something the ZSET index can
 * act on.
 * <p>
 * The library's own ordering semantics are the comparators built from the order expressions (the
 * same ones {@code OrderEvaluator} applies when filtering in memory), so those comparators are the
 * reference. The index can stand in for them only when the requested ordering is exactly the score
 * ordering, which needs the repository to declare which property its score carries
 * ({@code RedisRepositoryConfig#getScoreField()}); otherwise the rows are read in index order and
 * sorted in memory.
 */
final class RedisRowOrder<T> {

    private final Comparator<T> comparator;
    private final boolean indexOrderIsRequested;
    private final boolean descending;

    private RedisRowOrder(Comparator<T> comparator, boolean indexOrderIsRequested, boolean descending) {
        this.comparator = comparator;
        this.indexOrderIsRequested = indexOrderIsRequested;
        this.descending = descending;
    }

    /**
     * @param specification the query being served
     * @param scoreField    property whose value the ZSET score carries, or {@code null}
     * @return the ordering plan, never {@code null}
     */
    static <T> RedisRowOrder<T> of(ISpecification<T> specification, String scoreField) {
        final List<OrderExpressionInfo<T>> requested = new ArrayList<OrderExpressionInfo<T>>();
        for (OrderExpressionInfo<T> info : specification.getOrderExpressions()) {
            requested.add(info);
        }
        if (requested.isEmpty()) {
            return new RedisRowOrder<T>(null, true, false);
        }
        Comparator<T> composite = null;
        for (OrderExpressionInfo<T> info : requested) {
            final Comparator<T> next = info.getKeySelectorFunc();
            composite = composite == null ? next : composite.thenComparing(next);
        }
        if (scoreField != null && requested.size() == 1) {
            final OrderFieldProbe<T> probe = probe(requested.get(0));
            if (probe.isScoreFieldOrder(scoreField)) {
                return new RedisRowOrder<T>(composite, true, !probe.ascending());
            }
        }
        return new RedisRowOrder<T>(composite, false, false);
    }

    /**
     * @return true when reading the index in {@link #descending()} order already yields the
     *         requested order, so no in-memory sort is needed and pagination can slice directly
     */
    boolean indexOrderIsRequested() {
        return indexOrderIsRequested;
    }

    /**
     * @return true when the index has to be walked high-score first
     */
    boolean descending() {
        return descending;
    }

    /**
     * @return the rows in the order the specification asks for
     */
    List<T> applyTo(List<T> rows) {
        if (indexOrderIsRequested || comparator == null) {
            return rows;
        }
        final List<T> sorted = new ArrayList<T>(rows);
        sorted.sort(comparator);
        return sorted;
    }

    private static <T> OrderFieldProbe<T> probe(OrderExpressionInfo<T> info) {
        final OrderFieldProbe<T> probe = new OrderFieldProbe<T>();
        try {
            info.func(probe);
        } catch (RuntimeException ex) {
            return probe;
        }
        return probe;
    }

    /** Records the field reference of the order expression it visits. */
    private static final class OrderFieldProbe<T> extends AbstractExpressionVisitor<T, Boolean> {

        private SFunction<T, ?> field;
        private int expressionCount;
        private boolean ascending = true;

        @Override
        public <V extends Comparable<V>> Boolean visitOrder(OrderExpression<T, V> expression) {
            this.field = expression.getLeft();
            this.ascending = expression.isAscending();
            this.expressionCount++;
            return Boolean.TRUE;
        }

        /**
         * The direction is taken from the expression itself, not from the specification's order type:
         * the comparator the library sorts with is derived from the same expression, so the index walk
         * cannot diverge from it.
         *
         * @return true unless the expression asks for high values first
         */
        boolean ascending() {
            return ascending;
        }

        /**
         * @param scoreField property the repository declares its score to carry
         * @return true when exactly one numeric order key was seen and it is that property
         */
        boolean isScoreFieldOrder(String scoreField) {
            if (field == null || expressionCount != 1 || !scoreField.equals(FieldMeta.of(field).fieldName())) {
                return false;
            }
            // a score is a double, so it can only carry the order of a numeric property
            return FieldMeta.of(field).kind() == FieldMeta.Kind.INTEGER;
        }
    }
}
