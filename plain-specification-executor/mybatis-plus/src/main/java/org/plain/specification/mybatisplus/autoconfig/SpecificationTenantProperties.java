package org.plain.specification.mybatisplus.autoconfig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 多租户配置属性。
 *
 * @author Jayden.Liang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "plain.tenant")
public class SpecificationTenantProperties {

    /**
     * 是否开启多租户过滤
     */
    private boolean enabled = false;

    /**
     * 租户列名
     */
    private String column = "tenant_id";

    /**
     * 豁免租户过滤的表名（不区分大小写，可带 schema 前缀书写）
     */
    private Set<String> ignoreTables = new LinkedHashSet<>();
}
