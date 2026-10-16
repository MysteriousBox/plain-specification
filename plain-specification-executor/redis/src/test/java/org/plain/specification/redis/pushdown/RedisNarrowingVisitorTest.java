package org.plain.specification.redis.pushdown;

import org.junit.jupiter.api.Test;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.AndExpression;
import org.plain.specification.core.expression.EqualExpression;
import org.plain.specification.core.expression.ExistsExpression;
import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.expression.IExpression;
import org.plain.specification.core.expression.InExpression;
import org.plain.specification.core.expression.IsNullExpression;
import org.plain.specification.core.expression.IsNotNullExpression;
import org.plain.specification.core.expression.NotExpression;
import org.plain.specification.core.expression.NotExistsExpression;
import org.plain.specification.core.expression.NotInExpression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what the narrowing compiler is allowed to claim. Every "is trivial" assertion below is a
 * soundness claim: Redis must not decide that predicate, because the raw HASH string does not carry
 * enough information to reproduce the Java comparison.
 */
class RedisNarrowingVisitorTest {

    static class Emp {
        private String name;
        private String code;
        private Integer age;
        private int level;
        private long id;
        private BigInteger seq;
        private Float score;
        private double ratio;
        private BigDecimal salary;
        private LocalDateTime joined;
        private Boolean active;
        private List<String> tags;

        public String getName() { return name; }
        public String getCode() { return code; }
        public Integer getAge() { return age; }
        public int getLevel() { return level; }
        public long getId() { return id; }
        public BigInteger getSeq() { return seq; }
        public Float getScore() { return score; }
        public double getRatio() { return ratio; }
        public BigDecimal getSalary() { return salary; }
        public LocalDateTime getJoined() { return joined; }
        public Boolean getActive() { return active; }
        public List<String> getTags() { return tags; }
        public String getFullName() { return name + "/" + code; }
    }

    private final RedisNarrowingVisitor<Emp> visitor = new RedisNarrowingVisitor<>();

    private static Expressions<Emp> exprs() {
        return Expressions.create();
    }

    private static ISpecification<Emp> spec(Expressions<Emp> expressions) {
        return new Specification<Emp>().query().where(expressions).getSpecification();
    }

    private NarrowPlan planOf(ISpecification<Emp> specification) {
        NarrowPlan plan = NarrowPlan.trivial();
        for (IExpressionDescriptor<Emp> descriptor : specification.getWhereExpressions()) {
            plan = NarrowPlan.and(plan, descriptor.func(visitor));
        }
        return plan;
    }

    private NarrowPlan planOf(Expressions<Emp> expressions) {
        return planOf(spec(expressions));
    }

    private static Cond only(NarrowPlan plan) {
        assertEquals(1, plan.clauses().size(), "expected a single clause in " + plan);
        assertEquals(1, plan.clauses().get(0).size(), "expected a single condition in " + plan);
        return plan.clauses().get(0).get(0);
    }

    // ---- value domains Redis can decide ----

    @Test
    void equalOnStringFieldBecomesExactStringEquality() {
        Cond cond = only(planOf(Specification.where(Emp::getName).eq("Alice").build()));
        assertEquals(Cond.Op.STR_EQ, cond.op());
        assertEquals("name", cond.field());
        assertEquals(Collections.singletonList("Alice"), cond.operands());
    }

    @Test
    void equalOnIntegerFieldBecomesCanonicalIntegerEquality() {
        Cond cond = only(planOf(Specification.where(Emp::getAge).eq(25).build()));
        assertEquals(Cond.Op.INT_EQ, cond.op());
        assertEquals("age", cond.field());
        assertEquals(Collections.singletonList("25"), cond.operands());
    }

    @Test
    void equalOnPrimitiveIntFieldIsPushable() {
        Cond cond = only(planOf(Specification.where(Emp::getLevel).eq(3).build()));
        assertEquals(Cond.Op.INT_EQ, cond.op());
        assertEquals("level", cond.field());
    }

