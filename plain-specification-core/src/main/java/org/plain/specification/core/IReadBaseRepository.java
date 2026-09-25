package org.plain.specification.core;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 读取基本 Repository。
 * <p>
 * 异步方法默认通过 {@link #getAsyncExecutor()} 提供的执行器异步调用同步方法。
 * 实现类可覆盖 {@link #getAsyncExecutor()} 提供自定义线程池。
 * </p>
 *
 * @author Jayden.Liang
 * @param <T> 实体类型
 * @param <TID> ID 类型
 */
public interface IReadBaseRepository<T, TID> {

    /**
     * 返回用于异步操作的执行器。
     * <p>默认返回 {@link ForkJoinPool#commonPool()}，实现类可覆盖以提供自定义线程池。</p>
     *
     * @return 异步执行器
     */
    default Executor getAsyncExecutor() {
        return ForkJoinPool.commonPool();
    }

    /**
     * 根据 id 查找。
     *
     * @param id id
     * @return T
     */
    T findById(TID id);

    /**
     * 根据 id 异步查找。
     *
     * @param id id
     * @return CompletableFuture<T>
     */
    default CompletableFuture<T> findByIdAsync(TID id) {
        return CompletableFuture.supplyAsync(() -> findById(id), getAsyncExecutor());
    }

    /**
     * 根据条件查找单条记录。
     *
     * @param specification 条件
     * @return T
     */
    T findOne(ISpecification<T> specification);

    /**
     * 根据条件异步查找单条记录。
     *
     * @param specification 条件
     * @return CompletableFuture<T>
     */
    default CompletableFuture<T> findOneAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> findOne(specification), getAsyncExecutor());
    }

    /**
     * 根据条件查找多条记录。
     *
     * @param specification 条件
     * @return Collection<T>
     */
    Collection<T> findRange(ISpecification<T> specification);

    /**
     * 根据条件异步查找多条记录。
     *
     * @param specification 条件
     * @return CompletableFuture<Collection<T>>
     */
    default CompletableFuture<Collection<T>> findRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> findRange(specification), getAsyncExecutor());
    }

    /**
     * 获取总数。
     *
     * @param specification 条件
     * @return long
     */
    long count(ISpecification<T> specification);

    /**
     * 异步获取总数。
     *
     * @param specification 条件
     * @return CompletableFuture<Long>
     */
    default CompletableFuture<Long> countAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> count(specification), getAsyncExecutor());
    }

    /**
     * 获取分页数据。
     *
     * @param specification 查询条件
     * @param pageQuery 分页参数
     * @return IPageResult<T>
     */
    IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery);

    /**
     * 异步获取分页数据。
     *
     * @param specification 查询条件
     * @param pageQuery 分页参数
     * @return CompletableFuture<IPageResult<T>>
     */
    default CompletableFuture<IPageResult<T>> pageAsync(ISpecification<T> specification, PageQuery pageQuery) {
        return CompletableFuture.supplyAsync(() -> page(specification, pageQuery), getAsyncExecutor());
    }

    /**
     * 查询所有记录。
     *
     * @return 所有记录
     */
    default Collection<T> findAll() {
        return findRange(new Specification<>());
    }

    /**
     * 异步查询所有记录。
     *
     * @return 所有记录
     */
    default CompletableFuture<Collection<T>> findAllAsync() {
        return findRangeAsync(new Specification<>());
    }

    /**
     * 判断是否存在匹配的记录。
     *
     * @param specification 查询条件
     * @return true 如果存在
     */
    default boolean exists(ISpecification<T> specification) {
        return count(specification) > 0;
    }

    /**
     * 异步判断是否存在匹配的记录。
     *
     * @param specification 查询条件
     * @return true 如果存在
     */
    default CompletableFuture<Boolean> existsAsync(ISpecification<T> specification) {
        return countAsync(specification).thenApply(c -> c > 0);
    }

    /**
     * 查找单条记录，返回 Optional。
     *
     * @param specification 查询条件
     * @return Optional 包装的结果
     */
    default Optional<T> findOptional(ISpecification<T> specification) {
        return Optional.ofNullable(findOne(specification));
    }

    /**
     * 异步查找单条记录，返回 Optional。
     *
     * @param specification 查询条件
     * @return Optional 包装的结果
     */
    default CompletableFuture<Optional<T>> findOptionalAsync(ISpecification<T> specification) {
        return findOneAsync(specification).thenApply(Optional::ofNullable);
    }

}
