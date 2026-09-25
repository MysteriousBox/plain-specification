package org.plain.specification.mybatisplus.autoconfig;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

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
}
