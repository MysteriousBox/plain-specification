package org.plain.specification.redis;

/**
 * 租户ID提供器接口，便于在多租户环境下灵活获取当前租户。
 * 可由业务自定义实现并通过Spring注入。
 * @author Jayden.Liang
 */
@FunctionalInterface
public interface TenantProvider {
    /**
     * 获取当前租户ID，用于多租户环境下区分不同租户的数据隔离。
     * 实现应根据当前上下文返回对应的租户标识，未设置时返回null或空字符串。
     *
     * @return 当前租户ID，未设置时返回null或空字符串
     */
    String getTenantId();
}

