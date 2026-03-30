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
     *   <li>Always returns a key starting with {@code prefix}.</li>
     *   <li>If {@code zSetKey} is empty: returns {@code prefix + defaultName}.</li>
     *   <li>If {@code zSetKey} starts with ':' it is treated as a relative name and the leading ':' is dropped.</li>
     *   <li>Otherwise returns {@code prefix + zSetKey}.</li>
     * </ul>
     * <b>Note: prefix must not be null or empty.</b>
     *
     * @param prefix      computed prefix (must not be {@code null} or empty, always prepended)
     * @param zSetKey     requested zset name (may be {@code null} or empty)
     * @param defaultName default zset name when none provided (must not be {@code null} or empty)
     * @return full ZSet key (never {@code null}), always starts with prefix
     * @throws IllegalArgumentException if prefix or defaultName is null or empty
     */
    public static String computeZsetKey(String prefix, String zSetKey, String defaultName) {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be null or empty");
        }
        Objects.requireNonNull(defaultName, "defaultName must not be null");
        if (defaultName.isEmpty()) {
            throw new IllegalArgumentException("defaultName must not be empty");
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
