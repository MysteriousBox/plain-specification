package org.plain.specification.core.spi;

/**
 * 当前请求租户ID的提供器 SPI。
 * <p>
 * specification 各存储后端通过本接口获取租户，不自己持有 ThreadLocal，
 * 从而保证与上层认证上下文是同一个租户来源。
 * </p>
 *
 * @author Jayden.Liang
 */
@FunctionalInterface
public interface ITenantProvider {

    /**
     * 获取当前租户ID。
     *
     * @return 当前租户ID；未认证或无租户时返回 {@code null} 或空字符串
     */
    String getTenantId();
}
