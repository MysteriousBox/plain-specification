package org.plain.specification.mybatisplus.config;

public interface AsyncConstant {

    String THREAD_POOL_TASK_EXECUTOR_NAME = "taskExecutor";

    String THREAD_NAME_PREFIX = "async-";

    Integer THREAD_CORE_POOL_SIZE = 16;

    Integer MAX_POOL_SIZE = 32;

    Integer QUEUE_CAPACITY = 50;
}
