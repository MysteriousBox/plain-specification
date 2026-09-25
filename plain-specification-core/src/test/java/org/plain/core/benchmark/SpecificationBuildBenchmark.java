package org.plain.core.benchmark;

import org.openjdk.jmh.annotations.*;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;

import java.util.concurrent.TimeUnit;

/**
 * JMH 基准测试：Specification 构建性能
 * 
 * 比较快捷 API 和传统 API 的构建性能
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class SpecificationBuildBenchmark {

    @Benchmark
    public ISpecification<User> benchmarkQuickApiSimple() {
        return Specification.where(User::getAge).gte(30).build();
    }

    @Benchmark
    public ISpecification<User> benchmarkQuickApiComplex() {
        return Specification.where(User::getAge).gte(25)
                .and(User::getStatus).eq("ACTIVE")
                .or(User::getName).like("%test%")
                .build();
    }

    @Benchmark
    public ISpecification<User> benchmarkQuickApiWithOrder() {
        return Specification.where(User::getAge).gte(25)
                .and(User::getStatus).eq("ACTIVE")
                .orderBy(User::getAge)
                .thenByDescending(User::getName)
                .build();
    }

    @Benchmark
    public ISpecification<User> benchmarkTraditionalApiSimple() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().greaterThanOrEqual(User::getAge, 30));
        return spec;
    }

    @Benchmark
    public ISpecification<User> benchmarkTraditionalApiComplex() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create()
                .greaterThanOrEqual(User::getAge, 25)
                .and(new org.plain.specification.core.expression.EqualExpression<>(User::getStatus, "ACTIVE"))
                .or(new org.plain.specification.core.expression.LikeExpression<>(User::getName, "%test%")));
        return spec;
    }

    @Benchmark
    public ISpecification<User> benchmarkTraditionalApiWithOrder() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create()
                .greaterThanOrEqual(User::getAge, 25)
                .and(new org.plain.specification.core.expression.EqualExpression<>(User::getStatus, "ACTIVE")));
        spec.query().orderBy(Expressions.<User>create().orderBy(User::getAge));
        spec.query().orderBy(Expressions.<User>create().orderByDescending(User::getName));
        return spec;
    }

    /**
     * 测试实体类
     */
    public static class User {
        private final String name;
        private final int age;
        private final String status;

        public User(String name, int age, String status) {
            this.name = name;
            this.age = age;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public String getStatus() {
            return status;
        }
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
