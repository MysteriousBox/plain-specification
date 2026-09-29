package org.plain.specification.core;


import lombok.Setter;

import java.io.Serializable;


/**
 * 分页查询参数。
 * <p>
 * 作为 Spring MVC 的 model attribute 绑定时需要无参构造与 setter，因此字段不再
 * {@code final}。getter 对 {@code null} 与非正值兜底，避免下游
 * {@code new Page<>(long, long)} 解包时 NPE。
 *
 * @author Jayden.Liang
 */
@Setter
public class PageQuery implements Serializable {

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_PAGE_SIZE = 10;

    private Integer page = DEFAULT_PAGE;

    private Integer pageSize = DEFAULT_PAGE_SIZE;

    public PageQuery() {
    }

    public PageQuery(Integer page, Integer pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public Integer getPage() {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
    }
}
