package org.plain.specification.mybatisplus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    public ThreadPoolTaskExecutor taskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(AsyncConstant.THREAD_CORE_POOL_SIZE);
        executor.setMaxPoolSize(AsyncConstant.MAX_POOL_SIZE);
        executor.setQueueCapacity(AsyncConstant.QUEUE_CAPACITY);
        executor.setThreadNamePrefix(AsyncConstant.THREAD_NAME_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }


}
