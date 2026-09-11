package org.plain.specification.mybatisplus.context;

/**
 * 租户上下文，基于 ThreadLocal 存储当前请求的租户ID。
 * <p>
 * 在请求入口处设置，请求结束后务必调用 {@link #clear()} 防止线程池复用导致数据泄漏。
 * </p>
 *
 * @author Jayden.Liang
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
