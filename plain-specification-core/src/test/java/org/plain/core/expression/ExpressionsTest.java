package org.plain.core.expression;

import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plain.core.visitor.PredicateExpressionVisitor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test Expressions used
 */
public class ExpressionsTest {
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
    private Function<TestEntity, String> mockStringFunc;
    private Function<TestEntity, Integer> mockIntFunc;

    @BeforeEach
    public void setUp() {
        builder = Expressions.create();
        mockStringFunc = TestEntity::getName;
        mockIntFunc = TestEntity::getAge;
    }

    // Test case 1: Basic AND combination
    @Test
    public void testAndExpression() {
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
    public void testOrExpressionWithNot() {
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
    public void testNotExpression() {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity("Alice", 20),
                new TestEntity("Bob", 17),
                new TestEntity("Charlie", 30)
        );
        // 按照年龄倒序 排序倒序


    }

}
