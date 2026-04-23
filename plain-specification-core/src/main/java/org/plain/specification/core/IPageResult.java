package org.plain.specification.core;

import java.io.Serializable;
import java.util.Collection;

/**
 * Interface IPageResult.
 *
 * @author Jayden.Liang
 */


public interface IPageResult<T> extends Serializable {


    /**
     * 获取每页数量
     * @return 每页数量
     */
    Long getPageSize();

    /**
     * 获取当前页码
     * @return 当前页码
     */
    Long getPage();

    /**
     * 获取总记录数
     * @return 总记录数
     */
    Long getTotal();

    /**
     * 获取总页数
     * @return 总页数
     */
    Long getPages();

    /**
     * 获取记录列表
     * @return 记录列表
     */
    Collection<T> getRecords();

    /**
     * 是否有上一页
     * @return 是否有上一页
     */
    Boolean hasPrevious();

    /**
     * 是否有下一页
     * @return 是否有下一页
     */
    Boolean hasNext();

}
