package org.plain.specification.redis;

/**
 * Utility class that provides {@link RedisRepositoryConfig.KeyPrefixProvider} factory methods.
 * <p>
 * The default provider appends the entity simple name to the global prefix.
 * The enterprise provider additionally prepends an environment segment and a tenant segment.
 * </p>
 *
 * @author Jayden.Liang
 * @since 1.0
 */
public final class DefaultKeyPrefixProvider {

    private static final String COLON = ":";

    private DefaultKeyPrefixProvider() {
        // no-op
    }

    /**
     * Default KeyPrefixProvider: append entity simple name to global prefix (legacy behaviour).
     */
    public static RedisRepositoryConfig.KeyPrefixProvider defaultKeyPrefixProvider() {
        return (clazz, globalPrefix) -> buildKeyPrefix(null, null, clazz, globalPrefix);
    }

    /**
     * 企业级 KeyPrefixProvider 工厂：支持 env + tenantProvider + globalPrefix + entity。
     * @param env 环境标识
     * @param tenantProvider 租户ID提供器
     * @return KeyPrefixProvider 实例
     */
    public static RedisRepositoryConfig.KeyPrefixProvider enterpriseKeyPrefixProvider(String env, TenantProvider tenantProvider) {
        return (clazz, globalPrefix) -> buildKeyPrefix(env, tenantProvider == null ? null : tenantProvider.getTenantId(), clazz, globalPrefix);
    }

    private static String buildKeyPrefix(String env, String tenant, Class<?> clazz, String globalPrefix) {
        StringBuilder sb = new StringBuilder();
        appendSegment(sb, env);
        appendSegment(sb, tenant);
        appendGlobalPrefix(sb, globalPrefix);
        String entityName = (clazz == null) ? "Entity" : clazz.getSimpleName();
        sb.append(entityName).append(COLON);
        return sb.toString();
    }

    private static void appendSegment(StringBuilder sb, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(value).append(COLON);
        }
    }

    private static void appendGlobalPrefix(StringBuilder sb, String globalPrefix) {
        if (globalPrefix != null && !globalPrefix.isEmpty()) {
            sb.append(globalPrefix);
            if (!globalPrefix.endsWith(COLON)) {
                sb.append(COLON);
            }
        }
    }
}
