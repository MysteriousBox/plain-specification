package org.plain.specification.mybatisplus.context;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;

/**
 * 基于 {@link TenantContext} 的官方 {@link TenantLineHandler} 实现。
 * <p>
 * 配合 MyBatis-Plus 的 {@code TenantLineInnerInterceptor} 使用：
 * <pre>{@code
 * MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
 * interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new PlainTenantLineHandler()));
 * }</pre>
 * </p>
 * <p>
 * 若 {@link TenantContext} 中未设置租户ID，返回 {@code NULL} 使条件恒假，避免全表扫描泄漏。
 * 要跳过某张表的多租户过滤，可继承本类并覆写 {@link #ignoreTable(String)}。
 * </p>
 *
 * @author Jayden.Liang
 */
public class PlainTenantLineHandler implements TenantLineHandler {

    private static final String DEFAULT_TENANT_COLUMN = "tenant_id";

    private final String tenantIdColumn;

    public PlainTenantLineHandler() {
        this(DEFAULT_TENANT_COLUMN);
    }

    public PlainTenantLineHandler(String tenantIdColumn) {
        this.tenantIdColumn = tenantIdColumn;
    }

    @Override
    public Expression getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            return new NullValue();
        }
        try {
            return new LongValue(Long.parseLong(tenantId));
        } catch (NumberFormatException e) {
            return new StringValue(tenantId);
        }
    }

    @Override
    public String getTenantIdColumn() {
        return tenantIdColumn;
    }
}
