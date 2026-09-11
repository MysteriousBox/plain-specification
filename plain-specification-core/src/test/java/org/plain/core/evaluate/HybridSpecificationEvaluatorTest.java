package org.plain.core.evaluate;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.Specification;
import org.plain.specification.core.evaluate.HybridSpecificationEvaluator;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.spi.ISpecificationExecutor;
import org.plain.specification.core.visitor.IExpressionVisitor;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HybridSpecificationEvaluatorTest {

    @Getter
    private static class User {
        String name;
        int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static class MockExecutor<T> implements ISpecificationExecutor<T> {
        private final Set<Class<? extends IExpression<T>>> supported;
        private final List<T> data;
        boolean executeCalled = false;

        MockExecutor(Set<Class<? extends IExpression<T>>> supported, List<T> data) {
            this.supported = supported;
            this.data = data;
        }

        @Override
        public IExpressionVisitor<T, ?> getVisitor() {
            return new PredicateExpressionVisitor<>();
        }

        @Override
        public List<T> execute(ISpecification<T> specification) {
            executeCalled = true;
            return new ArrayList<>(data);
        }

        @Override
        public IPageResult<T> execute(ISpecification<T> specification, PageQuery pageQuery) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Class<? extends IExpression<T>>> supportedExpressions() {
            return supported;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void canPushDown_shouldReturnTrue_whenAllExpressionsSupported() {
        Set<Class<? extends IExpression<User>>> supported = new HashSet<>();
        supported.add((Class) EqualExpression.class);
        supported.add((Class) AndExpression.class);

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));

        assertTrue(HybridSpecificationEvaluator.canPushDown(
            new MockExecutor<>(supported, Collections.emptyList()), spec));
    }

    @SuppressWarnings("unchecked")
    @Test
    void canPushDown_shouldReturnFalse_whenExpressionNotSupported() {
        Set<Class<? extends IExpression<User>>> supported = new HashSet<>();
        supported.add((Class) EqualExpression.class);

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 18));

        assertFalse(HybridSpecificationEvaluator.canPushDown(
            new MockExecutor<>(supported, Collections.emptyList()), spec));
    }

    @Test
    void canPushDown_shouldReturnFalse_whenSupportedExpressionsEmpty() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));

        assertFalse(HybridSpecificationEvaluator.canPushDown(
            new MockExecutor<>(Collections.emptySet(), Collections.emptyList()), spec));
    }

    @SuppressWarnings("unchecked")
    @Test
    void evaluate_shouldDelegateToExecutor_whenCanPushDown() {
        Set<Class<? extends IExpression<User>>> supported = new HashSet<>();
        supported.add((Class) EqualExpression.class);

        List<User> data = Arrays.asList(new User("Alice", 20), new User("Bob", 25));
        MockExecutor<User> executor = new MockExecutor<>(supported, data);
        HybridSpecificationEvaluator<User> evaluator = new HybridSpecificationEvaluator<>(executor);

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));

        Collection<User> result = evaluator.evaluate(data, spec);
        assertTrue(executor.executeCalled);
    }

    @Test
    void evaluate_shouldFallbackToMemory_whenCannotPushDown() {
        List<User> data = Arrays.asList(new User("Alice", 20), new User("Bob", 25));
        MockExecutor<User> executor = new MockExecutor<>(Collections.emptySet(), data);
        HybridSpecificationEvaluator<User> evaluator = new HybridSpecificationEvaluator<>(executor);

        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));

        Collection<User> result = evaluator.evaluate(data, spec);
        assertFalse(executor.executeCalled);
        assertEquals(1, result.size());
    }

    @Test
    void constructor_shouldThrow_whenExecutorIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new HybridSpecificationEvaluator<>(null));
    }
}
