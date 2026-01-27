package org.plain.specification.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 读取基本repository
 * @author Jayden.Liang
 * @param <T>
 * @param <TID>
 */
public interface IReadBaseRepository<T, TID> {

    /**
     * 根据id 查找
     * @param id id
     * @return T
     */
    T findById(TID id);

    /**
     * 根据id 异步查找
     * @param id id
     * @return CompletableFuture<T>
     */
    CompletableFuture<T> findByIdAsync(TID id);

    /**
     * 根据条件查找
     * @param specification 条件
     * @return T
     */
    T findOne(ISpecification<T> specification);

    /**
     * 根据条件查找
     * @param specification 条件
     * @return CompletableFuture<T>
     */
    CompletableFuture<T> findOneAsync(ISpecification<T> specification);

    /**
     * 根据条件查找
     * @param specification 条件
     * @return Collection<T>
     */
    Collection<T> findRange(ISpecification<T> specification);

    /**
     * 根据条件查找
     * @param specification 条件
     * @return CompletableFuture<Collection<T>>
     */
    CompletableFuture<Collection<T>> findRangeAsync(ISpecification<T> specification);

    /**
     * 获取总数
     * @param specification
     * @return long
     */
    long count(ISpecification<T> specification);

    /**
     * 获取总数
     * @param specification
     * @return CompletableFuture<Long>
     */
    CompletableFuture<Long> countAsync(ISpecification<T> specification);

    /**
     * 获取分页数据
     * @param specification 查询条件
     * @param pageQuery 分页参数
     * @return IPageResult<T>
     */
    IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery);


}
