package org.plain.specification.redis.pushdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One server-side narrowing condition: a <em>necessary</em> condition evaluated against the raw
 * HASH field string of a candidate entry.
 * <p>
 * A condition that cannot be decided from the stored string must keep the candidate; only a
 * provably failed condition may drop it. Candidates that survive are still re-checked in memory
 * with the original specification, so an imprecise "keep" costs performance while an incorrect
 * "drop" would cost correctness — which is why every opcode below treats undecidable input as
 * "keep".
 * <p>
 * The numeric codes are shared with the Lua scan script; see {@code RedisNarrowScan}.
 */
public final class Cond {

    /** Opcode identifiers, mirrored by the Lua scan script. */
    public enum Op {
        /** Canonical integer equality. */
        INT_EQ(1),
        /** Canonical integer inequality. */
        INT_NE(2),
        /** Canonical integer membership. */
        INT_IN(3),
        /** Canonical integer non-membership. */
        INT_NOTIN(4),
        /** Canonical integer strictly greater than the operand. */
        INT_GT(5),
        /** Canonical integer greater than or equal to the operand. */
        INT_GE(6),
        /** Canonical integer strictly less than the operand. */
        INT_LT(7),
        /** Canonical integer less than or equal to the operand. */
        INT_LE(8),
        /** Exact string equality. */
        STR_EQ(11),
        /** Exact string inequality. */
        STR_NE(12),
        /** String equals one of the operands. */
        STR_IN(13),
        /** String equals none of the operands. */
        STR_NOTIN(14),
        /** String contains the operand as a plain substring. */
        STR_CONTAINS(15),
        /** String starts with the operand. */
        STR_PREFIX(16),
        /** String ends with the operand. */
        STR_SUFFIX(17),
        /**
         * Value may still be {@code null} after the entity is rebuilt: keep when the stored string
         * is absent or empty, which is exactly what the write path stores for a null reference.
         */
        NULLABLE(20);

        private final int code;

        Op(int code) {
            this.code = code;
        }

        /**
         * @return the opcode as understood by the Lua script
         */
        public int code() {
            return code;
        }
    }

    private final Op op;
    private final String field;
    private final List<String> operands;

    private Cond(Op op, String field, List<String> operands) {
        this.op = op;
        this.field = field;
        this.operands = operands;
    }

    /**
     * Condition with a single operand.
     *
     * @param op      opcode
     * @param field   HASH field name
     * @param operand canonical stored form of the comparison value
     * @return the condition
     */
    public static Cond of(Op op, String field, String operand) {
        return new Cond(op, field, operand == null
                ? Collections.<String>emptyList()
                : Collections.singletonList(operand));
    }

    /**
     * Condition with a value set.
     *
     * @param op       opcode
     * @param field    HASH field name
     * @param operands canonical stored forms, {@code null} entries dropped
     * @return the condition
     */
    public static Cond of(Op op, String field, List<String> operands) {
        final List<String> copy = new ArrayList<String>(operands.size());
        for (String o : operands) {
            if (o != null) {
                copy.add(o);
            }
        }
        return new Cond(op, field, Collections.unmodifiableList(copy));
    }

    /**
     * Presence-only condition (no operands).
     *
     * @param op    opcode
     * @param field HASH field name
     * @return the condition
     */
    public static Cond presence(Op op, String field) {
        return new Cond(op, field, Collections.<String>emptyList());
    }

    /**
     * @return the opcode
     */
    public Op op() {
        return op;
    }

    /**
     * @return the HASH field name this condition reads
     */
    public String field() {
        return field;
    }

    /**
     * @return the operands in ARGV order, possibly empty
     */
    public List<String> operands() {
        return operands;
    }

    @Override
    public String toString() {
        return op + "(" + field + "," + operands + ")";
    }
}
