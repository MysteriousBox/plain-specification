package org.plain.core.expression;

import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test Expressions used
 */
class ExpressionsTest {
    // Helper class for testing
    @Getter
    private static class TestEntity {
        String name;
        int age;

        public TestEntity(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private Expressions<TestEntity> builder;
    private SFunction<TestEntity, String> mockStringFunc;
    private SFunction<TestEntity, Integer> mockIntFunc;

    @BeforeEach
    void setUp() {
        builder = Expressions.create();
        mockStringFunc = TestEntity::getName;
        mockIntFunc = TestEntity::getAge;
    }

    // Test case 1: Basic AND combination
    @Test
    void testAndExpression() {
        IExpression<TestEntity> expr = builder.equal(mockStringFunc, "Alice")
                .and()
                .greaterThan(mockIntFunc, 18)
                .build();
        assertInstanceOf(AndExpression.class, expr);
        AndExpression<TestEntity> andExpr = (AndExpression<TestEntity>) expr;
        assertInstanceOf(EqualExpression.class, andExpr.getLeft());
        assertInstanceOf(GreaterThanExpression.class, andExpr.getRight());
        boolean alice = expr.accept(new PredicateExpressionVisitor<>()).test(new TestEntity("Alice", 20));
        assertTrue(alice);
        boolean notAlice = expr.accept(new PredicateExpressionVisitor<>()).test(new TestEntity("Bob", 17));
        assertFalse(notAlice);
    }

    // Test case 2: OR with NOT combination
    @Test
    void testOrExpressionWithNot() {
        IExpression<TestEntity> expr = builder.equal(mockStringFunc, "Alice")
                .or()
                .not()
                .greaterThan(mockIntFunc, 18)
                .or()
                .lessThan(mockIntFunc, 60)
                .build();
        assertInstanceOf(OrExpression.class, expr);
        OrExpression<TestEntity> orExpr = (OrExpression<TestEntity>) expr;
        assertInstanceOf(OrExpression.class, orExpr.getLeft());
        assertInstanceOf(LessThanExpression.class, orExpr.getRight());
        boolean alice = expr.accept(new PredicateExpressionVisitor<>()).test(new TestEntity("Alice", 20));
        assertTrue(alice);
        boolean notAlice = expr.accept(new PredicateExpressionVisitor<>()).test(new TestEntity("Bob", 17));
        assertTrue(notAlice);
    }

    @Test
    void testNotExpression() {
        IExpression<TestEntity> expr = new NotExpression<>(new EqualExpression<>(TestEntity::getAge, 20));
        assertInstanceOf(NotExpression.class, expr);
        Predicate<TestEntity> predicate = expr.accept(new PredicateExpressionVisitor<>());
        assertTrue(predicate.test(new TestEntity("Bob", 17)));
        assertFalse(predicate.test(new TestEntity("Alice", 20)));
    }

}
