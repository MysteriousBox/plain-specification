package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.plain.specification.core.IPageResult;

import java.util.Collection;

/**
 * Class PageResultAdapter.
 *
 * @author Jayden.Liang
 */
public class PageResultAdapter<T> implements IPageResult<T> {

    private final IPage<T> page;

    public PageResultAdapter(IPage<T> page) {
        this.page = page;

    }


    @Override
    public Long getPageSize() {
        return page.getSize();
    }

    @Override
    public Long getPage() {
        return page.getCurrent();
    }

    @Override
    public Long getTotal() {
        return page.getTotal();
    }

    @Override
    public Long getPages() {
        return page.getPages();
    }

    @Override
    public Collection<T> getRecords() {
        return page.getRecords();
    }

    @Override
    public Boolean hasPrevious() {
        return this.getPage() > 1;
    }

    @Override
    public Boolean hasNext() {
        return this.getPage()<this.getPages();
    }
}
