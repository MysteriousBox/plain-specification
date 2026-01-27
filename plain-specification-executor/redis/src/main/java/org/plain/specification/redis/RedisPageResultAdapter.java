package org.plain.specification.redis;

import org.plain.specification.core.IPageResult;
import org.plain.specification.core.PageQuery;

import java.util.Collection;

/**
 * Redis 分页结果适配器
 *
 * <p>适配通用分页接口，便于统一分页处理。</p>
 *
 * @author Jayden.Liang
 * @since 1.0
 */
public class RedisPageResultAdapter<T> implements IPageResult<T> {

    /**
     * 当前页数据记录
     */
    private final Collection<T> records;
    /**
     * 分页查询参数
     */
    private final PageQuery pageQuery;
    /**
     * 总记录数
     */
    private final Long total;

    /**
     * 构造方法
     *
     * @param records   当前页数据
     * @param pageQuery 分页参数
     * @param total     总记录数
     */
    public RedisPageResultAdapter(Collection<T> records, PageQuery pageQuery, Long total) {
        this.records = records;
        this.pageQuery = pageQuery;
        this.total = total;
    }

    @Override
    public Collection<T> getRecords() {
        return records;
    }

    @Override
    public Long getPageSize() {
        return Long.valueOf(pageQuery.getPageSize());
    }

    @Override
    public Long getPage() {
        return Long.valueOf(pageQuery.getPage());
    }

    @Override
    public Long getTotal() {
        return total;
    }

    @Override
    public Long getPages() {
        if (pageQuery.getPageSize() == 0) {
            return 0L;
        }
        return (total + pageQuery.getPageSize() - 1) / pageQuery.getPageSize();
    }

    @Override
    public Boolean hasPrevious() {
        return pageQuery.getPage() > 1;
    }

    @Override
    public Boolean hasNext() {
        return pageQuery.getPage() < getPages();
    }
}
