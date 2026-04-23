package org.plain.specification.redis.autoconfig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 全局 Redis 仓储配置。
 * <p>
 * 可通过 application.yml/application.properties 中的 plain.redis.repo.* 属性覆盖。
 * </p>
 *
 * @author Jayden.Liang
 * @since 1.0
 */
/**
 * Class RedisRepositoryProperties.
 *
 * @author Jayden.Liang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "plain.redis.repo")
@Validated
public class RedisRepositoryProperties {

    /** 默认 TTL（秒），必须大于 0。 */
    @Min(1)
    private long defaultTtl = 3600;

    /** 批量操作的默认批次大小，必须大于 0。 */
    @Min(1)
    private int batchSize = 100;

    /** 值对象存储的 key 前缀，不能为空。 */
    @NotBlank
    private String keyPrefix = "plain:";

    /** 运行环境标识（可选），例如 prod/staging/dev，用于生产级命名空间前缀 */
    private String env;

    /** 默认的 zset 名称，用于在未传 zSetKey 时使用（例如 "index" 或 "zset"） */
    @NotBlank
    private String defaultZsetName = "index";
}
