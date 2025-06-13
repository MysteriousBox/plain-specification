package org.plain.specification;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.plain.core.ISpecification;
import org.plain.core.Specification;
import org.plain.core.builder.ISpecificationBuilder;
import org.plain.core.descriptor.IExpressionDescriptor;
import org.plain.core.expression.Expressions;
import org.plain.core.expression.OrderExpressionInfo;

import java.util.Arrays;

public class MyBatisplusExpressionVisitorTest {


    @Test
    public void testAND() {

        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();

        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().equal(MpSpecificationBuilderTest.Person::getName, "张三").equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().equal(MpSpecificationBuilderTest.Person::getName, "李四").equal(MpSpecificationBuilderTest.Person::getAge, 28));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testOR() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().equal(MpSpecificationBuilderTest.Person::getName, "张三").or().equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().equal(MpSpecificationBuilderTest.Person::getName, "李四").or().equal(MpSpecificationBuilderTest.Person::getAge, 28));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);

    }

    @Test
    public void testNOT() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().not().equal(MpSpecificationBuilderTest.Person::getName, "张三").equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().not().equal(MpSpecificationBuilderTest.Person::getName, "李四").equal(MpSpecificationBuilderTest.Person::getAge, 28));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testIsNull() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().isNull(MpSpecificationBuilderTest.Person::getName).equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().isNull(MpSpecificationBuilderTest.Person::getName).equal(MpSpecificationBuilderTest.Person::getAge, 28));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testIsNotNull() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().notNull(MpSpecificationBuilderTest.Person::getName).equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().notNull(MpSpecificationBuilderTest.Person::getName).equal(MpSpecificationBuilderTest.Person::getAge, 28));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }
    @Test
    public void testLike() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().like(MpSpecificationBuilderTest.Person::getName, "张三").greaterThan(MpSpecificationBuilderTest.Person::getAge, 18))
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().like(MpSpecificationBuilderTest.Person::getName, "李四").lessThan(MpSpecificationBuilderTest.Person::getAge, 28));
        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        specificationTest.getWhereExpressions().forEach(whereExpressionInfo -> {
            QueryWrapper<MpSpecificationBuilderTest.Person> func = whereExpressionInfo.func(new MybatisplusExpressionVisitor<>());
            System.out.println(func.getTargetSql());
        });
    }

    @Test
    public void testIn() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();

        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().in(MpSpecificationBuilderTest.Person::getName, Arrays.asList("张三", "李四")).greaterThan(MpSpecificationBuilderTest.Person::getAge, 18));
        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testNotIn() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().notIn(MpSpecificationBuilderTest.Person::getName, Arrays.asList("张三", "李四")).greaterThan(MpSpecificationBuilderTest.Person::getAge, 18));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testBetween() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().between(MpSpecificationBuilderTest.Person::getAge, 18, 28));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testOrderBy() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().equal(MpSpecificationBuilderTest.Person::getName, "张三").equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .orderBy(Expressions.<MpSpecificationBuilderTest.Person>create().orderBy(MpSpecificationBuilderTest.Person::getAge));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testOrderByDesc() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().equal(MpSpecificationBuilderTest.Person::getName, "张三").equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .orderByDescending(Expressions.<MpSpecificationBuilderTest.Person>create().orderByDescending(MpSpecificationBuilderTest.Person::getAge));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testOrderByDesc2() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().equal(MpSpecificationBuilderTest.Person::getName, "张三").equal(MpSpecificationBuilderTest.Person::getAge, 18))
                .orderByDescending(Expressions.<MpSpecificationBuilderTest.Person>create().orderByDescending(MpSpecificationBuilderTest.Person::getName))
                .orderByDescending(Expressions.<MpSpecificationBuilderTest.Person>create().orderByDescending(MpSpecificationBuilderTest.Person::getAge));
        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testLessThan() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().lessThan(MpSpecificationBuilderTest.Person::getAge, 18));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testNe() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().notEqual(MpSpecificationBuilderTest.Person::getAge, 18));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testGte() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().greaterThanOrEqual(MpSpecificationBuilderTest.Person::getAge, 18));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testLte() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().lessThanOrEqual(MpSpecificationBuilderTest.Person::getAge, 18))
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().greaterThan(MpSpecificationBuilderTest.Person::getAge, 18));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();
        print(specificationTest);
    }

    @Test
    public void testGt() {
        Specification<MpSpecificationBuilderTest.Person> specification = new Specification<>();
        ISpecificationBuilder<MpSpecificationBuilderTest.Person> where = specification.query()
                .where(Expressions.<MpSpecificationBuilderTest.Person>create().greaterThan(MpSpecificationBuilderTest.Person::getAge, 18));

        ISpecification<MpSpecificationBuilderTest.Person> specificationTest = where.getSpecification();

        print(specificationTest);
    }

    private static void print(ISpecification<MpSpecificationBuilderTest.Person> specificationTest) {
        MybatisplusExpressionVisitor<MpSpecificationBuilderTest.Person> objectMybatisplusExpressionVisitor = new MybatisplusExpressionVisitor<>();
        QueryWrapper<MpSpecificationBuilderTest.Person> func = null;
        for (IExpressionDescriptor<MpSpecificationBuilderTest.Person> whereExpression : specificationTest.getWhereExpressions()) {
            func = whereExpression.func(objectMybatisplusExpressionVisitor);
        }
        for (OrderExpressionInfo<MpSpecificationBuilderTest.Person> orderExpression : specificationTest.getOrderExpressions()) {
            func = orderExpression.func(objectMybatisplusExpressionVisitor);
        }
        if (func != null){
            System.out.println(func.getTargetSql());
        }
    }


}
