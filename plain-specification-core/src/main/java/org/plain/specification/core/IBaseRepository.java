package org.plain.specification.core;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 基础的 repository
 * 该 repository 主要实现一些常用的，通用的 操作方法 ，比如 保存、更新、删除等方法
 * @param <T> ENTITY 的类型
 * @author Jayden.Liang
 */
public interface IBaseRepository<T, TID> extends IReadBaseRepository<T, TID> {

    /**
     * 保存一个 entity 到数据库 database 中
     * @param entity 需要添加到 database 的 entity
     * @return entity
     */
    T save(T entity);

    /**
     * 异步的保存 entity 到 database 中
     * @param entity 需要添加到 database 的entity
     * @return  CompletableFuture<T> 异步的任务
     */
    CompletableFuture<T> saveAsync(T entity);

    /**
     * 异步的 保存给定的实体到数据库中。
     * @param entities 给定的实体
     * @return CompletableFuture<T> 异步的任务
     */
    CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities);

    /**
     * 更新entity到 database 中
     * @param entity 需要添加到 database 的entity
     * @return T
     */
    void update(T entity);

    /**
     * 异步更新到 database 中
     * @param entity 需要添加到 database 的entity
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> updateAsync(T entity);

    /**
     * 异步的更新 给定的entities 到数据库中
     * @param entities entities
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> updateRangeAsync(Collection<T> entities);

    /**
     * 根据id 删除 client
     * @param id id
     */
    void deleteById(TID id);

    /**
     * 根据ids 批量删除 entities
     * @param ids ids
     */
    void deleteByIds(Collection<TID> ids);

    /**
     * 根据id 从数据库中 删除 entity
     * @param entity 将要删除的 entity
     */
    void delete(T entity);

    /**
     * 异步从数据库中 删除 entity
     * @param entity entity
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> deleteAsync(T entity);

    /**
     * 异步从数据库中删除 给定的 entities
     * @param entities 需要删除的 entities
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> deleteRangeAsync(Collection<T> entities);


    /**
     * 根据 specification 删除 entities
     * @param specification  specification
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification);
}
