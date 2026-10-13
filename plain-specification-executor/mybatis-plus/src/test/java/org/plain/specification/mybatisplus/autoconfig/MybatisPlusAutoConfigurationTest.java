package org.plain.specification.mybatisplus.autoconfig;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.spi.ITenantProvider;
import org.plain.specification.mybatisplus.context.PlainTenantLineHandler;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(MybatisPlusAutoConfiguration.class));

    @Test
    void defaultConfig_shouldCreateExecutor() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutor.class);
            assertThat(executor.getCorePoolSize()).isEqualTo(16);
            assertThat(executor.getMaxPoolSize()).isEqualTo(32);
            assertThat(executor.getThreadNamePrefix()).startsWith("plain-spec-");
        });
    }

    @Test
    void defaultConfig_shouldCreatePaginationInterceptor() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            assertThat(interceptor.getInterceptors()).hasSize(1);
            assertThat(interceptor.getInterceptors().get(0)).isInstanceOf(PaginationInnerInterceptor.class);
        });
    }

    @Test
    void customProperties_shouldOverrideDefaults() {
        contextRunner
                .withPropertyValues(
                        "plain.specification.async.core-pool-size=8",
                        "plain.specification.async.max-pool-size=16",
                        "plain.specification.async.queue-capacity=100",
                        "plain.specification.async.thread-name-prefix=custom-"
                )
                .run(context -> {
                    ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutor.class);
                    assertThat(executor.getCorePoolSize()).isEqualTo(8);
                    assertThat(executor.getMaxPoolSize()).isEqualTo(16);
                    assertThat(executor.getQueueCapacity()).isEqualTo(100);
                    assertThat(executor.getThreadNamePrefix()).startsWith("custom-");
                });
    }

    @Test
    void userDefinedExecutor_shouldBackOff() {
        contextRunner
                .withUserConfiguration(CustomExecutorConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Executor.class);
                    assertThat(context.getBean(Executor.class)).isNotInstanceOf(ThreadPoolTaskExecutor.class);
                });
    }

    @Test
    void userDefinedInterceptor_shouldBackOff() {
        contextRunner
                .withUserConfiguration(CustomInterceptorConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors()).isEmpty();
                });
    }

    @Test
    void tenantEnabledWithoutProvider_shouldFail() {
        contextRunner
                .withPropertyValues("plain.tenant.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("ITenantProvider");
                });
    }

    @Test
    void tenantEnabledWithProvider_shouldRegisterTenantInterceptorBeforePagination() {
        contextRunner
                .withPropertyValues("plain.tenant.enabled=true", "plain.tenant.ignore-tables=client")
                .withUserConfiguration(TenantProviderConfig.class)
                .run(context -> {
                    MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
                    assertThat(interceptor.getInterceptors())
                            .first().isInstanceOf(TenantLineInnerInterceptor.class);
                    assertThat(interceptor.getInterceptors())
                            .last().isInstanceOf(PaginationInnerInterceptor.class);

                    PlainTenantLineHandler handler = context.getBean(PlainTenantLineHandler.class);
                    assertThat(handler.ignoreTable("schema_client.\"CLIENT\"")).isTrue();
                    assertThat(handler.ignoreTable("user_role")).isFalse();
                });
    }

    @Test
    void taskDecorator_shouldBeAppliedToAsyncExecutor() {
        contextRunner
                .withUserConfiguration(MarkingTaskDecoratorConfig.class)
                .run(context -> {
                    ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutor.class);
                    CountDownLatch latch = new CountDownLatch(1);
                    AtomicBoolean decorated = new AtomicBoolean();
                    executor.execute(() -> {
                        decorated.set(MarkingTaskDecoratorConfig.MARKED.get());
                        latch.countDown();
                    });
                    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThat(decorated).isTrue();
                });
    }

    @Configuration
    static class CustomExecutorConfig {
        @Bean
        public Executor customExecutor() {
            return Runnable::run;
        }
    }

    @Configuration
    static class CustomInterceptorConfig {
        @Bean
        public MybatisPlusInterceptor customInterceptor() {
            return new MybatisPlusInterceptor();
        }
    }

    @Configuration
    static class TenantProviderConfig {
        @Bean
        public ITenantProvider tenantProvider() {
            return () -> "000000";
        }
    }

    @Configuration
    static class MarkingTaskDecoratorConfig {

        static final ThreadLocal<Boolean> MARKED = new ThreadLocal<>();

        @Bean
        public TaskDecorator markingDecorator() {
            return runnable -> () -> {
                MARKED.set(Boolean.TRUE);
                try {
                    runnable.run();
                } finally {
                    MARKED.remove();
                }
            };
        }
    }
}
