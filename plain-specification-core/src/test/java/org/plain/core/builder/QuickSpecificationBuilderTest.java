package org.plain.core.builder;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.OrderTypeEnum;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.OrderExpressionInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuickSpecificationBuilderTest {

    @Getter
    private static class User {
        String name;
        int age;
        String status;

        User(String name, int age, String status) {
            this.name = name;
            this.age = age;
            this.status = status;
        }
    }

    // --- Single condition tests ---

    @Test
    void where_eq_shouldMatchEqualValue() {
        ISpecification<User> spec = Specification.where(User::getName).eq("Alice").build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 20, "ACTIVE")));
    }

    @Test
    void where_neq_shouldMatchDifferentValue() {
        ISpecification<User> spec = Specification.where(User::getName).neq("Alice").build();
        assertFalse(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 20, "ACTIVE")));
    }

    @Test
    void where_gt_shouldMatchGreaterValue() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 18, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 17, "ACTIVE")));
    }

    @Test
    void where_gte_shouldMatchGreaterOrEqualValue() {
        ISpecification<User> spec = Specification.where(User::getAge).gte(18).build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 18, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 17, "ACTIVE")));
    }

    @Test
    void where_lt_shouldMatchLesserValue() {
        ISpecification<User> spec = Specification.where(User::getAge).lt(30).build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 30, "ACTIVE")));
    }

    @Test
    void where_lte_shouldMatchLesserOrEqualValue() {
        ISpecification<User> spec = Specification.where(User::getAge).lte(30).build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 30, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 31, "ACTIVE")));
    }

    @Test
    void where_between_shouldMatchValueInRange() {
        ISpecification<User> spec = Specification.where(User::getAge).between(18, 30).build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 18, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Charlie", 30, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Dave", 17, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Eve", 31, "ACTIVE")));
    }

    @Test
    void where_like_shouldMatchPattern() {
        ISpecification<User> spec = Specification.where(User::getName).like("%Ali%").build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 20, "ACTIVE")));
    }

    @Test
    void where_in_shouldMatchValueInCollection() {
        ISpecification<User> spec = Specification.where(User::getStatus).in("ACTIVE", "VIP").build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 20, "VIP")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 20, "INACTIVE")));
    }

    @Test
    void where_isNull_shouldMatchNullValue() {
        ISpecification<User> spec = Specification.where(User::getName).isNull().build();
        assertTrue(spec.isSatisfiedBy(new User(null, 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
    }

    @Test
    void where_isNotNull_shouldMatchNonNullValue() {
        ISpecification<User> spec = Specification.where(User::getName).isNotNull().build();
        assertFalse(spec.isSatisfiedBy(new User(null, 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
    }

    // --- Combined condition tests ---

    @Test
    void and_shouldCombineConditions() {
        ISpecification<User> spec = Specification.where(User::getName).eq("Alice")
            .and(User::getAge).gt(18)
            .build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Alice", 17, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 20, "ACTIVE")));
    }

    @Test
    void or_shouldCombineConditionsWithOr() {
        ISpecification<User> spec = Specification.where(User::getName).eq("Alice")
            .or(User::getName).eq("Bob")
            .build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 20, "ACTIVE")));
    }

    @Test
    void complexQuery_shouldWorkCorrectly() {
        ISpecification<User> spec = Specification.where(User::getAge).gte(18)
            .and(User::getStatus).eq("ACTIVE")
            .or(User::getStatus).eq("VIP")
            .build();

        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 17, "VIP")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 17, "INACTIVE")));
    }

    // --- Order tests ---

    @Test
    void orderBy_shouldRegisterOrderByExpression() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getAge)
            .build();

        List<OrderExpressionInfo<User>> orders = toList(spec.getOrderExpressions());
        assertEquals(1, orders.size());
        assertEquals(OrderTypeEnum.ORDER_BY, orders.get(0).getOrderType());

        List<User> users = new ArrayList<>(Arrays.asList(
            new User("Charlie", 30, "ACTIVE"),
            new User("Alice", 20, "ACTIVE"),
            new User("Bob", 25, "ACTIVE")
        ));
        users.sort(orders.get(0).getKeySelectorFunc());
        assertEquals(20, users.get(0).getAge());
        assertEquals(25, users.get(1).getAge());
        assertEquals(30, users.get(2).getAge());
    }

    @Test
    void orderByDescending_shouldRegisterOrderByDescendingExpression() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderByDescending(User::getAge)
            .build();

        List<OrderExpressionInfo<User>> orders = toList(spec.getOrderExpressions());
        assertEquals(1, orders.size());
        assertEquals(OrderTypeEnum.ORDER_BY_DESCENDING, orders.get(0).getOrderType());

        List<User> users = new ArrayList<>(Arrays.asList(
            new User("Alice", 20, "ACTIVE"),
            new User("Charlie", 30, "ACTIVE"),
            new User("Bob", 25, "ACTIVE")
        ));
        users.sort(orders.get(0).getKeySelectorFunc());
        assertEquals(30, users.get(0).getAge());
        assertEquals(25, users.get(1).getAge());
        assertEquals(20, users.get(2).getAge());
    }

    @Test
    void thenBy_shouldRegisterThenByExpression() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getStatus)
            .thenBy(User::getAge)
            .build();

        List<OrderExpressionInfo<User>> orders = toList(spec.getOrderExpressions());
        assertEquals(2, orders.size());
        assertEquals(OrderTypeEnum.ORDER_BY, orders.get(0).getOrderType());
        assertEquals(OrderTypeEnum.THEN_BY, orders.get(1).getOrderType());
    }

    @Test
    void thenByDescending_shouldRegisterThenByDescendingExpression() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getStatus)
            .thenByDescending(User::getAge)
            .build();

        List<OrderExpressionInfo<User>> orders = toList(spec.getOrderExpressions());
        assertEquals(2, orders.size());
        assertEquals(OrderTypeEnum.ORDER_BY, orders.get(0).getOrderType());
        assertEquals(OrderTypeEnum.THEN_BY_DESCENDING, orders.get(1).getOrderType());
    }

    @Test
    void thenBy_withoutOrderBy_shouldThrow() {
        assertThrows(IllegalStateException.class, () ->
            Specification.where(User::getAge).gt(0)
                .thenBy(User::getName)
        );
    }

    // --- OR-after-orderBy regression test ---

    @Test
    void orAfterOrderBy_shouldPreserveOrSemantics() {
        ISpecification<User> spec = Specification.where(User::getName).eq("Alice")
            .orderBy(User::getAge)
            .or(User::getName).eq("Bob")
            .build();

        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 30, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 25, "ACTIVE")));
    }

    // --- Type inference tests ---

    @Test
    void noTypeWitness_neededForStringField() {
        ISpecification<User> spec = Specification.where(User::getName).eq("Alice").build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
    }

    @Test
    void noTypeWitness_neededForIntField() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
    }

    @Test
    void build_shouldReturnSpecification() {
        ISpecification<User> spec = Specification.where(User::getName).eq("Alice").build();
        assertNotNull(spec);
        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
    }

    // --- Edge case tests ---

    @Test
    void multipleThenBy_shouldChainCorrectly() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getStatus)
            .thenBy(User::getAge)
            .thenByDescending(User::getName)
            .build();

        List<OrderExpressionInfo<User>> orders = toList(spec.getOrderExpressions());
        assertEquals(3, orders.size());
        assertEquals(OrderTypeEnum.ORDER_BY, orders.get(0).getOrderType());
        assertEquals(OrderTypeEnum.THEN_BY, orders.get(1).getOrderType());
        assertEquals(OrderTypeEnum.THEN_BY_DESCENDING, orders.get(2).getOrderType());
    }

    @Test
    void whereAfterOrderBy_shouldWorkCorrectly() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18)
            .orderBy(User::getAge)
            .and(User::getStatus).eq("ACTIVE")
            .build();

        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 20, "INACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Charlie", 17, "ACTIVE")));

        List<OrderExpressionInfo<User>> orders = toList(spec.getOrderExpressions());
        assertEquals(1, orders.size());
    }

    @Test
    void multipleOrderBy_shouldRegisterAll() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0)
            .orderBy(User::getStatus)
            .orderBy(User::getAge)
            .build();

        List<OrderExpressionInfo<User>> orders = toList(spec.getOrderExpressions());
        assertEquals(2, orders.size());
        assertEquals(OrderTypeEnum.ORDER_BY, orders.get(0).getOrderType());
        assertEquals(OrderTypeEnum.ORDER_BY, orders.get(1).getOrderType());
    }

    @Test
    void orAsFirstCondition_shouldBehaveAsAnd() {
        ISpecification<User> spec = Specification.where(User::getName).eq("Alice")
            .or(User::getAge).gt(18)
            .build();

        assertTrue(spec.isSatisfiedBy(new User("Alice", 20, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Alice", 17, "ACTIVE")));
        assertTrue(spec.isSatisfiedBy(new User("Bob", 20, "ACTIVE")));
        assertFalse(spec.isSatisfiedBy(new User("Bob", 17, "ACTIVE")));
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
