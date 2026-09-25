package org.plain.core.evaluate;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderEvaluatorTest {

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
    void evaluate_shouldSortAscending_bySingleField() {
        List<User> users = Arrays.asList(
            new User("Charlie", 30),
            new User("Alice", 25),
            new User("Bob", 35)
        );

        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getAge)
            .build();

        Collection<User> result = spec.evaluate(users);
        List<User> resultList = new ArrayList<>(result);

        assertEquals(3, resultList.size());
        assertEquals(25, resultList.get(0).getAge());
        assertEquals(30, resultList.get(1).getAge());
        assertEquals(35, resultList.get(2).getAge());
    }

    @Test
    void evaluate_shouldSortDescending_bySingleField() {
        List<User> users = Arrays.asList(
            new User("Charlie", 30),
            new User("Alice", 25),
            new User("Bob", 35)
        );

        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderByDescending(User::getAge)
            .build();

        Collection<User> result = spec.evaluate(users);
        List<User> resultList = new ArrayList<>(result);

        assertEquals(3, resultList.size());
        assertEquals(35, resultList.get(0).getAge());
        assertEquals(30, resultList.get(1).getAge());
        assertEquals(25, resultList.get(2).getAge());
    }

    @Test
    void evaluate_shouldSortByMultipleFields() {
        List<User> users = Arrays.asList(
            new User("Alice", 30),
            new User("Bob", 25),
            new User("Charlie", 30),
            new User("David", 25)
        );

        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getAge)
            .thenBy(User::getName)
            .build();

        Collection<User> result = spec.evaluate(users);
        List<User> resultList = new ArrayList<>(result);

        assertEquals(4, resultList.size());
        assertEquals("Bob", resultList.get(0).getName());
        assertEquals("David", resultList.get(1).getName());
        assertEquals("Alice", resultList.get(2).getName());
        assertEquals("Charlie", resultList.get(3).getName());
    }

    @Test
    void evaluate_shouldSortAscendingThenDescending() {
        List<User> users = Arrays.asList(
            new User("Alice", 30),
            new User("Bob", 25),
            new User("Charlie", 30),
            new User("David", 25)
        );

        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getAge)
            .thenByDescending(User::getName)
            .build();

        Collection<User> result = spec.evaluate(users);
        List<User> resultList = new ArrayList<>(result);

        assertEquals(4, resultList.size());
        assertEquals("David", resultList.get(0).getName());
        assertEquals("Bob", resultList.get(1).getName());
        assertEquals("Charlie", resultList.get(2).getName());
        assertEquals("Alice", resultList.get(3).getName());
    }

    @Test
    void evaluate_shouldReturnOriginal_whenNoOrderExpressions() {
        List<User> users = Arrays.asList(
            new User("Charlie", 30),
            new User("Alice", 25),
            new User("Bob", 35)
        );

        ISpecification<User> spec = Specification.where(User::getAge).gt(0).build();

        Collection<User> result = spec.evaluate(users);

        assertEquals(3, result.size());
    }

    @Test
    void evaluate_shouldFilterAndSort() {
        List<User> users = Arrays.asList(
            new User("Alice", 30),
            new User("Bob", 20),
            new User("Charlie", 35),
            new User("David", 25)
        );

        ISpecification<User> spec = Specification.where(User::getAge).gte(25)
            .orderByDescending(User::getAge)
            .build();

        Collection<User> result = spec.evaluate(users);
        List<User> resultList = new ArrayList<>(result);

        assertEquals(3, resultList.size());
        assertEquals(35, resultList.get(0).getAge());
        assertEquals(30, resultList.get(1).getAge());
        assertEquals(25, resultList.get(2).getAge());
    }
}
