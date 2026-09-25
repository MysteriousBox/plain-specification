package org.plain.core.evaluate;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.Specification;
import org.plain.specification.core.evaluate.WhereEvaluator;
import org.plain.specification.core.expression.Expressions;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WhereEvaluatorTest {

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
    void instance_shouldReturnSameInstance() {
        assertSame(WhereEvaluator.INSTANCE, WhereEvaluator.INSTANCE);
    }

    @Test
    void isCriteriaEvaluator_shouldReturnFalse() {
        assertFalse(WhereEvaluator.INSTANCE.isCriteriaEvaluator());
    }

    @Test
    void evaluate_shouldFilterBySingleCondition() {
        List<User> users = Arrays.asList(
            new User("Alice", 25),
            new User("Bob", 35),
            new User("Charlie", 20)
        );

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 24));

        Collection<User> result = WhereEvaluator.INSTANCE.evaluate(users, spec);
        assertEquals(2, result.size());
    }

    @Test
    void evaluate_shouldApplyMultipleDescriptorsAsAnd() {
        List<User> users = Arrays.asList(
            new User("Alice", 25),
            new User("Bob", 35),
            new User("Charlie", 30)
        );

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 20));
        spec.query().where(Expressions.<User>create().lessThan(User::getAge, 32));

        Collection<User> result = WhereEvaluator.INSTANCE.evaluate(users, spec);
        assertEquals(2, result.size());
    }

    @Test
    void evaluate_shouldReturnEmpty_whenNoneMatch() {
        List<User> users = Arrays.asList(
            new User("Alice", 25),
            new User("Bob", 35)
        );

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 100));

        Collection<User> result = WhereEvaluator.INSTANCE.evaluate(users, spec);
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_shouldReturnAll_whenNoWhereExpressions() {
        List<User> users = Arrays.asList(
            new User("Alice", 25),
            new User("Bob", 35)
        );

        Specification<User> spec = new Specification<>();

        Collection<User> result = WhereEvaluator.INSTANCE.evaluate(users, spec);
        assertEquals(2, result.size());
    }

    @Test
    void evaluate_shouldHandleEmptyCollection() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 0));

        Collection<User> result = WhereEvaluator.INSTANCE.evaluate(Collections.emptyList(), spec);
        assertTrue(result.isEmpty());
    }
}
