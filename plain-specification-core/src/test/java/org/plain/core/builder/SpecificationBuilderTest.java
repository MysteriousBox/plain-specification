package org.plain.core.builder;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;

import static org.junit.jupiter.api.Assertions.*;

class SpecificationBuilderTest {

    @Getter
    private static class User {
        String name;
        int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Test
    void where_withTrueCondition_shouldAddExpression() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"), true);
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20)));
    }

    @Test
    void where_withFalseCondition_shouldNotAddExpression() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"), false);
        assertTrue(spec.isSatisfiedBy(new User("Bob", 20)));
    }

    @Test
    void where_withNoConditions_shouldRegisterNothingAndMatchEverything() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create());
        assertFalse(spec.getWhereExpressions().iterator().hasNext());
        assertTrue(spec.isSatisfiedBy(new User("Bob", 20)));
        assertTrue(spec.isSatisfiedBy(new User("Alice", 30)));
    }

    @Test
    void orderBy_withTrueCondition_shouldAddOrder() {
        Specification<User> spec = new Specification<>();
        spec.query().orderBy(Expressions.<User>create().orderBy(User::getAge), true);
        assertTrue(spec.getOrderExpressions().iterator().hasNext());
    }

    @Test
    void orderBy_withFalseCondition_shouldNotAddOrder_butSetChainDiscarded() {
        Specification<User> spec = new Specification<>();
        spec.query()
            .orderBy(Expressions.<User>create().orderBy(User::getAge), false)
            .thenBy(Expressions.<User>create().orderBy(User::getName), true);
        assertFalse(spec.getOrderExpressions().iterator().hasNext());
    }

    @Test
    void thenBy_shouldBeSkipped_whenPrimaryOrderByWasDiscarded() {
        Specification<User> spec = new Specification<>();
        spec.query()
            .orderBy(Expressions.<User>create().orderBy(User::getAge), false)
            .thenBy(Expressions.<User>create().orderBy(User::getName), true);

        int count = 0;
        for (Object ignored : spec.getOrderExpressions()) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    void thenBy_withFalseCondition_shouldDiscardChain() {
        Specification<User> spec = new Specification<>();
        spec.query()
            .orderBy(Expressions.<User>create().orderBy(User::getAge), true)
            .thenBy(Expressions.<User>create().orderBy(User::getName), false)
            .thenBy(Expressions.<User>create().orderBy(User::getAge), true);

        int count = 0;
        for (Object ignored : spec.getOrderExpressions()) {
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    void thenByDescending_withFalseCondition_shouldDiscardChain() {
        Specification<User> spec = new Specification<>();
        spec.query()
            .orderBy(Expressions.<User>create().orderBy(User::getAge), true)
            .thenByDescending(Expressions.<User>create().orderByDescending(User::getName), false)
            .thenBy(Expressions.<User>create().orderBy(User::getAge), true);

        int count = 0;
        for (Object ignored : spec.getOrderExpressions()) {
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    void orderByDescending_withFalseCondition_shouldDiscardChain() {
        Specification<User> spec = new Specification<>();
        spec.query()
            .orderByDescending(Expressions.<User>create().orderByDescending(User::getAge), false)
            .thenBy(Expressions.<User>create().orderBy(User::getName), true);

        int count = 0;
        for (Object ignored : spec.getOrderExpressions()) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    void multipleWhereExpressions_shouldBeAnded() {
        Specification<User> spec = new Specification<>();
        spec.query()
            .where(Expressions.<User>create().equal(User::getName, "Alice"))
            .where(Expressions.<User>create().greaterThan(User::getAge, 18));

        assertTrue(spec.isSatisfiedBy(new User("Alice", 20)));
        assertFalse(spec.isSatisfiedBy(new User("Alice", 16)));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 20)));
    }

    @Test
    void getSpecification_shouldReturnSameSpecification() {
        Specification<User> spec = new Specification<>();
        assertSame(spec, spec.query().getSpecification());
    }
}
