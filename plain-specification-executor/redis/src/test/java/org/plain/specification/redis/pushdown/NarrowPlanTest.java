package org.plain.specification.redis.pushdown;

import org.junit.jupiter.api.Test;
import org.plain.specification.core.expression.SFunction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two contracts the Lua script depends on: which value domain a getter resolves to, and
 * how a plan is serialised into ARGV.
 */
class NarrowPlanTest {

    static class Shape {
        private int count;
        private Integer boxed;
        private long wide;
        private Long boxedWide;
        private short small;
        private byte tiny;
        private BigInteger seq;
        private String label;
        private float ratio;
        private double amount;
        private BigDecimal money;
        private LocalDateTime stamp;
        private Boolean on;
        private List<String> items;

        public int getCount() { return count; }
        public Integer getBoxed() { return boxed; }
        public long getWide() { return wide; }
        public Long getBoxedWide() { return boxedWide; }
        public short getSmall() { return small; }
        public byte getTiny() { return tiny; }
        public BigInteger getSeq() { return seq; }
        public String getLabel() { return label; }
        public float getRatio() { return ratio; }
        public double getAmount() { return amount; }
        public BigDecimal getMoney() { return money; }
        public LocalDateTime getStamp() { return stamp; }
        public Boolean getOn() { return on; }
        public List<String> getItems() { return items; }
        public String getDerived() { return label + count; }
    }

    // ---- FieldMeta ----

    @Test
    void integralDomainsAreNarrowable() {
        assertEquals(FieldMeta.Kind.INTEGER, FieldMeta.of((SFunction<Shape, Integer>) Shape::getBoxed).kind());
        assertEquals(FieldMeta.Kind.INTEGER, FieldMeta.of((SFunction<Shape, Integer>) Shape::getCount).kind());
        assertEquals(FieldMeta.Kind.INTEGER, FieldMeta.of((SFunction<Shape, Long>) Shape::getWide).kind());
        assertEquals(FieldMeta.Kind.INTEGER, FieldMeta.of((SFunction<Shape, Long>) Shape::getBoxedWide).kind());
        assertEquals(FieldMeta.Kind.INTEGER, FieldMeta.of((SFunction<Shape, Short>) Shape::getSmall).kind());
        assertEquals(FieldMeta.Kind.INTEGER, FieldMeta.of((SFunction<Shape, Byte>) Shape::getTiny).kind());
        assertEquals(FieldMeta.Kind.INTEGER, FieldMeta.of((SFunction<Shape, BigInteger>) Shape::getSeq).kind());
    }

    @Test
    void stringDomainIsNarrowable() {
        assertEquals(FieldMeta.Kind.STRING, FieldMeta.of((SFunction<Shape, String>) Shape::getLabel).kind());
    }

    @Test
    void floatingTemporalBooleanAndCollectionDomainsAreNotNarrowable() {
        assertEquals(FieldMeta.Kind.UNPUSHABLE, FieldMeta.of((SFunction<Shape, Float>) Shape::getRatio).kind());
        assertEquals(FieldMeta.Kind.UNPUSHABLE, FieldMeta.of((SFunction<Shape, Double>) Shape::getAmount).kind());
        assertEquals(FieldMeta.Kind.UNPUSHABLE, FieldMeta.of((SFunction<Shape, BigDecimal>) Shape::getMoney).kind());
        assertEquals(FieldMeta.Kind.UNPUSHABLE, FieldMeta.of((SFunction<Shape, LocalDateTime>) Shape::getStamp).kind());
        assertEquals(FieldMeta.Kind.UNPUSHABLE, FieldMeta.of((SFunction<Shape, Boolean>) Shape::getOn).kind());
        assertEquals(FieldMeta.Kind.UNPUSHABLE, FieldMeta.of((SFunction<Shape, List<String>>) Shape::getItems).kind());
    }

    @Test
    void propertyNamesComeFromTheGetter() {
        assertEquals("boxed", FieldMeta.of((SFunction<Shape, Integer>) Shape::getBoxed).fieldName());
        assertEquals("derived", FieldMeta.of((SFunction<Shape, String>) Shape::getDerived).fieldName());
    }

    @Test
    void unresolvableReferencesDegradeInsteadOfThrowing() {
        SFunction<Shape, Integer> computed = shape -> 42;
        FieldMeta meta = FieldMeta.of(computed);
        assertEquals(FieldMeta.Kind.UNPUSHABLE, meta.kind());
        assertFalse(meta.pushable());
        assertFalse(FieldMeta.of(null).pushable());
    }

