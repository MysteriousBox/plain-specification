package org.plain.core.expression;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionsErrorPathTest {

    @Getter
    private static class Item {
        String name;
        int value;

        Item(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    void build_shouldThrow_whenNoExpressionAdded() {
        Expressions<Item> expr = Expressions.create();
        assertThrows(IllegalArgumentException.class, expr::build);
    }

    @Test
    void and_shouldThrow_whenLeftIsNull() {
        Expressions<Item> expr = Expressions.create();
        assertThrows(IllegalArgumentException.class, expr::and);
    }

    @Test
    void or_shouldThrow_whenLeftIsNull() {
        Expressions<Item> expr = Expressions.create();
        assertThrows(IllegalArgumentException.class, expr::or);
    }

    @Test
    void andExpression_shouldCombineDirectly() {
        Expressions<Item> expr = Expressions.create();
        expr.equal(Item::getName, "A");
        expr.and(new EqualExpression<>(Item::getName, "B"));

        Predicate<Item> predicate = expr.build().accept(new PredicateExpressionVisitor<>());
        assertFalse(predicate.test(new Item("A", 1)));
        assertFalse(predicate.test(new Item("B", 1)));
    }

    @Test
    void orExpression_shouldCombineDirectly() {
        Expressions<Item> expr = Expressions.create();
        expr.equal(Item::getName, "A");
        expr.or(new EqualExpression<>(Item::getName, "B"));

        Predicate<Item> predicate = expr.build().accept(new PredicateExpressionVisitor<>());
        assertTrue(predicate.test(new Item("A", 1)));
        assertTrue(predicate.test(new Item("B", 1)));
        assertFalse(predicate.test(new Item("C", 1)));
    }

    @Test
    void notExpression_shouldCombineDirectly() {
        Expressions<Item> expr = Expressions.create();
        expr.equal(Item::getName, "A");
        expr.not(new EqualExpression<>(Item::getName, "B"));

        Predicate<Item> predicate = expr.build().accept(new PredicateExpressionVisitor<>());
        assertTrue(predicate.test(new Item("A", 1)));
        assertFalse(predicate.test(new Item("B", 1)));
    }

    @Test
    void compiler_shouldCompileExpression() {
        Expressions<Item> expr = Expressions.create();
        expr.equal(Item::getName, "test");

        Predicate<Item> predicate = expr.compiler(new PredicateExpressionVisitor<>()).compile();
        assertTrue(predicate.test(new Item("test", 1)));
        assertFalse(predicate.test(new Item("other", 1)));
    }

    @Test
    void notOperator_thenComparison_whenBeforeOperatorIsNull() {
        Expressions<Item> expr = Expressions.create();
        expr.not();
        expr.equal(Item::getName, "A");

        Predicate<Item> predicate = expr.build().accept(new PredicateExpressionVisitor<>());
        assertFalse(predicate.test(new Item("A", 1)));
        assertTrue(predicate.test(new Item("B", 1)));
    }
}
