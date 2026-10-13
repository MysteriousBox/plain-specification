package org.plain.specification.mybatisplus.context;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.spi.ITenantProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 {@link PlainTenantLineHandler} 的租户字面量与豁免表行为。
 *
 * @author Jayden.Liang
 */
class PlainTenantLineHandlerTest {

    private static final String USER_ROLE_QUERY =
            "SELECT \"id\",\"user_id\",\"role_id\" FROM schema_user.\"user_role\" WHERE \"user_id\" = 1";

    @Test
    void shouldRenderZeroPaddedTenantAsQuotedString() {
        Expression tenantId = handler(() -> "000000").getTenantId();

        // 按数字解析会把 000000 变成 0，VARCHAR 租户列匹配不到任何行
        assertEquals(new StringValue("000000"), tenantId);
    }

    @Test
    void shouldEmitNeverMatchingPredicateWhenTenantMissing() {
        assertTrue(handler(() -> null).getTenantId() instanceof NullValue);
        assertTrue(handler(() -> "").getTenantId() instanceof NullValue);
    }

    @Test
    void shouldPushTenantValueIntoGeneratedSql() {
        String sql = new TenantLineInnerInterceptor(handler(() -> "000000"))
                .parserSingle(USER_ROLE_QUERY, null);

        assertTrue(sql.contains("tenant_id"), "未追加租户条件: " + sql);
        assertTrue(sql.contains("'000000'"), "租户值未下推: " + sql);
    }

    @Test
    void shouldIgnoreConfiguredTablesRegardlessOfQuotingOrCase() {
        PlainTenantLineHandler handler = handler(() -> "t1", Collections.singleton("CLIENT"));

        assertTrue(handler.ignoreTable("client"));
        assertTrue(handler.ignoreTable("\"client\""));
        assertTrue(handler.ignoreTable("schema_client.\"CLIENT\""));
        assertTrue(handler.ignoreTable("  client  "));
        assertFalse(handler.ignoreTable("user_role"));
    }

    @Test
    void shouldNotIgnoreAnythingWithoutConfiguration() {
        PlainTenantLineHandler handler = handler(() -> "t1", Collections.<String>emptySet());

        assertFalse(handler.ignoreTable("client"));
        assertFalse(handler.ignoreTable(null));
    }

    @Test
    void shouldFallbackToDefaultColumnWhenBlank() {
        assertEquals("tenant_id", new PlainTenantLineHandler(() -> "t1", "", null).getTenantIdColumn());
        assertEquals("org_id", new PlainTenantLineHandler(() -> "t1", "org_id", null).getTenantIdColumn());
    }

    @Test
    void shouldReadTenantFromProviderOnEveryCall() {
        MutableTenantSource source = new MutableTenantSource();
        PlainTenantLineHandler handler = handler(source);

        assertTrue(handler.getTenantId() instanceof NullValue);
        source.tenantId = "t2";
        assertEquals(new StringValue("t2"), handler.getTenantId());
    }

    private static PlainTenantLineHandler handler(ITenantProvider provider) {
        return new PlainTenantLineHandler(provider, "tenant_id", new HashSet<String>());
    }

    private static PlainTenantLineHandler handler(ITenantProvider provider, Set<String> ignoreTables) {
        return new PlainTenantLineHandler(provider, "tenant_id", ignoreTables);
    }

    private static class MutableTenantSource implements ITenantProvider {
        private String tenantId;

        @Override
        public String getTenantId() {
            return tenantId;
        }
    }
}
