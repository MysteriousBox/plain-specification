package org.plain.core.expression;



import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plain.core.visitor.PredicateExpressionVisitor;


import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class IExpressionTest {

    // Helper class for testing
    @Getter
    private static class TestEntity {
        String name;
        int age;

        public TestEntity(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private IExpression<TestEntity> iExpression;

    @BeforeEach
    public void setUp() {
        iExpression = new EqualExpression<>(TestEntity::getAge,20);
    }

    @Test
    public void IExpression_ShouldBeInstantiated() {
        assertNotNull(iExpression,"IExpression should not be null");
    }

    @Test
    public void IExpression_ShouldBeVisitable() {
        PredicateExpressionVisitor<TestEntity> visitor = new PredicateExpressionVisitor<>();
        Predicate<TestEntity> predicate = iExpression.accept(visitor);
        assertNotNull(predicate,"Predicate should not be null");
    }

    @Test
    public void IExpression_ShouldBeVisitable_With_And_Expression() {
        IExpression<TestEntity> iExpression = new AndExpression<>(
                new EqualExpression<>(TestEntity::getAge,20),
                new EqualExpression<>(TestEntity::getName,null)
        );
        assertNotNull(iExpression,"IExpression should not be null");
    }

    @Test
    public void IExpression_ShouldBeVisitable_With_Or_Expression() {
        IExpression<TestEntity> iExpression = new OrExpression<>(
                new EqualExpression<>(TestEntity::getAge,20),
                new EqualExpression<>(TestEntity::getName,null)
        );
        assertNotNull(iExpression,"IExpression should not be null");
    }

    // 测试 accept 结果是否正确
    @Test
    public void IExpression_ShouldBeVisitable_With_And_Expression_And_Accept() {
        IExpression<TestEntity> iExpression = new AndExpression<>(
                new EqualExpression<>(TestEntity::getAge,20),
                new EqualExpression<>(TestEntity::getName,null)
        );
        PredicateExpressionVisitor<TestEntity> visitor = new PredicateExpressionVisitor<>();
        Predicate<TestEntity> predicate = iExpression.accept(visitor);
        assertNotNull(predicate,"Predicate should not be null");
    }

    // 测试 accept 结果是否和预期一致
    @Test
    public void IExpression_ShouldBeVisitable_With_And_Expression_And_Accept_And_Predicate() {
        IExpression<TestEntity> iExpression = new AndExpression<>(
                new EqualExpression<>(TestEntity::getAge, 20),
                new EqualExpression<>(TestEntity::getName, null)
        );
        PredicateExpressionVisitor<TestEntity> visitor = new PredicateExpressionVisitor<>();
        Predicate<TestEntity> predicate = iExpression.accept(visitor);

        // 创建一个匹配的 Client 对象
        TestEntity matchingClient = new TestEntity(null,20);


        // 创建一个不匹配的 Client 对象
        TestEntity nonMatchingClient = new TestEntity(null,10);

        assertTrue(predicate.test(matchingClient), "Predicate should return true for matching client");
        assertFalse(predicate.test(nonMatchingClient), "Predicate should return false for non-matching client");
    }


    // 测试 and , or, not, equal, notEqual, greaterThan, greaterThanOrEqual, lessThan, lessThanOrEqual, in, notIn, between, like, isNull, isNotNull, exists, notExists 组合使用
    @Test
    public void IExpression_ShouldBeVisitable_With_And_Expression_And_Accept_And_Predicate_And_Combination() {
        // 不使用 Expressions 创建，而是使用 IExpression 的具体实现类型 进行测试，列如表达式 a&&b||(c&&(d>e&&d<f))
        IExpression<TestEntity> iExpression = new AndExpression<>(
                new EqualExpression<>(TestEntity::getAge, 20),
                new OrExpression<>(
                        new EqualExpression<>(TestEntity::getName, null),
                        new AndExpression<>(
                                new EqualExpression<>(TestEntity::getName, null),
                                new AndExpression<>(
                                        new GreaterThanExpression<>(TestEntity::getAge,  10),
                                        new LessThanExpression<>(TestEntity::getAge, 20)
                                )
                        )
                )
        );



    }

}