    // ---- plan algebra ----

    @Test
    void trivialPlanKeepsEverything() {
        NarrowPlan trivial = NarrowPlan.trivial();
        assertTrue(trivial.isTrivial());
        assertEquals(1, trivial.clauseCount());
        assertTrue(trivial.referencedFields().isEmpty());
    }

    @Test
    void andWithTrivialIsIdentity() {
        NarrowPlan plan = NarrowPlan.of(Cond.of(Cond.Op.STR_EQ, "label", "a"));
        assertEquals(plan.clauses(), NarrowPlan.and(NarrowPlan.trivial(), plan).clauses());
        assertEquals(plan.clauses(), NarrowPlan.and(plan, NarrowPlan.trivial()).clauses());
    }

    @Test
    void orWithTrivialLosesTheWholeNarrowing() {
        NarrowPlan plan = NarrowPlan.of(Cond.of(Cond.Op.STR_EQ, "label", "a"));
        assertTrue(NarrowPlan.or(plan, NarrowPlan.trivial()).isTrivial());
    }

    @Test
    void conjunctionsAreCappedToAvoidBlowUp() {
        List<Cond> many = new ArrayList<Cond>();
        for (int i = 0; i <= NarrowPlan.MAX_CONDS; i++) {
            many.add(Cond.of(Cond.Op.INT_EQ, "f" + i, "1"));
        }
        assertTrue(NarrowPlan.conjunction(many).isTrivial());
    }

    @Test
    void distributionIsCappedToAvoidBlowUp() {
        NarrowPlan wide = branches(NarrowPlan.MAX_CLAUSES);
        assertTrue(NarrowPlan.and(wide, wide).isTrivial(), "64 x 64 must collapse instead of exploding");
        NarrowPlan small = branches(4);
        assertEquals(16, NarrowPlan.and(small, small).clauseCount());
    }

    private static NarrowPlan branches(int count) {
        NarrowPlan plan = NarrowPlan.of(Cond.of(Cond.Op.STR_EQ, "label", "v0"));
        for (int i = 1; i < count; i++) {
            plan = NarrowPlan.or(plan, NarrowPlan.of(Cond.of(Cond.Op.STR_EQ, "label", "v" + i)));
        }
        return plan;
    }

    // ---- ARGV encoding ----

    @Test
    void encodesTheWindowContract() {
        NarrowPlan plan = NarrowPlan.of(Cond.of(Cond.Op.STR_EQ, "label", "Al"));
        List<String> args = plan.buildArgs("shape:", 10L, 19L);
        assertEquals(Arrays.asList(
                "shape:",            // hash key prefix
                "10", "19",          // window
                "1",                 // field count
                "label",             // fields
                "1",                 // clause count
                "1",                 // condition count
                "0",                 // field index
                "11",                // opcode STR_EQ
                "1",                 // operand count
                "Al"), args);
    }

    @Test
    void sharesOneFieldIndexAcrossConditionsOnTheSameField() {
        List<Cond> conds = Arrays.asList(
                Cond.of(Cond.Op.INT_GE, "count", "1"),
                Cond.of(Cond.Op.INT_LE, "count", "9"));
        List<String> args = NarrowPlan.conjunction(conds).buildArgs("s:", 0L, 4L);
        assertEquals(Arrays.asList("s:", "0", "4", "1", "count", "1"), args.subList(0, 6));
        assertEquals(
                Arrays.asList("2", "0", "6", "1", "1", "0", "8", "1", "9"),
                args.subList(6, args.size()));
    }

    @Test
    void encodesMultiValuedOperandsInOrder() {
        List<String> args = NarrowPlan.of(Cond.of(Cond.Op.STR_IN, "label", Arrays.asList("A", "B", "C")))
                .buildArgs("s:", 0L, 0L);
        assertEquals(Arrays.asList("3", "A", "B", "C"), args.subList(args.size() - 4, args.size()));
    }

    @Test
    void presenceConditionsCarryNoOperands() {
        List<String> args = NarrowPlan.of(Cond.presence(Cond.Op.NULLABLE, "stamp"))
                .buildArgs("s:", 0L, 0L);
        assertEquals("0", args.get(args.size() - 1));
    }
}
