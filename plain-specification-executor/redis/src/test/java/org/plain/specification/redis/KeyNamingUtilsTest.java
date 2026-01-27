package org.plain.specification.redis;

import lombok.var;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：KeyNamingUtils 命名逻辑
 * @author Jayden.Liang
 */
class KeyNamingUtilsTest {
    @Test
    void testComputePrefix_basic() {
        var provider = DefaultStrategies.defaultKeyPrefixProvider();
        String prefix = KeyNamingUtils.computePrefix(provider, String.class, "plain");
        assertEquals("plain:String:", prefix);
    }

    @Test
    void testComputePrefix_withNulls() {
        var provider = DefaultStrategies.defaultKeyPrefixProvider();
        assertEquals("Entity:", KeyNamingUtils.computePrefix(provider, null, null));
    }

    @Test
    void testComputePrefix_enterprise() {
        TenantProvider tenantProvider = () -> "t1";
        var provider = DefaultStrategies.enterpriseKeyPrefixProvider("prod", tenantProvider);
        String prefix = KeyNamingUtils.computePrefix(provider, Integer.class, "plain");
        assertEquals("prod:t1:plain:Integer:", prefix);
    }

    @Test
    void testComputeZSetKey_cases() {
        assertEquals("foo:index", KeyNamingUtils.computeZsetKey("foo:", null, "index"));
        assertEquals("foo:bar", KeyNamingUtils.computeZsetKey("foo:", "bar", "index"));
        assertEquals("foo:bar", KeyNamingUtils.computeZsetKey("foo:", ":bar", "index"));
        assertEquals("index", KeyNamingUtils.computeZsetKey("", null, "index"));
        assertEquals("bar", KeyNamingUtils.computeZsetKey("", "bar", "index"));
    }
}