    @Test
    void equalOnLongFieldKeepsEveryDigit() {
        Cond cond = only(planOf(Specification.where(Emp::getId).eq(9007199254740993L).build()));
        assertEquals("9007199254740993", cond.operands().get(0));
    }

    @Test
    void equalOnBigIntegerFieldUsesDecimalText() {
        Cond cond = only(planOf(Specification.where(Emp::getSeq)
                .eq(new BigInteger("123456789012345678901234567890")).build()));
        assertEquals(Cond.Op.INT_EQ, cond.op());
        assertEquals(Collections.singletonList("123456789012345678901234567890"), cond.operands());
    }

    @Test
    void rangeOnLongFieldBecomesExactIntegerComparison() {
        Cond cond = only(planOf(Specification.where(Emp::getId).gt(100L).build()));
        assertEquals(Cond.Op.INT_GT, cond.op());
        assertEquals("100", cond.operands().get(0));
    }

    @Test
    void betweenBecomesTwoConditionsInOneClause() {
        NarrowPlan plan = planOf(Specification.where(Emp::getAge).between(18, 30).build());
        assertEquals(1, plan.clauses().size());
        List<Cond> conds = plan.clauses().get(0);
        assertEquals(2, conds.size());
        assertEquals(Cond.Op.INT_GE, conds.get(0).op());
        assertEquals(Cond.Op.INT_LE, conds.get(1).op());
        assertEquals("age", conds.get(1).field());
    }

    @Test
    void inOnStringFieldBecomesMembership() {
        Cond cond = only(planOf(Specification.where(Emp::getCode).in("A", "B").build()));
        assertEquals(Cond.Op.STR_IN, cond.op());
        assertEquals(Arrays.asList("A", "B"), cond.operands());
    }

    @Test
    void emptyInSetNarrowsEverythingAway() {
        // In memory an empty set matches nothing, so dropping every candidate is the exact answer.
        Cond cond = only(planOf(Specification.where(Emp::getCode).in(new String[0]).build()));
        assertEquals(Cond.Op.STR_IN, cond.op());
        assertTrue(cond.operands().isEmpty());
    }

    @Test
    void nullElementsAreDroppedFromMembershipSet() {
        Cond cond = only(planOf(spec(exprs().in(Emp::getCode, Arrays.asList("A", null, "B")))));
        assertEquals(Arrays.asList("A", "B"), cond.operands());
    }

    @Test
    void notInKeepsTheNullBranch() {
        NarrowPlan plan = planOf(spec(exprs().notIn(Emp::getCode, Arrays.asList("A", "B"))));
        assertEquals(2, plan.clauses().size(), "one clause for a null value, one for the non-members");
        Cond nullable = plan.clauses().get(0).get(0);
        assertEquals(Cond.Op.NULLABLE, nullable.op());
        assertEquals(Cond.Op.STR_NOTIN, plan.clauses().get(1).get(0).op());
    }

    // ---- value domains Redis must not decide ----

    @Test
    void floatEqualityIsNotPushedDown() {
        // stored "1.5234153287987707E-31" versus query "1.5234153E-31": the Java values are equal.
        assertTrue(planOf(Specification.where(Emp::getScore).eq(1.5234153E-31f).build()).isTrivial());
    }

