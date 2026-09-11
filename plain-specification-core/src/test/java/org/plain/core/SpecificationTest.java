package org.plain.core;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpecificationTest {

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
    void selectCompiler_shouldReturnEmptyList_whenNoWhereExpressions() {
        Specification<User> spec = new Specification<>();
        List<?> result = spec.selectCompiler(new PredicateExpressionVisitor<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void selectCompiler_shouldCompileWhereExpressions() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));
        List<?> result = spec.selectCompiler(new PredicateExpressionVisitor<>());
        assertEquals(1, result.size());
    }

    @Test
    void getWhereExpressions_shouldReturnEmptyIterable_whenNoneAdded() {
        Specification<User> spec = new Specification<>();
        Iterable<?> where = spec.getWhereExpressions();
        assertNotNull(where);
        assertFalse(where.iterator().hasNext());
    }

    @Test
    void getOrderExpressions_shouldReturnEmptyIterable_whenNoneAdded() {
        Specification<User> spec = new Specification<>();
        Iterable<?> orders = spec.getOrderExpressions();
        assertNotNull(orders);
        assertFalse(orders.iterator().hasNext());
    }

    @Test
    void isSatisfiedBy_shouldReturnTrue_whenEntityMatches() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20)));
    }

    @Test
    void isSatisfiedBy_shouldReturnFalse_whenEntityDoesNotMatch() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 20)));
    }

    @Test
    void evaluate_shouldFilterCollection() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 18));

        Collection<User> users = Arrays.asList(
            new User("Alice", 20),
            new User("Bob", 16),
            new User("Charlie", 25)
        );

        Collection<User> result = spec.evaluate(users);
        assertEquals(2, result.size());
    }

    @Test
    void evaluate_shouldReturnAll_whenNoWhereExpressions() {
        Specification<User> spec = new Specification<>();
        Collection<User> users = Arrays.asList(
            new User("Alice", 20),
            new User("Bob", 16)
        );
        Collection<User> result = spec.evaluate(users);
        assertEquals(2, result.size());
    }

    @Test
    void query_shouldReturnSameBuilderInstance() {
        Specification<User> spec = new Specification<>();
        assertSame(spec.query(), spec.query());
    }
}
