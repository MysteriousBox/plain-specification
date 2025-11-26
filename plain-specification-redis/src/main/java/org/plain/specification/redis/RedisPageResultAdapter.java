package org.plain.specification.redis;

import org.plain.specification.core.IPageResult;
import org.plain.specification.core.PageQuery;

import java.util.Collection;
import java.util.Collections;

public class RedisPageResultAdapter<T> implements IPageResult<T> {

    private final Collection<T> records;

    private final PageQuery pageQuery;

    private final Long total;

    public RedisPageResultAdapter(Collection<T> records, PageQuery pageQuery,Long total) {
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
        if (pageQuery.getPageSize() == 0) return 0L;
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
