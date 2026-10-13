package org.plain.specification.mybatisplus.context;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.plain.specification.core.spi.ITenantProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 基于 {@link ITenantProvider} 的官方 {@link TenantLineHandler} 实现。
 * <p>
 * 配合 MyBatis-Plus 的 {@code TenantLineInnerInterceptor} 使用：
 * <pre>{@code
 * MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
 * interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
 *         new PlainTenantLineHandler(tenantProvider, ignoreTables)));
 * }</pre>
 * </p>
 * <p>
 * 若租户提供者返回空值，条件写作 {@code NULL} 使其恒假，避免无上下文路径（MQ 消费、
 * 定时任务、异步线程）泄漏成全表结果。
 * </p>
 *
 * @author Jayden.Liang
 */
public class PlainTenantLineHandler implements TenantLineHandler {

    private static final String DEFAULT_TENANT_COLUMN = "tenant_id";

    private final ITenantProvider tenantProvider;

    private final String tenantIdColumn;

    private final Set<String> ignoreTables;

    public PlainTenantLineHandler(ITenantProvider tenantProvider) {
        this(tenantProvider, DEFAULT_TENANT_COLUMN, Collections.<String>emptySet());
    }

    public PlainTenantLineHandler(ITenantProvider tenantProvider, Set<String> ignoreTables) {
        this(tenantProvider, DEFAULT_TENANT_COLUMN, ignoreTables);
    }

    public PlainTenantLineHandler(ITenantProvider tenantProvider,
                                  String tenantIdColumn,
                                  Set<String> ignoreTables) {
        this.tenantProvider = tenantProvider;
        this.tenantIdColumn = tenantIdColumn == null || tenantIdColumn.isEmpty()
                ? DEFAULT_TENANT_COLUMN : tenantIdColumn;
        this.ignoreTables = normalize(ignoreTables);
    }

    /**
     * 返回租户条件的字面量。
     * <p>
     * 始终以字符串输出：租户ID 常见为零填充形态（{@code "000000"}），
     * 按数字解析会丢掉前导零而使条件匹配不到任何行。
     * </p>
     */
    @Override
    public Expression getTenantId() {
        String tenantId = tenantProvider == null ? null : tenantProvider.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            return new NullValue();
        }
        return new StringValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return tenantIdColumn;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null || ignoreTables.isEmpty()) {
            return false;
        }
        return ignoreTables.contains(normalizeTable(tableName));
    }

    private static Set<String> normalize(Set<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new HashSet<>();
        for (String tableName : tableNames) {
            String cleanName = normalizeTable(tableName);
            if (!cleanName.isEmpty()) {
                normalized.add(cleanName);
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static String normalizeTable(String tableName) {
        if (tableName == null) {
            return "";
        }
        String cleanName = tableName.replace("\"", "")
                .replace("`", "")
                .trim()
                .toLowerCase();
        int schemaSeparator = cleanName.lastIndexOf('.');
        if (schemaSeparator >= 0) {
            cleanName = cleanName.substring(schemaSeparator + 1);
        }
        return cleanName;
    }
}
