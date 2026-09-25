package org.plain.specification.mybatisplus.autoconfig;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.plain.specification.core.evaluate.ISpecificationEvaluator;
import org.plain.specification.core.evaluate.InMemorySpecificationEvaluator;
import org.plain.specification.mybatisplus.context.PlainTenantLineHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * MyBatis-Plus 模块自动配置。
 * <p>
 * 提供默认的异步线程池、分页拦截器和可选的多租户拦截器。
 * 当用户自定义 {@link Executor} 或 {@link MybatisPlusInterceptor} bean 时自动退让。
 * </p>
 * <p>
 * 多租户拦截器通过 {@code plain.tenant.enabled=true} 开启，
 * 租户列名可通过 {@code plain.tenant.column} 自定义（默认 {@code tenant_id}）。
 * </p>
 *
 * @author Jayden.Liang
 */
@Configuration
@EnableConfigurationProperties(SpecificationAsyncProperties.class)
public class MybatisPlusAutoConfiguration {

    @Bean(name = "plainSpecificationAsyncExecutor")
    @Primary
    @ConditionalOnMissingBean(Executor.class)
    public ThreadPoolTaskExecutor plainSpecificationAsyncExecutor(SpecificationAsyncProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(PlainTenantLineHandler.class)
    @ConditionalOnProperty(prefix = "plain.tenant", name = "enabled", havingValue = "true")
    public PlainTenantLineHandler plainTenantLineHandler(
            @Value("${plain.tenant.column:tenant_id}") String tenantColumn) {
        return new PlainTenantLineHandler(tenantColumn);
    }

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            ObjectProvider<PlainTenantLineHandler> tenantHandlerProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PlainTenantLineHandler handler = tenantHandlerProvider.getIfAvailable();
        if (handler != null) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(handler));
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(ISpecificationEvaluator.class)
    public InMemorySpecificationEvaluator inMemorySpecificationEvaluator() {
        return InMemorySpecificationEvaluator.DEFAULT;
    }
}
