package org.plain.core.visitor;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class PredicateExpressionVisitorTest {

    @Getter
    private static class User {
        String name;
        Integer age;
        List<String> tags;

        User(String name, Integer age) {
            this.name = name;
            this.age = age;
            this.tags = Collections.emptyList();
        }

        User(String name, Integer age, List<String> tags) {
            this.name = name;
            this.age = age;
            this.tags = tags;
        }
    }

    private final PredicateExpressionVisitor<User> visitor = new PredicateExpressionVisitor<>();

    // --- Equal ---

    @Test
    void visitEqual_bothNull_shouldReturnTrue() {
        Predicate<User> p = new EqualExpression<User, String>(User::getName, null).accept(visitor);
        assertTrue(p.test(new User(null, 20)));
    }

    @Test
    void visitEqual_leftNullRightNonNull_shouldReturnFalse() {
        Predicate<User> p = new EqualExpression<User, String>(User::getName, "Alice").accept(visitor);
        assertFalse(p.test(new User(null, 20)));
    }

    @Test
    void visitEqual_sameValues_shouldReturnTrue() {
        Predicate<User> p = new EqualExpression<User, String>(User::getName, "Alice").accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitEqual_differentValues_shouldReturnFalse() {
        Predicate<User> p = new EqualExpression<User, String>(User::getName, "Alice").accept(visitor);
        assertFalse(p.test(new User("Bob", 20)));
    }

    // --- NotEqual ---

    @Test
    void visitNotEqual_bothNull_shouldReturnFalse() {
        Predicate<User> p = new NotEqualExpression<User, String>(User::getName, null).accept(visitor);
        assertFalse(p.test(new User(null, 20)));
    }

    @Test
    void visitNotEqual_leftNullRightNonNull_shouldReturnFalse() {
        Predicate<User> p = new NotEqualExpression<User, String>(User::getName, "Alice").accept(visitor);
        assertFalse(p.test(new User(null, 20)));
    }

    @Test
    void visitNotEqual_differentValues_shouldReturnTrue() {
        Predicate<User> p = new NotEqualExpression<User, String>(User::getName, "Alice").accept(visitor);
        assertTrue(p.test(new User("Bob", 20)));
    }

    @Test
    void visitNotEqual_sameValues_shouldReturnFalse() {
        Predicate<User> p = new NotEqualExpression<User, String>(User::getName, "Alice").accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    // --- GreaterThan ---

    @Test
    void visitGt_nullValue_shouldThrow() {
        Predicate<User> p = new GreaterThanExpression<User, Integer>(User::getAge, 10).accept(visitor);
        assertThrows(IllegalArgumentException.class, () -> p.test(new User(null, null)));
    }

    @Test
    void visitGt_greaterValue_shouldReturnTrue() {
        Predicate<User> p = new GreaterThanExpression<User, Integer>(User::getAge, 10).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    // --- LessThan ---

    @Test
    void visitLt_nullValue_shouldThrow() {
        Predicate<User> p = new LessThanExpression<User, Integer>(User::getAge, 30).accept(visitor);
        assertThrows(IllegalArgumentException.class, () -> p.test(new User(null, null)));
    }

    @Test
    void visitLt_lesserValue_shouldReturnTrue() {
        Predicate<User> p = new LessThanExpression<User, Integer>(User::getAge, 30).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    // --- GreaterThanOrEqual ---

    @Test
    void visitGte_nullValue_shouldThrow() {
        Predicate<User> p = new GreaterThanOrEqualExpression<User, Integer>(User::getAge, 10).accept(visitor);
        assertThrows(IllegalArgumentException.class, () -> p.test(new User(null, null)));
    }

    @Test
    void visitGte_greaterValue_shouldReturnTrue() {
        Predicate<User> p = new GreaterThanOrEqualExpression<User, Integer>(User::getAge, 10).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitGte_equalValue_shouldReturnTrue() {
        Predicate<User> p = new GreaterThanOrEqualExpression<User, Integer>(User::getAge, 20).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitGte_lesserValue_shouldReturnFalse() {
        Predicate<User> p = new GreaterThanOrEqualExpression<User, Integer>(User::getAge, 30).accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    // --- LessThanOrEqual ---

    @Test
    void visitLte_nullValue_shouldThrow() {
        Predicate<User> p = new LessThanOrEqualExpression<User, Integer>(User::getAge, 30).accept(visitor);
        assertThrows(IllegalArgumentException.class, () -> p.test(new User(null, null)));
    }

    @Test
    void visitLte_lesserValue_shouldReturnTrue() {
        Predicate<User> p = new LessThanOrEqualExpression<User, Integer>(User::getAge, 30).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitLte_equalValue_shouldReturnTrue() {
        Predicate<User> p = new LessThanOrEqualExpression<User, Integer>(User::getAge, 20).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitLte_greaterValue_shouldReturnFalse() {
        Predicate<User> p = new LessThanOrEqualExpression<User, Integer>(User::getAge, 10).accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    // --- Between ---

    @Test
    void visitBetween_nullValue_shouldThrow() {
        Predicate<User> p = new BetweenExpression<User, Integer>(User::getAge, 10, 30).accept(visitor);
        assertThrows(IllegalArgumentException.class, () -> p.test(new User(null, null)));
    }

    @Test
    void visitBetween_valueInRange_shouldReturnTrue() {
        Predicate<User> p = new BetweenExpression<User, Integer>(User::getAge, 10, 30).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitBetween_valueOutOfRange_shouldReturnFalse() {
        Predicate<User> p = new BetweenExpression<User, Integer>(User::getAge, 10, 30).accept(visitor);
        assertFalse(p.test(new User("Alice", 5)));
    }

    // --- Like ---

    @Test
    void visitLike_nullValue_shouldReturnFalse() {
        Predicate<User> p = new LikeExpression<>(User::getName, "%Alice%").accept(visitor);
        assertFalse(p.test(new User(null, 20)));
    }

    @Test
    void visitLike_matchingPattern_shouldReturnTrue() {
        Predicate<User> p = new LikeExpression<>(User::getName, "%Ali%").accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitLike_nonMatchingPattern_shouldReturnFalse() {
        Predicate<User> p = new LikeExpression<>(User::getName, "%Bob%").accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    // --- In / NotIn ---

    @Test
    void visitIn_valueInCollection_shouldReturnTrue() {
        Predicate<User> p = new InExpression<User, String>(User::getName, Arrays.asList("Alice", "Bob")).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitIn_valueNotInCollection_shouldReturnFalse() {
        Predicate<User> p = new InExpression<User, String>(User::getName, Arrays.asList("Alice", "Bob")).accept(visitor);
        assertFalse(p.test(new User("Charlie", 20)));
    }

    @Test
    void visitNotIn_valueNotInCollection_shouldReturnTrue() {
        Predicate<User> p = new NotInExpression<User, String>(User::getName, Arrays.asList("Alice", "Bob")).accept(visitor);
        assertTrue(p.test(new User("Charlie", 20)));
    }

    // --- IsNull / IsNotNull ---

    @Test
    void visitIsNull_nullValue_shouldReturnTrue() {
        Predicate<User> p = new IsNullExpression<User, String>(User::getName).accept(visitor);
        assertTrue(p.test(new User(null, 20)));
    }

    @Test
    void visitIsNull_nonNullValue_shouldReturnFalse() {
        Predicate<User> p = new IsNullExpression<User, String>(User::getName).accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    @Test
    void visitIsNotNull_nonNullValue_shouldReturnTrue() {
        Predicate<User> p = new IsNotNullExpression<User, String>(User::getName).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    // --- Exists / NotExists ---

    @Test
    void visitExists_nullCollection_shouldReturnFalse() {
        Predicate<User> p = new ExistsExpression<User, String>(
            u -> null,
            new EqualExpression<String, String>(s -> s, "test")
        ).accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    @Test
    void visitExists_emptyCollection_shouldReturnFalse() {
        Predicate<User> p = new ExistsExpression<User, String>(
            u -> Collections.emptyList(),
            new EqualExpression<String, String>(s -> s, "test")
        ).accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    @Test
    void visitNotExists_nullCollection_shouldReturnTrue() {
        Predicate<User> p = new NotExistsExpression<User, String>(
            u -> null,
            new EqualExpression<String, String>(s -> s, "test")
        ).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitNotExists_emptyCollection_shouldReturnTrue() {
        Predicate<User> p = new NotExistsExpression<User, String>(
            u -> Collections.emptyList(),
            new EqualExpression<String, String>(s -> s, "test")
        ).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    // --- And / Or / Not ---

    @Test
    void visitAnd_bothTrue_shouldReturnTrue() {
        AndExpression<User> expr = new AndExpression<>(
            new EqualExpression<>(User::getName, "Alice"),
            new GreaterThanExpression<>(User::getAge, 10)
        );
        assertTrue(expr.accept(visitor).test(new User("Alice", 20)));
    }

    @Test
    void visitAnd_oneFalse_shouldReturnFalse() {
        AndExpression<User> expr = new AndExpression<>(
            new EqualExpression<>(User::getName, "Alice"),
            new GreaterThanExpression<>(User::getAge, 30)
        );
        assertFalse(expr.accept(visitor).test(new User("Alice", 20)));
    }

    @Test
    void visitOr_oneTrue_shouldReturnTrue() {
        OrExpression<User> expr = new OrExpression<>(
            new EqualExpression<>(User::getName, "Alice"),
            new EqualExpression<>(User::getName, "Bob")
        );
        assertTrue(expr.accept(visitor).test(new User("Bob", 20)));
    }

    @Test
    void visitNot_shouldNegate() {
        NotExpression<User> expr = new NotExpression<>(new EqualExpression<>(User::getName, "Alice"));
        Predicate<User> p = expr.accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
        assertTrue(p.test(new User("Bob", 20)));
    }

    // --- Boundary tests ---

    @Test
    void visitGt_equalValue_shouldReturnFalse() {
        Predicate<User> p = new GreaterThanExpression<User, Integer>(User::getAge, 20).accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    @Test
    void visitLt_equalValue_shouldReturnFalse() {
        Predicate<User> p = new LessThanExpression<User, Integer>(User::getAge, 20).accept(visitor);
        assertFalse(p.test(new User("Alice", 20)));
    }

    @Test
    void visitBetween_atLowerBound_shouldReturnTrue() {
        Predicate<User> p = new BetweenExpression<User, Integer>(User::getAge, 20, 30).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    @Test
    void visitBetween_atUpperBound_shouldReturnTrue() {
        Predicate<User> p = new BetweenExpression<User, Integer>(User::getAge, 10, 20).accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
    }

    // --- Nested expressions ---

    @Test
    void nestedAndInsideOr_shouldEvaluateCorrectly() {
        OrExpression<User> expr = new OrExpression<>(
            new EqualExpression<>(User::getName, "Bob"),
            new AndExpression<>(
                new EqualExpression<>(User::getName, "Alice"),
                new GreaterThanExpression<>(User::getAge, 10)
            )
        );
        Predicate<User> p = expr.accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
        assertTrue(p.test(new User("Bob", 5)));
        assertFalse(p.test(new User("Charlie", 20)));
    }

    @Test
    void nestedOrInsideAnd_shouldEvaluateCorrectly() {
        AndExpression<User> expr = new AndExpression<>(
            new OrExpression<>(
                new EqualExpression<>(User::getName, "Alice"),
                new EqualExpression<>(User::getName, "Bob")
            ),
            new GreaterThanExpression<>(User::getAge, 10)
        );
        Predicate<User> p = expr.accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
        assertTrue(p.test(new User("Bob", 15)));
        assertFalse(p.test(new User("Alice", 5)));
        assertFalse(p.test(new User("Charlie", 20)));
    }

    @Test
    void deeplyNestedExpressions_shouldEvaluateCorrectly() {
        AndExpression<User> expr = new AndExpression<>(
            new OrExpression<>(
                new AndExpression<>(
                    new GreaterThanExpression<>(User::getAge, 10),
                    new LessThanExpression<>(User::getAge, 30)
                ),
                new EqualExpression<>(User::getName, "Admin")
            ),
            new NotExpression<>(new EqualExpression<>(User::getName, "Blocked"))
        );
        Predicate<User> p = expr.accept(visitor);
        assertTrue(p.test(new User("Alice", 20)));
        assertTrue(p.test(new User("Admin", 5)));
        assertFalse(p.test(new User("Blocked", 20)));
        assertFalse(p.test(new User("Alice", 40)));
    }
}
