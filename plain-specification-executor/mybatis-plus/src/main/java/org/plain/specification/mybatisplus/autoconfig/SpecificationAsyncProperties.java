package org.plain.specification.mybatisplus.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 异步线程池配置属性。
 *
 * @author Jayden.Liang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "plain.specification.async")
public class SpecificationAsyncProperties {

    /** 核心线程数 */
    private int corePoolSize = 16;

    /** 最大线程数 */
    private int maxPoolSize = 32;

    /** 队列容量 */
    private int queueCapacity = 50;

    /** 线程名前缀 */
    private String threadNamePrefix = "plain-spec-";
}
