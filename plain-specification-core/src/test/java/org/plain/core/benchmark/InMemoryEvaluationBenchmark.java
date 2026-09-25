package org.plain.core.benchmark;

import org.openjdk.jmh.annotations.*;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.evaluate.InMemorySpecificationEvaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH 基准测试：内存评估性能
 * 
 * 运行方式：
 * 1. 编译：mvn clean compile test-compile
 * 2. 运行：java -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout) org.openjdk.jmh.Main InMemoryEvaluationBenchmark
 * 
 * 或简化运行（在 IDE 中）：直接运行 main 方法
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class InMemoryEvaluationBenchmark {

    @Param({"100", "1000", "10000"})
    private int dataSize;

    private List<User> users;
    private ISpecification<User> simpleSpec;
    private ISpecification<User> complexSpec;
    private ISpecification<User> specWithOrder;

    @Setup
    public void setup() {
        users = new ArrayList<>(dataSize);
        for (int i = 0; i < dataSize; i++) {
            users.add(new User("User" + i, 18 + (i % 50), i % 2 == 0 ? "ACTIVE" : "INACTIVE"));
        }

        // 简单查询：单条件
        simpleSpec = Specification.where(User::getAge).gte(30).build();

        // 复杂查询：多条件 AND + OR
        complexSpec = Specification.where(User::getAge).gte(25)
                .and(User::getStatus).eq("ACTIVE")
                .or(User::getAge).lte(20)
                .build();

        // 带排序的查询
        specWithOrder = Specification.where(User::getAge).gte(25)
                .orderBy(User::getAge)
                .thenByDescending(User::getName)
                .build();
    }

    @Benchmark
    public Collection<User> benchmarkSimpleFilter() {
        return simpleSpec.evaluate(users);
    }

    @Benchmark
    public Collection<User> benchmarkComplexFilter() {
        return complexSpec.evaluate(users);
    }

    @Benchmark
    public Collection<User> benchmarkFilterWithOrder() {
        return specWithOrder.evaluate(users);
    }

    @Benchmark
    public Collection<User> benchmarkInMemoryEvaluator() {
        return InMemorySpecificationEvaluator.DEFAULT.evaluate(users, simpleSpec);
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

    /**
     * 直接运行基准测试（用于 IDE 调试）
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
