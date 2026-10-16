package org.plain.specification.redis.pushdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Compiled server-side narrowing: a disjunction of conjunctions of {@link Cond} conditions.
 * <p>
 * Composition follows the soundness rules of necessary conditions:
 * <ul>
 *   <li>{@code AND(a, b)} — a match must satisfy both, so the narrowing is the conjunction;
 *       expressed as the cartesian product of the two disjunctions;</li>
 *   <li>{@code OR(a, b)} — a match must satisfy at least one branch, so the narrowing is the
 *       union, expressed by concatenating the clause lists;</li>
 *   <li>{@code NOT(x)} — the negation of a necessary condition is not itself necessary, so the
 *       plan collapses to {@link #trivial()} (keep everything) and the decision is left to the
 *       in-memory verifier.</li>
 * </ul>
 */
public final class NarrowPlan {

    /** Guard against exponential blow-up when distributing AND over OR. */
    public static final int MAX_CLAUSES = 64;
    /** Guard against a pathological number of conditions. */
    public static final int MAX_CONDS = 128;

    private static final List<Cond> NO_CONDS = Collections.emptyList();
    private static final NarrowPlan TRIVIAL = new NarrowPlan(Collections.singletonList(NO_CONDS));

    private final List<List<Cond>> clauses;

    private NarrowPlan(List<List<Cond>> clauses) {
        this.clauses = clauses;
    }

    /**
     * @return the plan that keeps every candidate
     */
    public static NarrowPlan trivial() {
        return TRIVIAL;
    }

    /**
     * @param condition the condition every match must satisfy
     * @return a single-clause plan
     */
    public static NarrowPlan of(Cond condition) {
        return new NarrowPlan(Collections.<List<Cond>>singletonList(
                Collections.singletonList(condition)));
    }

    /**
     * @param conditions conditions all required for a match, must not be empty
     * @return a single-clause conjunction plan
     */
    public static NarrowPlan conjunction(List<Cond> conditions) {
        if (conditions.isEmpty() || conditions.size() > MAX_CONDS) {
            return TRIVIAL;
        }
        return new NarrowPlan(Collections.<List<Cond>>singletonList(
                Collections.unmodifiableList(new ArrayList<Cond>(conditions))));
    }

    /**
     * Conjoin two plans by distributing AND over OR. Falls back to {@link #trivial()} when the
     * product exceeds {@link #MAX_CLAUSES} or the conjunction exceeds {@link #MAX_CONDS}.
     *
     * @param left  operand plan
     * @param right operand plan
     * @return the conjoined plan
     */
    public static NarrowPlan and(NarrowPlan left, NarrowPlan right) {
        if (left.isTrivial()) {
            return right;
        }
        if (right.isTrivial()) {
            return left;
        }
        if ((long) left.clauses.size() * right.clauses.size() > MAX_CLAUSES) {
            return TRIVIAL;
        }
        final List<List<Cond>> product = new ArrayList<List<Cond>>(left.clauses.size() * right.clauses.size());
        for (List<Cond> a : left.clauses) {
            for (List<Cond> b : right.clauses) {
                if (a.size() + b.size() > MAX_CONDS) {
                    return TRIVIAL;
                }
                final List<Cond> merged = new ArrayList<Cond>(a.size() + b.size());
                merged.addAll(a);
                merged.addAll(b);
                product.add(Collections.unmodifiableList(merged));
            }
        }
        return new NarrowPlan(Collections.unmodifiableList(product));
    }

    /**
     * Disjoin two plans. Falls back to {@link #trivial()} when the union exceeds
     * {@link #MAX_CLAUSES}.
     *
     * @param left  operand plan
     * @param right operand plan
     * @return the disjoined plan
     */
    public static NarrowPlan or(NarrowPlan left, NarrowPlan right) {
        if (left.isTrivial() || right.isTrivial()) {
            return TRIVIAL;
        }
        if (left.clauses.size() + right.clauses.size() > MAX_CLAUSES) {
            return TRIVIAL;
        }
        final List<List<Cond>> union = new ArrayList<List<Cond>>(left.clauses.size() + right.clauses.size());
        union.addAll(left.clauses);
        union.addAll(right.clauses);
        return new NarrowPlan(Collections.unmodifiableList(union));
    }

    /**
     * @return true when the plan cannot drop any candidate
     */
    public boolean isTrivial() {
        return clauses.size() == 1 && clauses.get(0).isEmpty();
    }

    /**
     * @return the number of disjuncts
     */
    public int clauseCount() {
        return clauses.size();
    }

    /**
     * @return the conjunctions, in evaluation order
     */
    public List<List<Cond>> clauses() {
        return clauses;
    }

    /**
     * @return the HASH fields the plan reads, in ARGV index order
     */
    public List<String> referencedFields() {
        final Set<String> names = new LinkedHashSet<String>();
        for (List<Cond> clause : clauses) {
            for (Cond cond : clause) {
                names.add(cond.field());
            }
        }
        return new ArrayList<String>(names);
    }

    /**
     * Encode the scan window for the Lua script.
     * <p>
     * Layout: {@code [prefix, windowStart, windowEnd, direction, fieldCount, fields..,
     * clauseCount, (condCount, (fieldIndex, opCode, operandCount, operands..)..)..]}.
     *
     * @param hashKeyPrefix prefix that turns a ZSET member into the entity HASH key
     * @param windowStart   inclusive 0-based ZSET index to start scanning at
     * @param windowEnd     inclusive 0-based ZSET index to stop scanning at
     * @param descending    true to walk the index high-score first, matching an ORDER BY on the
     *                      declared score field
     * @return the ARGV list
     */
    public List<String> buildArgs(String hashKeyPrefix, long windowStart, long windowEnd, boolean descending) {
        final List<String> fields = referencedFields();
        final List<String> args = new ArrayList<String>(9 + fields.size() + clauses.size() * 4);
        args.add(hashKeyPrefix);
        args.add(Long.toString(windowStart));
        args.add(Long.toString(windowEnd));
        args.add(descending ? "desc" : "asc");
        args.add(Integer.toString(fields.size()));
        args.addAll(fields);
        args.add(Integer.toString(clauses.size()));
        for (List<Cond> clause : clauses) {
            args.add(Integer.toString(clause.size()));
            for (Cond cond : clause) {
                args.add(Integer.toString(fields.indexOf(cond.field())));
                args.add(Integer.toString(cond.op().code()));
                args.add(Integer.toString(cond.operands().size()));
                args.addAll(cond.operands());
            }
        }
        return args;
    }

    @Override
    public String toString() {
        return "NarrowPlan" + clauses;
    }
}