    @Test
    void doubleEqualityIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getRatio).eq(0.1d).build()).isTrivial());
    }

    @Test
    void bigDecimalComparisonIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getSalary).eq(new BigDecimal("1.0")).build()).isTrivial());
    }

    @Test
    void temporalComparisonIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getJoined)
                .gt(LocalDateTime.of(2026, 1, 1, 0, 0)).build()).isTrivial());
    }

    @Test
    void booleanEqualityIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getActive).eq(Boolean.TRUE).build()).isTrivial());
    }

    @Test
    void stringOrderingIsNotPushedDown() {
        // Lua compares UTF-8 bytes, Java compares UTF-16 code units: they disagree off the ASCII range.
        assertTrue(planOf(Specification.where(Emp::getName).gt("Alice").build()).isTrivial());
    }

    // ---- null handling ----

    @Test
    void equalNullBecomesNullableCheck() {
        Cond cond = only(planOf(Specification.where(Emp::getAge).eq((Integer) null).build()));
        assertEquals(Cond.Op.NULLABLE, cond.op());
        assertEquals("age", cond.field());
    }

    @Test
    void isNullBecomesNullableCheckForAnyValueDomain() {
        Cond temporal = only(planOf(Specification.where(Emp::getJoined).isNull().build()));
        assertEquals(Cond.Op.NULLABLE, temporal.op());
        assertEquals("joined", temporal.field());
    }

    @Test
    void isNotNullIsNeverPushedDown() {
        // an absent field does not imply a null getter: derived getters have no backing field at all.
        assertTrue(planOf(Specification.where(Emp::getCode).isNotNull().build()).isTrivial());
    }

    @Test
    void notEqualNullIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getAge).neq((Integer) null).build()).isTrivial());
    }

    // ---- composition ----

    @Test
    void andConjoinsConditionsIntoOneClause() {
        NarrowPlan plan = planOf(Specification.where(Emp::getName).eq("Alice")
                .and(Emp::getAge).gt(18).build());
        assertEquals(1, plan.clauses().size());
        assertEquals(2, plan.clauses().get(0).size());
    }

    @Test
    void orDisjoinsIntoSeparateClauses() {
        NarrowPlan plan = planOf(Specification.where(Emp::getName).eq("Alice")
                .or(Emp::getCode).eq("X").build());
        assertEquals(2, plan.clauses().size());
    }

    @Test
    void andOverOrDistributes() {
        NarrowPlan plan = planOf(Specification.where(Emp::getName).eq("Alice")
                .or(Emp::getCode).eq("A")
                .and(Emp::getAge).eq(25).build());
        assertEquals(2, plan.clauses().size());
        for (List<Cond> clause : plan.clauses()) {
            assertEquals(2, clause.size(), "every branch must carry the conjunct " + clause);
        }
    }

    @Test
    void negationCollapsesTheWholeSubtree() {
        NarrowPlan plan = planOf(Specification.where(Emp::getName).eq("Alice")
                .or(Emp::getCode).eq("A").build());
        assertEquals(2, plan.clauses().size());
        NarrowPlan negated = planOf(spec(exprs()
                .equal(Emp::getName, "Alice")
                .not(new NotExpression<>(new EqualExpression<>(Emp::getCode, "A")))));
        assertEquals(Cond.Op.STR_EQ, only(negated).op());
        assertEquals("name", only(negated).field());
    }

    @Test
    void existsAndNotExistsAreNeverPushedDown() {
        IExpression<String> inner = new EqualExpression<String, String>(value -> value, "x");
        assertTrue(new ExistsExpression<Emp, String>(Emp::getTags, inner).accept(visitor).isTrivial());
        assertTrue(new NotExistsExpression<Emp, String>(Emp::getTags, inner).accept(visitor).isTrivial());
    }

    @Test
    void orderByContributesNoCondition() {
        NarrowPlan plan = planOf(Specification.where(Emp::getAge).eq(25).orderBy(Emp::getName).build());
        assertEquals(Cond.Op.INT_EQ, only(plan).op());
    }

    // ---- LIKE ----

    @Test
    void containsPatternRequiresASubstring() {
        Cond cond = only(planOf(Specification.where(Emp::getName).like("%lic%").build()));
        assertEquals(Cond.Op.STR_CONTAINS, cond.op());
        assertEquals("lic", cond.operands().get(0));
    }

    @Test
    void prefixPatternRequiresAPrefix() {
        Cond cond = only(planOf(Specification.where(Emp::getName).like("Al%").build()));
        assertEquals(Cond.Op.STR_PREFIX, cond.op());
    }

    @Test
    void suffixPatternRequiresASuffix() {
        Cond cond = only(planOf(Specification.where(Emp::getName).like("%ace").build()));
        assertEquals(Cond.Op.STR_SUFFIX, cond.op());
    }

    @Test
    void unanchoredPatternRequiresPrefixAndSuffix() {
        List<Cond> conds = planOf(Specification.where(Emp::getName).like("Al%ce").build()).clauses().get(0);
        assertEquals(2, conds.size(), "a prefix already implies containment, so no redundant condition");
        assertEquals(Cond.Op.STR_PREFIX, conds.get(0).op());
        assertEquals(Cond.Op.STR_SUFFIX, conds.get(1).op());
    }

    @Test
    void patternWithoutWildcardsRequiresPrefixAndSuffix() {
        List<Cond> conds = planOf(Specification.where(Emp::getName).like("Alice").build()).clauses().get(0);
        assertEquals(2, conds.size());
        assertEquals(Cond.Op.STR_PREFIX, conds.get(0).op());
        assertEquals(Cond.Op.STR_SUFFIX, conds.get(1).op());
    }

    @Test
    void multiSegmentPatternRequiresEverySegment() {
        List<Cond> conds = planOf(Specification.where(Emp::getName).like("%ali%ce%").build()).clauses().get(0);
        assertEquals(2, conds.size());
        assertEquals("ali", conds.get(0).operands().get(0));
        assertEquals("ce", conds.get(1).operands().get(0));
    }

    @Test
    void singleCharacterWildcardIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getName).like("a_c").build()).isTrivial());
    }

    @Test
    void escapedPatternIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getName).like("a\\%c").build()).isTrivial());
    }

    @Test
    void fullyWildcardedPatternIsNotPushedDown() {
        assertTrue(planOf(Specification.where(Emp::getName).like("%").build()).isTrivial());
    }

    // ---- declared capability set ----

    @Test
    void declaresOnlyWhatItCanNarrow() {
        Set<Class<? extends IExpression<Emp>>> declared = RedisNarrowingVisitor.narrowedExpressionTypes();
        assertTrue(declared.contains(EqualExpression.class));
        assertTrue(declared.contains(InExpression.class));
        assertTrue(declared.contains(NotInExpression.class));
        assertTrue(declared.contains(IsNullExpression.class));
        assertTrue(declared.contains(AndExpression.class));
        assertFalse(declared.contains(NotExpression.class), "negation has no sound necessary condition");
        assertFalse(declared.contains(IsNotNullExpression.class));
        assertFalse(declared.contains(ExistsExpression.class));
        assertFalse(declared.contains(NotExistsExpression.class));
    }

    @Test
    void opcodesMatchTheContractTheLuaScriptReads() {
        assertEquals(1, Cond.Op.INT_EQ.code());
        assertEquals(2, Cond.Op.INT_NE.code());
        assertEquals(3, Cond.Op.INT_IN.code());
        assertEquals(4, Cond.Op.INT_NOTIN.code());
        assertEquals(5, Cond.Op.INT_GT.code());
        assertEquals(6, Cond.Op.INT_GE.code());
        assertEquals(7, Cond.Op.INT_LT.code());
        assertEquals(8, Cond.Op.INT_LE.code());
        assertEquals(11, Cond.Op.STR_EQ.code());
        assertEquals(12, Cond.Op.STR_NE.code());
        assertEquals(13, Cond.Op.STR_IN.code());
        assertEquals(14, Cond.Op.STR_NOTIN.code());
        assertEquals(15, Cond.Op.STR_CONTAINS.code());
        assertEquals(16, Cond.Op.STR_PREFIX.code());
        assertEquals(17, Cond.Op.STR_SUFFIX.code());
        assertEquals(20, Cond.Op.NULLABLE.code());
    }
}
