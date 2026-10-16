package org.plain.specification.redis.pushdown;

import org.plain.specification.core.expression.AndExpression;
import org.plain.specification.core.expression.BetweenExpression;
import org.plain.specification.core.expression.EqualExpression;
import org.plain.specification.core.expression.ExistsExpression;
import org.plain.specification.core.expression.GreaterThanExpression;
import org.plain.specification.core.expression.GreaterThanOrEqualExpression;
import org.plain.specification.core.expression.IExpression;
import org.plain.specification.core.expression.InExpression;
import org.plain.specification.core.expression.IsNullExpression;
import org.plain.specification.core.expression.IsNotNullExpression;
import org.plain.specification.core.expression.LessThanExpression;
import org.plain.specification.core.expression.LessThanOrEqualExpression;
import org.plain.specification.core.expression.LikeExpression;
import org.plain.specification.core.expression.NotEqualExpression;
import org.plain.specification.core.expression.NotExpression;
import org.plain.specification.core.expression.NotExistsExpression;
import org.plain.specification.core.expression.NotInExpression;
import org.plain.specification.core.expression.OrderExpression;
import org.plain.specification.core.expression.OrExpression;
import org.plain.specification.core.expression.SFunction;
import org.plain.specification.core.visitor.AbstractExpressionVisitor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compiles an expression tree into a {@link NarrowPlan}: the conservative, server-side decidable
 * shadow of the predicate that the Redis scan script evaluates against raw HASH strings.
 * <p>
 * The plan is a <em>necessary</em> condition, never a sufficient one. Anything Redis cannot decide
 * stays, and survivors are re-checked in memory against the original specification. A leaf whose
 * value domain Redis cannot compare faithfully (floating point, temporal, nested objects,
 * collections, negations) compiles to {@link NarrowPlan#trivial()} instead of an approximation.
 */
public class RedisNarrowingVisitor<T> extends AbstractExpressionVisitor<T, NarrowPlan> {

    private static final char LIKE_WILDCARD = '%';
    private static final char LIKE_SINGLE_WILDCARD = '_';
    private static final char LIKE_ESCAPE = '\\';

    private static final Set<Class<?>> NARROWED = Collections.unmodifiableSet(
            new HashSet<Class<?>>(Arrays.<Class<?>>asList(
                    AndExpression.class, OrExpression.class,
                    EqualExpression.class, NotEqualExpression.class,
                    GreaterThanExpression.class, GreaterThanOrEqualExpression.class,
                    LessThanExpression.class, LessThanOrEqualExpression.class,
                    BetweenExpression.class, InExpression.class, NotInExpression.class,
                    LikeExpression.class, IsNullExpression.class)));

    /**
     * The expression types this compiler can turn into a server-side condition.
     * <p>
     * {@code NotExpression}, {@code IsNotNullExpression}, {@code ExistsExpression} and
     * {@code NotExistsExpression} are deliberately absent: none of them can be given a sound
     * necessary condition over raw HASH strings, so they never claim backend support.
     *
     * @param <T> entity type
     * @return the narrowed expression types
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> Set<Class<? extends IExpression<T>>> narrowedExpressionTypes() {
        return (Set) NARROWED;
    }

    @Override
    public NarrowPlan visitAnd(AndExpression<T> expression) {
        return NarrowPlan.and(expression.getLeft().accept(this), expression.getRight().accept(this));
    }

    @Override
    public NarrowPlan visitOr(OrExpression<T> expression) {
        return NarrowPlan.or(expression.getLeft().accept(this), expression.getRight().accept(this));
    }

    @Override
    public NarrowPlan visitNot(NotExpression<T> expression) {
        // The negation of a necessary condition is not a necessary condition: !NC(x) would drop
        // entries the server could not decide but which are actually false in memory.
        return NarrowPlan.trivial();
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitEqual(EqualExpression<T, V> expression) {
        if (expression.getRight() == null) {
            return nullable(expression.getLeft());
        }
        return comparison(expression.getLeft(), Cond.Op.INT_EQ, Cond.Op.STR_EQ, expression.getRight());
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitNotEqual(NotEqualExpression<T, V> expression) {
        if (expression.getRight() == null) {
            // In memory "x != null" is false for every entity; over-keeping is the safe answer and
            // the verifier produces the empty result.
            return NarrowPlan.trivial();
        }
        return comparison(expression.getLeft(), Cond.Op.INT_NE, Cond.Op.STR_NE, expression.getRight());
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitGt(GreaterThanExpression<T, V> expression) {
        return integerOnly(expression.getLeft(), Cond.Op.INT_GT, expression.getRight());
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitGte(GreaterThanOrEqualExpression<T, V> expression) {
        return integerOnly(expression.getLeft(), Cond.Op.INT_GE, expression.getRight());
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitLt(LessThanExpression<T, V> expression) {
        return integerOnly(expression.getLeft(), Cond.Op.INT_LT, expression.getRight());
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitLte(LessThanOrEqualExpression<T, V> expression) {
        return integerOnly(expression.getLeft(), Cond.Op.INT_LE, expression.getRight());
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitIn(InExpression<T, V> expression) {
        return membership(expression.getLeft(), Cond.Op.INT_IN, Cond.Op.STR_IN, expression.getRight());
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitNot(NotInExpression<T, V> expression) {
        final FieldMeta meta = FieldMeta.of(expression.getLeft());
        final NarrowPlan notIn = membership(expression.getLeft(), Cond.Op.INT_NOTIN, Cond.Op.STR_NOTIN,
                expression.getRight());
        if (notIn.isTrivial() || !meta.resolvable()) {
            return notIn;
        }
        // In memory "not in" also matches a null value, which the value opcode would drop.
        return NarrowPlan.or(NarrowPlan.of(Cond.presence(Cond.Op.NULLABLE, meta.fieldName())), notIn);
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitBetween(BetweenExpression<T, V> expression) {
        final FieldMeta meta = FieldMeta.of(expression.getLeft());
        if (meta.kind() != FieldMeta.Kind.INTEGER) {
            return NarrowPlan.trivial();
        }
        final String lower = canonicalInteger(expression.getLowerBound());
        final String upper = canonicalInteger(expression.getUpperBound());
        if (lower == null || upper == null) {
            return NarrowPlan.trivial();
        }
        final List<Cond> conds = new ArrayList<Cond>(2);
        conds.add(Cond.of(Cond.Op.INT_GE, meta.fieldName(), lower));
        conds.add(Cond.of(Cond.Op.INT_LE, meta.fieldName(), upper));
        return NarrowPlan.conjunction(conds);
    }

    @Override
    public NarrowPlan visitLike(LikeExpression<T> expression) {
        final FieldMeta meta = FieldMeta.of(expression.getLeft());
        if (meta.kind() != FieldMeta.Kind.STRING) {
            return NarrowPlan.trivial();
        }
        final List<Cond> conds = likeNecessaryConditions(meta.fieldName(), expression.getRight());
        if (conds.isEmpty()) {
            return NarrowPlan.trivial();
        }
        return NarrowPlan.conjunction(conds);
    }

    @Override
    public <V> NarrowPlan visitIsNull(IsNullExpression<T, V> expression) {
        return nullable(expression.getLeft());
    }

    @Override
    public <V> NarrowPlan visitIsNotNull(IsNotNullExpression<T, V> expression) {
        // "field absent" does not imply the getter returns null: a derived getter has no backing
        // field at all, and a primitive-backed getter rebuilds to a non-null default.
        return NarrowPlan.trivial();
    }

    @Override
    public <E> NarrowPlan visitExists(ExistsExpression<T, E> expression) {
        return NarrowPlan.trivial();
    }

    @Override
    public <E> NarrowPlan visitNotExists(NotExistsExpression<T, E> expression) {
        return NarrowPlan.trivial();
    }

    @Override
    public <V extends Comparable<V>> NarrowPlan visitOrder(OrderExpression<T, V> expression) {
        // Ordering is served by the ZSET index itself; there is nothing to narrow.
        return NarrowPlan.trivial();
    }

    // ------------------------------------------------------------------
    // leaf construction
    // ------------------------------------------------------------------

    private NarrowPlan nullable(SFunction<T, ?> left) {
        final FieldMeta meta = FieldMeta.of(left);
        if (!meta.resolvable()) {
            return NarrowPlan.trivial();
        }
        return NarrowPlan.of(Cond.presence(Cond.Op.NULLABLE, meta.fieldName()));
    }

    private <V extends Comparable<V>> NarrowPlan comparison(SFunction<T, V> left,
                                                            Cond.Op integerOp,
                                                            Cond.Op stringOp,
                                                            V right) {
        final FieldMeta meta = FieldMeta.of(left);
        final String operand = storedForm(meta, right);
        if (operand == null) {
            return NarrowPlan.trivial();
        }
        final Cond.Op op = meta.kind() == FieldMeta.Kind.STRING ? stringOp : integerOp;
        return NarrowPlan.of(Cond.of(op, meta.fieldName(), operand));
    }

    private <V extends Comparable<V>> NarrowPlan integerOnly(SFunction<T, V> left, Cond.Op op, V right) {
        final FieldMeta meta = FieldMeta.of(left);
        if (meta.kind() != FieldMeta.Kind.INTEGER) {
            return NarrowPlan.trivial();
        }
        final String operand = canonicalInteger(right);
        if (operand == null) {
            return NarrowPlan.trivial();
        }
        return NarrowPlan.of(Cond.of(op, meta.fieldName(), operand));
    }

    private <V extends Comparable<V>> NarrowPlan membership(SFunction<T, V> left,
                                                            Cond.Op integerOp,
                                                            Cond.Op stringOp,
                                                            Collection<V> values) {
        final FieldMeta meta = FieldMeta.of(left);
        if (!meta.pushable() || values == null) {
            return NarrowPlan.trivial();
        }
        final List<String> operands = new ArrayList<String>(values.size());
        for (V value : values) {
            if (value == null) {
                // A null element never matches a non-null value in memory, so it can be dropped.
                continue;
            }
            final String stored = storedForm(meta, value);
            if (stored == null) {
                return NarrowPlan.trivial();
            }
            operands.add(stored);
        }
        final Cond.Op op = meta.kind() == FieldMeta.Kind.STRING ? stringOp : integerOp;
        return NarrowPlan.of(Cond.of(op, meta.fieldName(), operands));
    }

    /**
     * @param meta  resolved field
     * @param value comparison operand
     * @return the canonical HASH string for {@code value}, or {@code null} when this field kind
     *         cannot be compared from its stored representation
     */
    private static String storedForm(FieldMeta meta, Object value) {
        if (value == null || !meta.pushable()) {
            return null;
        }
        if (meta.kind() == FieldMeta.Kind.STRING) {
            return value instanceof String ? (String) value : null;
        }
        return canonicalInteger(value);
    }

    /**
     * @param value comparison operand
     * @return {@code String.valueOf} of an integral value, or {@code null} for every other domain
     */
    private static String canonicalInteger(Object value) {
        if (value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            return Long.toString(((Number) value).longValue());
        }
        if (value instanceof BigInteger) {
            final String text = value.toString();
            return isCanonicalInteger(text) ? text : null;
        }
        return null;
    }

    private static boolean isCanonicalInteger(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        int i = 0;
        if (text.charAt(0) == '-') {
            if (text.length() == 1) {
                return false;
            }
            i = 1;
        }
        if (text.charAt(i) == '0' && text.length() - i > 1) {
            return false;
        }
        for (; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Build the necessary conditions of a SQL-LIKE pattern: every literal run between wildcards has
     * to appear in the value, and an unanchored side has to match as a prefix/suffix. The in-memory
     * verifier still applies the real regex, so this only ever over-approximates.
     *
     * @param fieldName HASH field name
     * @param pattern   SQL LIKE pattern
     * @return conditions, empty when the pattern cannot be narrowed soundly
     */
    private static List<Cond> likeNecessaryConditions(String fieldName, String pattern) {
        final List<Cond> conds = new ArrayList<Cond>();
        if (fieldName == null || pattern == null || pattern.isEmpty()
                || pattern.indexOf(LIKE_SINGLE_WILDCARD) >= 0 || pattern.indexOf(LIKE_ESCAPE) >= 0) {
            return conds;
        }
        final boolean anchoredStart = pattern.charAt(0) != LIKE_WILDCARD;
        final boolean anchoredEnd = pattern.charAt(pattern.length() - 1) != LIKE_WILDCARD;
        int literalStart = 0;
        for (int i = 0; i <= pattern.length(); i++) {
            if (i < pattern.length() && pattern.charAt(i) != LIKE_WILDCARD) {
                continue;
            }
            if (i > literalStart) {
                final String literal = pattern.substring(literalStart, i);
                final boolean coveredByPrefix = literalStart == 0 && anchoredStart;
                final boolean coveredBySuffix = i == pattern.length() && anchoredEnd;
                if (coveredByPrefix) {
                    conds.add(Cond.of(Cond.Op.STR_PREFIX, fieldName, literal));
                }
                if (!coveredByPrefix && !coveredBySuffix) {
                    conds.add(Cond.of(Cond.Op.STR_CONTAINS, fieldName, literal));
                }
                if (coveredBySuffix) {
                    conds.add(Cond.of(Cond.Op.STR_SUFFIX, fieldName, literal));
                }
            }
            literalStart = i + 1;
        }
        return conds;
    }
}
