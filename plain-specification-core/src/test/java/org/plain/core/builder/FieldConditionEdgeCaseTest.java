package org.plain.core.builder;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FieldConditionEdgeCaseTest {

    @Getter
    private static class Item {
        String name;
        Integer value;

        Item(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    void eq_null_shouldMatchNullField() {
        List<Item> items = Arrays.asList(
            new Item("A", null),
            new Item("B", 10)
        );

        ISpecification<Item> spec = Specification.where(Item::getValue).eq(null).build();
        Collection<Item> result = spec.evaluate(items);

        assertEquals(1, result.size());
        assertEquals("A", result.iterator().next().getName());
    }

    @Test
    void isNotNull_shouldMatchNonNullField() {
        List<Item> items = Arrays.asList(
            new Item("A", null),
            new Item("B", 10)
        );

        ISpecification<Item> spec = Specification.where(Item::getValue).isNotNull().build();
        Collection<Item> result = spec.evaluate(items);

        assertEquals(1, result.size());
        assertEquals("B", result.iterator().next().getName());
    }

    @Test
    void in_singleValue_shouldWork() {
        List<Item> items = Arrays.asList(
            new Item("A", 1),
            new Item("B", 2),
            new Item("C", 3)
        );

        ISpecification<Item> spec = Specification.where(Item::getValue).in(2).build();
        Collection<Item> result = spec.evaluate(items);

        assertEquals(1, result.size());
        assertEquals("B", result.iterator().next().getName());
    }

    @Test
    void in_emptyVarargs_shouldMatchNothing() {
        List<Item> items = Arrays.asList(
            new Item("A", 1),
            new Item("B", 2)
        );

        @SuppressWarnings("unchecked")
        ISpecification<Item> spec = Specification.where(Item::getValue).in(new Integer[0]).build();
        Collection<Item> result = spec.evaluate(items);

        assertTrue(result.isEmpty());
    }

    @Test
    void like_emptyPattern_shouldMatchEmptyString() {
        List<Item> items = Arrays.asList(
            new Item("", 1),
            new Item("hello", 2)
        );

        ISpecification<Item> spec = Specification.where(Item::getName).like("").build();
        Collection<Item> result = spec.evaluate(items);

        assertEquals(1, result.size());
        assertEquals("", result.iterator().next().getName());
    }

    @Test
    void like_wildcardPercent_shouldMatch() {
        List<Item> items = Arrays.asList(
            new Item("hello world", 1),
            new Item("goodbye", 2)
        );

        ISpecification<Item> spec = Specification.where(Item::getName).like("hello%").build();
        Collection<Item> result = spec.evaluate(items);

        assertEquals(1, result.size());
        assertEquals("hello world", result.iterator().next().getName());
    }

    @Test
    void build_withNoConditions_shouldReturnEmptySpec() {
        Specification<Item> spec = new Specification<>();
        spec.query().orderBy(Expressions.<Item>create().orderBy(Item::getValue));

        assertNotNull(spec);
        List<Item> items = Arrays.asList(new Item("A", 1), new Item("B", 2));
        Collection<Item> result = spec.evaluate(items);
        assertEquals(2, result.size());
    }

    @Test
    void orAsFirstCondition_shouldBehaveAsAnd() {
        List<Item> items = Arrays.asList(
            new Item("A", 1),
            new Item("B", 2),
            new Item("C", 3)
        );

        ISpecification<Item> spec = Specification.where(Item::getValue).gt(0)
            .or(Item::getValue).lt(10)
            .build();

        Collection<Item> result = spec.evaluate(items);
        assertEquals(3, result.size());
    }

    @Test
    void multipleAndOrInterleaved() {
        List<Item> items = Arrays.asList(
            new Item("A", 1),
            new Item("B", 2),
            new Item("C", 3),
            new Item("D", 4)
        );

        ISpecification<Item> spec = Specification.where(Item::getValue).gte(2)
            .and(Item::getValue).lte(4)
            .and(Item::getName).neq("C")
            .build();

        Collection<Item> result = spec.evaluate(items);
        assertEquals(2, result.size());
    }

    @Test
    void orderEvaluator_withNullFieldValues() {
        List<Item> items = Arrays.asList(
            new Item("B", 2),
            new Item("A", null),
            new Item("C", 1)
        );

        ISpecification<Item> spec = Specification.where(Item::getName).isNotNull()
            .orderBy(Item::getValue)
            .build();

        Collection<Item> result = spec.evaluate(items);
        List<Item> list = new ArrayList<>(result);

        assertEquals(3, list.size());
        assertEquals("C", list.get(0).getName());
        assertEquals("B", list.get(1).getName());
        assertEquals("A", list.get(2).getName());
    }
}
