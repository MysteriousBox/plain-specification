package org.plain.specification.core;


import lombok.Getter;

import java.io.Serializable;


/**
 * 分页查询参数
 */
@Getter
public class PageQuery implements Serializable {

    private final Integer page;
    private final Integer pageSize;

    public PageQuery(Integer page, Integer pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }
}
