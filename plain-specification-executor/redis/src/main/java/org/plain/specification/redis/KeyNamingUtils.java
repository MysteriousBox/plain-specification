package org.plain.specification.redis;

import java.util.Objects;

/**
 * Utility for computing per-entity key prefixes and ZSet keys.
 * <p>
 * Encapsulates naming rules so they are consistent and testable across the codebase.
 * </p>
 *
 * @author Plain
 * @since 1.0
 */
public final class KeyNamingUtils {

    private KeyNamingUtils() {
        // utility
    }

    /**
     * Compute the per-entity prefix.
     * <p>
     * If a {@link RedisRepositoryConfig.KeyPrefixProvider} is provided it will be invoked first;
     * when it returns {@code null} an empty prefix is used. When no provider is supplied a
     * safe default is returned: {@code [globalPrefix:]EntitySimpleName:} (globalPrefix may be empty).
     * </p>
     *
     * @param provider     optional provider (may be {@code null})
     * @param entityClass  entity class (may be {@code null})
     * @param globalPrefix configured global prefix (may be {@code null} or empty)
     * @return computed prefix (never {@code null}) and normally ends with ':' when non-empty
     */
    public static String computePrefix(RedisRepositoryConfig.KeyPrefixProvider provider,
                                       Class<?> entityClass,
                                       String globalPrefix) {
        if (provider != null) {
            String p = provider.provide(entityClass, globalPrefix);
            return (p == null) ? "" : p;
        }
        final String gp = nullToEmpty(globalPrefix);
        final String entityName = getEntityName(entityClass);
        StringBuilder sb = new StringBuilder();
        if (!isNullOrEmpty(gp)) {
            sb.append(gp);
            if (!gp.endsWith(":")) {
                sb.append(":");
            }
        }
        sb.append(entityName).append(":");
        return sb.toString();
    }

    /**
     * Compute the ZSet key by prefixing the provided zSet name with the computed prefix.
     * <ul>
     *   <li>If {@code prefix} is empty: return {@code zSetKey} if provided, otherwise {@code defaultName}.</li>
     *   <li>If {@code prefix} is non-empty: ensure a single ':' separator and return
     *       prefix + (zSetKey or defaultName).</li>
     *   <li>If {@code zSetKey} starts with ':' it is treated as a relative name and the leading ':' is dropped.</li>
     * </ul>
     *
     * @param prefix      computed prefix (may be {@code null} or empty)
     * @param zSetKey     requested zset name (may be {@code null} or empty)
     * @param defaultName default zset name when none provided (must not be {@code null} or empty)
     * @return full ZSet key (never {@code null})
     */
    public static String computeZsetKey(String prefix, String zSetKey, String defaultName) {
        Objects.requireNonNull(defaultName, "defaultName must not be null");
        if (defaultName.isEmpty()) {
            throw new IllegalArgumentException("defaultName must not be empty");
        }
        if (isNullOrEmpty(prefix)) {
            return isNullOrEmpty(zSetKey) ? defaultName : zSetKey;
        }
        final String p = prefix.endsWith(":") ? prefix : prefix + ":";
        if (isNullOrEmpty(zSetKey)) {
            return p + defaultName;
        }
        return p + (zSetKey.startsWith(":") ? zSetKey.substring(1) : zSetKey);
    }

    // Helper: get entity name
    private static String getEntityName(Class<?> entityClass) {
        if (entityClass == null) {
            return "Entity";
        }
        String simple = entityClass.getSimpleName();
        return simple.isEmpty() ? entityClass.getName().replace('.', '_') : simple;
    }

    // Helper: null to empty string
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // Helper: is null or empty
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
