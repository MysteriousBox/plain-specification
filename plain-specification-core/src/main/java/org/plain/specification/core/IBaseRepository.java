package org.plain.specification.core;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 读写基本 Repository。
 * <p>
 * 异步方法默认通过 {@link #getAsyncExecutor()} 提供的执行器异步调用同步方法。
 * 实现类可覆盖异步方法以提供事务性或批量化优化。
 * </p>
 *
 * @param <T> 实体类型
 * @param <TID> ID 类型
 * @author Jayden.Liang
 */
public interface IBaseRepository<T, TID> extends IReadBaseRepository<T, TID> {

    /**
     * 保存实体。
     *
     * @param entity 需要保存的实体
     * @return 保存后的实体
     */
    T save(T entity);

    /**
     * 异步保存实体。
     *
     * @param entity 需要保存的实体
     * @return 异步任务
     */
    default CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> save(entity), getAsyncExecutor());
    }

    /**
     * 异步批量保存实体。
     *
     * @param entities 需要保存的实体集合
     * @return 异步任务
     */
    default CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            entities.forEach(this::save);
            return entities;
        }, getAsyncExecutor());
    }

    /**
     * 更新实体。
     *
     * @param entity 需要更新的实体
     */
    void update(T entity);

    /**
     * 异步更新实体。
     *
     * @param entity 需要更新的实体
     * @return 异步任务
     */
    default CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(() -> update(entity), getAsyncExecutor());
    }

    /**
     * 异步批量更新实体。
     *
     * @param entities 需要更新的实体集合
     * @return 异步任务
     */
    default CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(() -> entities.forEach(this::update), getAsyncExecutor());
    }

    /**
     * 根据 id 删除。
     *
     * @param id id
     */
    void deleteById(TID id);

    /**
     * 根据 id 集合批量删除。
     *
     * @param ids id 集合
     */
    void deleteByIds(Collection<TID> ids);

    /**
     * 根据实体删除。
     *
     * @param entity 需要删除的实体
     */
    void delete(T entity);

    /**
     * 异步删除实体。
     *
     * @param entity 需要删除的实体
     * @return 异步任务
     */
    default CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(() -> delete(entity), getAsyncExecutor());
    }

    /**
     * 异步批量删除实体。
     *
     * @param entities 需要删除的实体集合
     * @return 异步任务
     */
    default CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(() -> entities.forEach(this::delete), getAsyncExecutor());
    }

    /**
     * 根据条件删除实体。
     *
     * @param specification 查询条件
     * @return 异步任务
     */
    default CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.runAsync(() -> findRange(specification).forEach(this::delete), getAsyncExecutor());
    }
}
