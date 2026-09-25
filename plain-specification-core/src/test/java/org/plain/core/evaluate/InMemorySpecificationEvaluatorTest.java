package org.plain.core.evaluate;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.*;
import org.plain.specification.core.evaluate.IEvaluator;
import org.plain.specification.core.evaluate.InMemorySpecificationEvaluator;
import org.plain.specification.core.expression.Expressions;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySpecificationEvaluatorTest {

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
    void defaultInstance_shouldFilterAndSort() {
        List<User> users = Arrays.asList(
            new User("Charlie", 30),
            new User("Alice", 25),
            new User("Bob", 35)
        );

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 20));
        spec.query().orderBy(Expressions.<User>create().orderBy(User::getAge));

        Collection<User> result = InMemorySpecificationEvaluator.DEFAULT.evaluate(users, spec);
        List<User> list = new ArrayList<>(result);

        assertEquals(3, list.size());
        assertEquals(25, list.get(0).getAge());
        assertEquals(30, list.get(1).getAge());
        assertEquals(35, list.get(2).getAge());
    }

    @Test
    void evaluate_shouldReturnAll_whenNoWhereExpressions() {
        List<User> users = Arrays.asList(
            new User("Alice", 25),
            new User("Bob", 35)
        );

        Specification<User> spec = new Specification<>();

        Collection<User> result = InMemorySpecificationEvaluator.DEFAULT.evaluate(users, spec);
        assertEquals(2, result.size());
    }

    @Test
    void evaluate_shouldReturnEmpty_whenNoMatch() {
        List<User> users = Arrays.asList(
            new User("Alice", 25),
            new User("Bob", 35)
        );

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 100));

        Collection<User> result = InMemorySpecificationEvaluator.DEFAULT.evaluate(users, spec);
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_shouldHandleEmptyCollection() {
        Collection<User> result = InMemorySpecificationEvaluator.DEFAULT.evaluate(
            Collections.emptyList(), new Specification<>());
        assertTrue(result.isEmpty());
    }

    @Test
    void customConstructor_shouldUseProvidedEvaluators() {
        AtomicBoolean called = new AtomicBoolean(false);
        IEvaluator customEvaluator = new IEvaluator() {
            @Override
            public Boolean isCriteriaEvaluator() { return false; }

            @Override
            public <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification) {
                called.set(true);
                return entities;
            }
        };

        InMemorySpecificationEvaluator evaluator = new InMemorySpecificationEvaluator(
            Collections.singletonList(customEvaluator));
        evaluator.evaluate(Arrays.asList(new User("A", 1)), new Specification<>());

        assertTrue(called.get());
    }
}
