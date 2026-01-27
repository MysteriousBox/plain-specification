package org.plain.specification.redis;

/**
 * 租户ID提供器接口，便于在多租户环境下灵活获取当前租户。
 * 可由业务自定义实现并通过Spring注入。
 */
@FunctionalInterface
public interface TenantProvider {
    /**
     * 获取当前租户ID，未设置时返回null或空字符串。
     */
    String getTenantId();
}

